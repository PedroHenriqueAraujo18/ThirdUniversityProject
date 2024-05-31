package com.example.lockit

import com.google.android.gms.maps.model.MarkerOptions
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.lockit.databinding.ActivityMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var lastKnownLocation: Location
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var auth: FirebaseAuth
    private var db = Firebase.firestore

    private var selectedPlace: Place? = null

    data class Place(
        val name: String,
        val latLng: LatLng,
        val description: String,
        val address: String,
    )

    private val places = arrayListOf(
        Place(
            "UNICAMP - Ciclo Básico",
            LatLng(-22.8179682, -47.0698738),
            "Localizado no meio do ciclo",
            "R. Sérgio Buarque de Holanda,- Cidade Universitária, Campinas - SP"
        ),
        Place(
            "PUCC-H15",
            LatLng(-22.8340787, -47.0552235),
            "Localizado dentro do H15",
            "Av. Reitor Benedito José Barreto Fonseca, H15 - Parque dos Jacarandás, Campinas - SP"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        val user = FirebaseAuth.getInstance().currentUser

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (user == null) {
            binding.btnAlugarArmario.isEnabled = false
            binding.btnAlugarArmario.visibility = View.GONE
        } else {
            binding.btnAlugarArmario.isEnabled = true
            binding.btnAlugarArmario.visibility = View.VISIBLE
        }

        binding.ivVoltar.setOnClickListener {
            if (user == null) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            } else {
                val intent = Intent(this, ClientMainActivity::class.java)
                startActivity(intent)
            }
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val sharedPreferences = getSharedPreferences("getLocacao", MODE_PRIVATE)
        sharedPreferences.getString("idLocacao", null)

        binding.btnAlugarArmario.setOnClickListener {
            if (selectedPlace == null) {
                Toast.makeText(this, "Por favor, selecione um armário no mapa.", Toast.LENGTH_SHORT).show()

            }
            if (user != null) {
                hasCard(user.uid) { hasCard ->
                    if (hasCard) {
                        verificarLocacao(user.uid) { hasPendentRent ->
                            if (hasPendentRent) {
                                Toast.makeText(this, "Você possui uma locação pendente.", Toast.LENGTH_SHORT).show()
                            } else {
                                checkSelectedLocker()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Nenhum cartão registrado.", Toast.LENGTH_LONG).show()
                        Log.d(TAG, "Cartão do usuário não encontrado")
                    }
                }
            }
        }
    }

    private fun verifyIfAvailableLocker(placeName: String?, callback: (Boolean) -> Unit) {
        var available = false
        db.collection("unidades de locação")
            .whereEqualTo("name", placeName)
            .get()
            .addOnSuccessListener { unidades ->
                val unidade = unidades.documents[0]
                if (unidade != null) {
                    Log.i(TAG, "Armários disponíveis: ${unidade.getLong("freeLockers")?.toInt()}")
                    if (unidade.getLong("freeLockers")?.toInt() != 0) {
                        available = true
                        Log.i(TAG, "$available")
                        callback(available)
                    }
                }
            }
    }

    private fun hasCard(uId: String, callback: (Boolean) -> Unit) {
        db.collection("pessoas")
            .document(uId)
            .collection("cartoes")
            .get()
            .addOnSuccessListener { documents ->
                if (documents != null && !documents.isEmpty) {
                    callback(true)
                }else{
                    callback(false)
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Erro ao recuperar os cartões do usuário", exception)
                callback(false)
            }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        myLocation(mMap)
        addPlaceMarkers()
        mMap.setInfoWindowAdapter(MarkerInfoAdapter(this))

        mMap.setOnMarkerClickListener { marker ->
            selectedPlace = places.find { it.latLng == marker.position }
            marker.showInfoWindow()
            true
        }
    }

    private fun addPlaceMarkers() {
        for (place in places) {
            val markerOptions = MarkerOptions().position(place.latLng)
                .title(place.name)
                .snippet(place.description)
            mMap.addMarker(markerOptions)
        }
    }

    private var isFirstLocationUpdate = true

    private fun myLocation(map: GoogleMap) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        // Inicializa o provedor de localização
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Configura a solicitação de localização
        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // Intervalo de atualização de localização em milissegundos
            fastestInterval = 5000 // Intervalo mais rápido de atualização de localização em milissegundos
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY // Prioridade de alta precisão
        }

        // Configura o callback de localização
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                if (locationResult.lastLocation != null) {
                    lastKnownLocation = locationResult.lastLocation!!
                    if (isFirstLocationUpdate) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude), 15f))
                        isFirstLocationUpdate = false
                    }
                }
            }
        }

        // Solicita atualizações de localização
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )

        // Configura o botão "Minha Localização"
        map.isMyLocationEnabled = true
    }

    private fun checkSelectedLocker() {
        if (selectedPlace != null) {
            val userLatLng = LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)
            val placeLatLng = selectedPlace!!.latLng
            val distance = calculateDistance(userLatLng, placeLatLng)
            val distanceThreshold = 1000 // Distância máxima em metros para considerar que o usuário está perto de um armário

            if (distance < distanceThreshold) {
                verifyIfAvailableLocker(selectedPlace!!.name) { available ->
                    if (available) {
                        Toast.makeText(this, "Você está perto do armário ${selectedPlace!!.name}", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, RentLockerActivity::class.java)
                        intent.putExtra("placeName", selectedPlace!!.name)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Você está perto do armário ${selectedPlace!!.name}, porém não há armários disponíveis no local.", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Você não está perto do armário selecionado.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calculateDistance(latLng1: LatLng, latLng2: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            latLng1.latitude, latLng1.longitude,
            latLng2.latitude, latLng2.longitude,
            results
        )
        return results[0]
    }

    private fun verificarLocacao(uId: String, callback: (Boolean) -> Unit) {
        db.collection("locação")
            .whereEqualTo("userId", uId) // Verifica se o campo "userId" é igual ao ID do usuário
            .get()
            .addOnSuccessListener { locacoes ->
                if (locacoes.isEmpty) {
                    callback(false)
                } else {
                    for (locacao in locacoes) {
                        if (locacao.getString("status") === "Encerrado" || locacao.getString("status") === "") {
                            callback(false)
                        } else {
                            Log.i(TAG, "Usuário possui locação ativa. Status: ${locacao.getString("status")}")
                            callback(true)
                        }
                    }
                }
            }
    }

    companion object {
        private var TAG = "checkCardMap"
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }
}
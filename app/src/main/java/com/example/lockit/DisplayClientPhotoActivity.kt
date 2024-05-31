package com.example.lockit

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.lockit.databinding.ActivityDisplayClientPhotoBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage


class DisplayClientPhotoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDisplayClientPhotoBinding
    private val db = Firebase.firestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDisplayClientPhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivVoltar.setOnClickListener {
            val intent = Intent(this,ReadNFCActivity::class.java)
            startActivity(intent)
        }
        binding.ivProsseguir.setOnClickListener {
            val intent = Intent(this,ChooseOpenOrEndRentActivity::class.java)
            startActivity(intent)
        }

        val sharedPreferences = getSharedPreferences("idLocacao", Context.MODE_PRIVATE)
        val idLocacao = sharedPreferences.getString("idLocacao", null)

        fetchFotoFromStorage(idLocacao)
    }

    private fun fetchFotoFromStorage(idLocacao: String?) {
        db.collection("locação")
            .document(idLocacao!!)
            .get()
            .addOnSuccessListener { locacao ->
                if (locacao.exists()) {
                    val fotoId = locacao.getString("fotoId") ?: ""
                    if(fotoId != ""){
                        val storageRef = FirebaseStorage.getInstance().reference.child("images/$fotoId.jpg")
                        storageRef.downloadUrl.addOnSuccessListener {
                            Log.i(TAG, "Foto encontrada: fotoId=$fotoId")

                            Glide.with(this)
                                .load(it)
                                .error(R.drawable.ic_launcher_background)
                                .into(binding.ivFotoCliente)

                        }.addOnFailureListener {
                            Log.e(TAG, "Erro ao buscar foto do armário", it)
                        }
                    }
                }else{
                    Log.d(TAG, "Nenhuma foto encontrada")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao buscar informações de locação", e)
            }
    }


}


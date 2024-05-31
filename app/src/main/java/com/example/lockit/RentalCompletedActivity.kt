package com.example.lockit

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.lockit.databinding.ActivityRentalCompletedBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class RentalCompletedActivity : AppCompatActivity() {
    private val db = Firebase.firestore
    private lateinit var binding: ActivityRentalCompletedBinding

    data class Armario(
        var idLocacao: String,
        val userId: String,
        val placeName: String,
        val tempoLocacao: String,
        val precoAPagar: Int,
        val dataLocacao: String,
        var numDeUsuarios: Int?,
        var status: String?,
        var fotoId: String?,
        var number: Int?
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRentalCompletedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences = getSharedPreferences("idLocacao", MODE_PRIVATE)
        val idLocacao = sharedPreferences.getString("idLocacao", null)

        binding.btnRetornarMenu.setOnClickListener {
           val intent = Intent(this,ManagerMainActivity::class.java)
           startActivity(intent)
        }

        fetchLocacaoFromFirestore(idLocacao){
            it?.let { locacao ->
                val infoLocacao = "Local: ${locacao.placeName}\n" +
                        "Tempo de locação: ${locacao.tempoLocacao}\n" +
                        "Preço a pagar: R$${locacao.precoAPagar}\n" +
                        "Data de locação: ${locacao.dataLocacao}\n" +
                        "Número do armário: ${locacao.number}\n" +
                        "Status: ${locacao.status}"
                binding.tvInfoLocacao.text = infoLocacao
            }
        }
    }

    private fun fetchLocacaoFromFirestore(idLocacao: String?, callback: (Armario?) -> Unit) {
        db.collection("locação")
            .document(idLocacao!!)
            .get()
            .addOnSuccessListener { locacao ->
                if (locacao.exists()) {
                    val locacao = Armario(
                        idLocacao = locacao.id,
                        userId = locacao.getString("userId") ?: "",
                        placeName = locacao.getString("placeName") ?: "",
                        tempoLocacao = locacao.getString("tempoLocacao") ?: "",
                        precoAPagar = locacao.getLong("precoAPagar")?.toInt() ?: 0,
                        dataLocacao = locacao.getString("dataLocacao") ?: "",
                        numDeUsuarios = locacao.getLong("numOfUsers")?.toInt() ?: 0,
                        status = locacao.getString("status") ?: "",
                        fotoId = locacao.getString("fotoUrl") ?: "",
                        number = locacao.getLong("lockerNumber")?.toInt() ?: 0
                    )
                    callback(locacao)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao buscar informações de locação", e)
                callback(null)
            }
    }
}
package com.example.lockit

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.lockit.databinding.ActivityOpenLockerBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class OpenLockerActivity : AppCompatActivity() {
    private val db = Firebase.firestore
    private lateinit var binding: ActivityOpenLockerBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityOpenLockerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvStatus.text = "fechado"


        val sharedPreferences = getSharedPreferences("idLocacao", MODE_PRIVATE)
        val idLocacao = sharedPreferences.getString("idLocacao", null)

        getLockerNumber(idLocacao){ number ->
            binding.tvNumber.text = number
        }


        binding.btnAbrirArmario.setOnClickListener {
            abrirArmarioMomentaneamente(idLocacao) { sucesso, status ->
                if (sucesso) {
                    Log.i(TAG, "Armário aberto.")
                    binding.tvStatus.text = status
                } else {
                    Log.i(TAG, "Erro ao abrir armário.")
                    binding.tvStatus.text = status

                }
            }
        }

        binding.btnFecharArmario.setOnClickListener {
            fecharArmario(idLocacao) { sucesso, status ->
                if (sucesso) {
                    Log.i(TAG, "Armário fechado.")
                    binding.tvStatus.text = status
                } else {
                    Log.i(TAG, "")
                    binding.tvStatus.text = status
                }
            }
        }

        binding.ivVoltar.setOnClickListener {
            val intent = Intent(this, ChooseOpenOrEndRentActivity::class.java)
            startActivity(intent)
        }
    }

    private fun getLockerNumber(idLocacao: String?, callback: (String) -> Unit) {
        db.collection("locação")
            .document(idLocacao!!)
            .get()
            .addOnSuccessListener { locacao ->
                val numeroArmario = locacao.get("number").toString()
                callback(numeroArmario)
            }
            .addOnFailureListener() {
                callback("Erro ao buscar número do armário.")
            }
    }

    private fun fecharArmario(idLocacao: String?, callback: (Boolean, String) -> Unit) {
        db.collection("locação")
            .document(idLocacao!!)
            .update("status", "Ativo")
            .addOnSuccessListener {
                callback(true, "fechado")
            }
            .addOnFailureListener(){
                callback(false, "Erro ao fechar armário.")
            }
    }

    private fun abrirArmarioMomentaneamente(idLocacao: String?, callback: (Boolean, String) -> Unit) {
        db.collection("locação")
            .document(idLocacao!!)
            .update("status", "Aberto")
            .addOnSuccessListener {
                callback(true, "aberto")
            }
            .addOnFailureListener(){
                callback(false, "ID de locação inválido")
            }

    }
}
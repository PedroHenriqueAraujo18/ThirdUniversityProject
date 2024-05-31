package com.example.lockit

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.lockit.databinding.ActivitySelectNumberOfUsersBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class SelectNumberOfUsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectNumberOfUsersBinding
    var numPessoas: Int = 0
    private var db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySelectNumberOfUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var sharedPreferences = getSharedPreferences("idLocacao", MODE_PRIVATE)
        var locacaoId = sharedPreferences.getString("idLocacao", null)


        binding.rgOpcoes.setOnCheckedChangeListener { _, i ->
            when (i) {
                R.id.rb1pessoa -> {
                    numPessoas = 1
                }

                R.id.rb2pessoas -> {
                    numPessoas = 2
                }
            }
            Log.i(TAG, "Número de pessoas selecionado: $numPessoas")
            if (locacaoId != null) {
                db.collection("locação")
                    .document(locacaoId)
                    .update("numDeUsuarios", numPessoas)
                    .addOnSuccessListener {
                        Log.i(TAG, "Número de pessoas atualizado com sucesso!")
                    }
                    .addOnFailureListener() {
                        Log.e(TAG, "Erro ao atualizar número de pessoas! locacaoId: $locacaoId")
                    }
            }
        }

        binding.btnContinuar.setOnClickListener {
            val intent = Intent(this, CameraPreviewActivity::class.java).apply {
                putExtra("locacaoId", locacaoId)
            }
            startActivity(intent)
        }

        binding.ivVoltar.setOnClickListener{
            val intent = Intent(this, ManagerMainActivity::class.java)
            startActivity(intent)
        }
    }
}
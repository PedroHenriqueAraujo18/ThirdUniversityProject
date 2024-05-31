package com.example.lockit


import android.content.ContentValues.TAG
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.lockit.databinding.ActivityClientMainBinding
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.firestore


class ClientMainActivity : AppCompatActivity() {
    private var binding: ActivityClientMainBinding? = null
    val auth = FirebaseAuth.getInstance()
    private var db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityClientMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        binding?.btnArmarioAlugado?.visibility = View.GONE
        binding?.btnArmarioAlugado?.isEnabled = false

        val currentUser = auth.currentUser


        if (currentUser != null) {
            Log.d(TAG, currentUser.uid)

            val sharedPreferences = getSharedPreferences("getLocacao", Context.MODE_PRIVATE)
            val idLocacao = sharedPreferences.getString("idLocacao", null)

            verificarLocacao(idLocacao.toString(),currentUser.uid) { hasLocacao ->
                if (hasLocacao) {
                    // Usuário possui locação, você pode executar ações relacionadas aqui
                    Log.d(TAG, "Usuário possui locação.")
                    binding?.btnArmarioAlugado?.isEnabled = true
                    binding?.btnArmarioAlugado?.visibility = View.VISIBLE
                    binding?.btnArmarioAlugado?.setOnClickListener {
                        val intent = Intent(this@ClientMainActivity, DisplayLockerInfoActivity::class.java)
                        startActivity(intent)
                    }
                } else {
                    Log.d(TAG, "Usuário não possui locação.")
                    binding?.btnArmarioAlugado?.visibility = View.GONE
                    binding?.btnArmarioAlugado?.isEnabled = false
                }
            }


            binding?.btnCards?.setOnClickListener {
                val intent = Intent(this@ClientMainActivity, SelectCardActivity::class.java)
                startActivity(intent)
            }
            binding?.btnMapa?.setOnClickListener {
                val intent = Intent(this@ClientMainActivity, MapActivity::class.java)
                startActivity(intent)
            }
            binding?.btnSair?.setOnClickListener {
                sair()
                val intent = Intent(this@ClientMainActivity, MainActivity::class.java)
                startActivity(intent)
            }
        }else{
            val intent = Intent(this@ClientMainActivity, MainActivity::class.java)
            Toast.makeText(this, "Usuário não logado", Toast.LENGTH_SHORT).show()
            startActivity(intent)
        }
    }

    private fun verificarLocacao(id:String, uId: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "ID LOCAÇÃO : $id")
        db.collection("locação")
            .whereEqualTo("userId", uId) // Verifica se o campo "userId" é igual ao ID do usuário
            .whereEqualTo(FieldPath.documentId(), id) // Verifica se o ID do documento é igual ao ID fornecido
            .get()
            .addOnSuccessListener { documents ->
                val hasLocacao = !documents.isEmpty()
                Log.d(TAG, "RETURN : $hasLocacao")
                callback(hasLocacao)
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Erro ao verificar locação do usuário.", exception)
                callback(false) // Em caso de falha, assume-se que o usuário não possui locação
            }
    }


    //logout user
    private fun sair(){
        Toast.makeText(baseContext,"Logout efetuado com sucesso.", Toast.LENGTH_SHORT).show()
        FirebaseAuth.getInstance().signOut()
    }
}
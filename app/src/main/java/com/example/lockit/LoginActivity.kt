package com.example.lockit

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.lockit.databinding.ActivityLoginBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore


class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var binding: ActivityLoginBinding? = null


    /**
     * @author:Pedro e Dupas
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        auth = Firebase.auth


        binding?.ivVoltar?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }


        binding?.btnLogin?.setOnClickListener {
            val email = binding?.etEmail?.text.toString()
            val password = binding?.etPassword?.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()){
                signInWithEmailAndPassword(email, password)

            }else{
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            }
        }

        binding?.tvCreateAccount?.setOnClickListener {
            val intent = Intent(this@LoginActivity, RegisterAccountActivity::class.java)
            startActivity(intent)
        }

        binding?.tvEsqueciSenha?.setOnClickListener {
            val intent = Intent(this@LoginActivity, ForgotPassword::class.java)
            startActivity(intent)
        }

    }

    /**
     * Função pra validar o cadastro com sharedPreferences
     * @author:Pedro e Dupas
     */


    private fun signInWithEmailAndPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                if (user != null && user.isEmailVerified) {
                    // Verifique localmente se o usuário é um gerente
                    checkIfUserIsManager(email) { isManager ->
                        if (isManager) {
                            // Redireciona o gerente para a atividade específica do gerente
                            val intent = Intent(this@LoginActivity, ManagerMainActivity::class.java)
                            startActivity(intent)
                        } else {
                            // Redireciona outros usuários para a atividade padrão
                            val intent = Intent(this@LoginActivity, ClientMainActivity::class.java)
                            startActivity(intent)
                        }
                    }
                } else {
                    Toast.makeText(baseContext, "Verifique seu email.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.w(TAG, "signInWithEmail:failure", task.exception)
                Toast.makeText(baseContext, "Falha na Autenticação.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Função para verificar localmente se o usuário é um gerente
    private fun checkIfUserIsManager(email: String, callback: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val usersRef = db.collection("pessoas").whereEqualTo("email", email)

        usersRef.get().addOnSuccessListener { documents ->
            if (documents != null && !documents.isEmpty) {
                val userDocument = documents.documents.first()
                val isManager = userDocument.getBoolean("gerente") ?: false
                callback(isManager)
            } else {
                // Usuário não encontrado na coleção 'pessoas' ou documento não possui o campo 'gerente'
                callback(false)
            }
        }.addOnFailureListener { exception ->
            Log.w(TAG, "Error getting documents: ", exception)
            // Se houver algum erro ao acessar o banco de dados, considere o usuário como não gerente
            callback(false)
        }
    }


    companion object{
        private var TAG ="EmailAndPassword"
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}
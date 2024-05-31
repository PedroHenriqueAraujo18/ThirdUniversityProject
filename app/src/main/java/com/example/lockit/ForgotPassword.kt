package com.example.lockit

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.lockit.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPassword : AppCompatActivity() {
    private var binding: ActivityForgotPasswordBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        binding?.ivVoltar?.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding?.btnEnviarEmail?.setOnClickListener {
            val email = binding?.edEmailL?.text.toString()
            if (email.isNotEmpty()) {
                forgotPassword(email)
                val intent = Intent(this@ForgotPassword, LoginActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(baseContext, "Preencha o campo", Toast.LENGTH_LONG).show()
            }

        }
    }

    private fun forgotPassword(email: String) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "createUserWithEmail:success")
                Toast.makeText(baseContext, "Email de resetar senha enviado para $email.", Toast.LENGTH_LONG).show()

            } else {
                Log.w(TAG, "createUserWithEmail:failure", task.exception)
                Toast.makeText(baseContext, "Erro ao enviar email", Toast.LENGTH_LONG).show()
            }
        }
    }

}
package com.example.lockit

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lockit.databinding.ActivityRegisterCardBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore


class RegisterCardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterCardBinding
    private val db = Firebase.firestore
    val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_card)

        binding = ActivityRegisterCardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupExpirationDateTextWatcher()
        setupCVVTextWatcher()
        setupCardNumberWatcher()

        binding.ivVoltar.setOnClickListener {
            val intent = Intent(this, SelectCardActivity::class.java)
            startActivity(intent)
        }


        val currentUser = auth.currentUser
        val userId = currentUser?.uid


        binding.btnRegisterCard.setOnClickListener {
            if (userId != null) {
                if (cardValidation()){

                    saveCardToFirestore(
                        userId, // Passa o ID do usuário atual
                        binding.etCardNumber.text.toString(),
                        binding.etExpiration.text.toString(),
                        binding.etUserName.text.toString(),
                        binding.etCVV.text.toString(),
                        active = false,
                        balance = "1000.0"
                    )

                    Log.w(TAG, "Cartão cadastrado com sucesso")
                    Toast.makeText(this, "Cartão cadastrado com sucesso", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this, "Erro ao cadastrar o cartão... Tente novamente.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            intent = Intent(this, SelectCardActivity::class.java)
            startActivity(intent)
        }
    }

    private fun cardValidation(): Boolean {
        if (binding.etCardNumber.text.toString().isEmpty() ||
            binding.etExpiration.text.toString().isEmpty() ||
            binding.etUserName.text.toString().isEmpty() ||
            binding.etCVV.text.toString().isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return false
        }else if (binding.etCardNumber.text.toString().length != 16){
            Toast.makeText(this, "Número do cartão inválido", Toast.LENGTH_SHORT).show()
            return false
        }else if (binding.etCVV.text.toString().length != 3){
            Toast.makeText(this, "CVV invalido", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun saveCardToFirestore(
        uId: String,
        number: String,
        expiration: String,
        cardHolderName: String,
        cvv: String,
        active:Boolean,
        balance: String
    ) {
        Log.w(TAG, uId)

        val card = hashMapOf(
            "uId" to uId,
            "number" to number,
            "expiration" to expiration,
            "cardHolderName" to cardHolderName,
            "cvv" to cvv,
            "active" to active,
            "balance" to balance
        )
        Log.d(TAG, "Cartão: $card")
        db.collection("pessoas")
            .document(uId).collection("cartoes")
            .add(card)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Cartao adicionado com Id: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }
    }

    private fun setupExpirationDateTextWatcher() {
        binding.etExpiration.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Não é necessário implementar antes da mudança de texto
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Não é necessário implementar durante a mudança de texto
            }

            override fun afterTextChanged(s: Editable?) {
                if (s != null) {
                    if (s.length == 2 && !s.contains("/")) {
                        // Adiciona uma barra ("/") após o mês (MM)
                        binding.etExpiration.setText(String.format("%s/", s))
                        binding.etExpiration.setSelection(binding.etExpiration.length()) // Coloca o cursor no final
                    } else if (s.length > 5) {
                        // Limita o número de caracteres a 5 (MM/AA)
                        binding.etExpiration.setText(s.subSequence(0, 5))
                        binding.etExpiration.setSelection(binding.etExpiration.length()) // Coloca o cursor no final
                    }
                }
            }
        })
    }


    private fun setupCVVTextWatcher() {
        binding.etCVV.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Não é necessário implementar antes da mudança de texto
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Não é necessário implementar durante a mudança de texto
            }

            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.length > 3) {
                    // Limita o número de caracteres a 3
                    binding.etCVV.setText(s.subSequence(0, 3))
                    binding.etCVV.setSelection(binding.etCVV.length()) // Coloca o cursor no final
                }
            }
        })
    }

    private fun setupCardNumberWatcher() {
        binding.etCardNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Não é necessário implementar antes da mudança de texto
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Não é necessário implementar durante a mudança de texto
            }

            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.length > 16) {
                    // Limita o número de caracteres a 3
                    binding.etCardNumber.setText(s.subSequence(0, 16))
                    binding.etCardNumber.setSelection(binding.etCardNumber.length()) // Coloca o cursor no final
                }
            }
        })
    }


}
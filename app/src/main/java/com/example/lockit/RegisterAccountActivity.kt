package com.example.lockit


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import com.example.lockit.databinding.ActivityRegisterBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class RegisterAccountActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var binding: ActivityRegisterBinding? = null
    private val db = Firebase.firestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        binding?.etBirthDate?.addTextChangedListener(dateWatcher)

        auth = Firebase.auth

        binding?.ivVoltar?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }


        binding?.btnRegister?.setOnClickListener {
            val name = binding?.etName?.text.toString()
            val cpf = binding?.etCpf?.text.toString()
            val birthDate = binding?.etBirthDate?.text.toString()
            val cel = binding?.etCel?.text.toString()
            val email = binding?.etEmail?.text.toString()
            val password = binding?.etPassword?.text.toString()
            val confirmPassword = binding?.etConfirmPassword?.text.toString()

            if (name.isNotEmpty() &&
                cpf.isNotEmpty() &&
                birthDate.isNotEmpty() &&
                cel.isNotEmpty() &&
                email.isNotEmpty() &&
                password.isNotEmpty() &&
                confirmPassword.isNotEmpty()){

                if (password.length < 6) {
                    Toast.makeText(this, "Senha deve ter pelo menos 6 caracteres.", Toast.LENGTH_SHORT).show()
                } else if (password == confirmPassword) {
                    val age = (isValidAge(birthDate))
                    if(age < 14 || age > 120){
                        Toast.makeText(this,"Idade deve ser maior que 14 ou menor que 120.",Toast.LENGTH_SHORT).show()
                    }else if(!isValidCPF(cpf)){
                        Toast.makeText(this, "CPF inválido", Toast.LENGTH_SHORT).show()
                    }
                    else if (cel.length != 11) {
                        Toast.makeText(
                            this,
                            "Número de celular inválido. Deve conter 11 dígitos.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else {
                        createUserWithEmailAndPassword(email, password, name, cpf, birthDate, cel)
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    Toast.makeText(this, "As senhas não são iguais", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            }
        }

    }


    private fun isValidCPF(cpf: String): Boolean {
        // Verifica se o CPF é uma sequência de números repetidos ou se não possui 11 dígitos
        if (cpf == "00000000000" || cpf == "11111111111" || cpf == "22222222222" ||
            cpf == "33333333333" || cpf == "44444444444" || cpf == "55555555555" ||
            cpf == "66666666666" || cpf == "77777777777" || cpf == "88888888888" ||
            cpf == "99999999999" || cpf.length != 11) {
            return false
        }

        // Calcula o primeiro dígito verificador do CPF
        var sm = 0
        var peso = 10
        for (i in 0 until 9) {
            val num = Character.getNumericValue(cpf[i])
            sm += num * peso
            peso--
        }

        var r = 11 - (sm % 11)
        val dig10 = if (r == 10 || r == 11) '0' else (r + 48).toChar()

        // Calcula o segundo dígito verificador do CPF
        sm = 0
        peso = 11
        for (i in 0 until 10) {
            val num = Character.getNumericValue(cpf[i])
            sm += num * peso
            peso--
        }

        r = 11 - (sm % 11)


        // Verifica se os dígitos calculados conferem com os dígitos informados
        val dig11 = if (r == 10 || r == 11) '0' else (r + 48).toChar()
        return dig10 == cpf[9] && dig11 == cpf[10]
    }


    private fun isValidAge(birthDate: String): Int {
        try {
            val date = LocalDate.parse(birthDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            val currentDate = LocalDate.now()
            var age = currentDate.year - date.year
            if (currentDate.dayOfYear < date.dayOfYear) {
                age--
            }
            return age
        } catch (e: Exception) {
            Toast.makeText(this, "Data de nascimento inválida", Toast.LENGTH_SHORT).show()
            return -1 // Retorna -1 para indicar que a idade não pôde ser calculada
        }
    }



    private fun createUserWithEmailAndPassword(email: String, password: String, name: String, cpf: String, birthDate: String, cel: String) {

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    Log.w(TAG, "Usuário criado com sucesso. Id: ${user.uid}")
                    user.sendEmailVerification().addOnCompleteListener { verificationTask ->
                        if (verificationTask.isSuccessful) {
                            var gerente : Boolean = false
                            saveUserToFirestore(user.uid, name, cpf, birthDate , cel, email, password, gerente)
                            Toast.makeText(this, "Email de verificação enviado.", Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "Email de verificação enviado.")
                        } else {
                            Log.e(TAG, "Falha ao enviar e-mail de verificação.", verificationTask.exception)
                        }
                    }
                }
            } else {
                Log.w(TAG,"createUserWithEmail:failure", task.exception)
                Toast.makeText(baseContext, "Falha na autenticação.${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun saveUserToFirestore(userId: String, name: String, cpf: String, birthDate: String, cel: String, email: String,password: String, gerente: Boolean){
        val user = hashMapOf(
            "userId" to auth.currentUser?.uid,
            "name" to name,
            "cpf" to cpf,
            "birthDate" to birthDate,
            "cel" to cel,
            "email" to email,
            "password" to password,
            "gerente" to gerente
        )
        db.collection("pessoas")
            .document(userId)
            .set(user)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot added with ID: ${userId}")
            }
            .addOnFailureListener { e -> Log.w(TAG, "Error adding document", e)
            }
    }

    private val dateWatcher = object : TextWatcher {
        private val maxLength = 10 // Formato dd/mm/aaaa

        private var edited = false

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            if (edited) return

            edited = true

            val input = s.toString()

            // Permite apenas edição antes do ano (aaaa)
            if (input.length > maxLength) {
                val formatted = input.substring(0, maxLength)
                binding?.etBirthDate?.setText(formatted)
                binding?.etBirthDate?.setSelection(maxLength)
            }

            // Formatação automática
            if (input.length <= 2) {
                // Adiciona a barra após dois dígitos
                val formatted = if (input.length == 2 && !input.contains("/")) {
                    "$input/"
                } else {
                    input
                }
                binding?.etBirthDate?.setText(formatted)
                binding?.etBirthDate?.setSelection(formatted.length)
            } else if (input.length <= 5) {
                // Adiciona a barra após cinco dígitos
                val formatted = if (input.length == 5 && !input.endsWith("/")) {
                    "$input/"
                } else {
                    input
                }
                binding?.etBirthDate?.setText(formatted)
                binding?.etBirthDate?.setSelection(formatted.length)
            }

            edited = false
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
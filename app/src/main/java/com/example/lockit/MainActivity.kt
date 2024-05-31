package com.example.lockit


import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.lockit.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var db = Firebase.firestore
    private var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = Firebase.auth
        val user = auth.currentUser

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)


        binding?.btnLogin?.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        binding?.btnRegister?.setOnClickListener {
            val intent = Intent(this, RegisterAccountActivity::class.java)
            startActivity(intent)
        }

        binding?.btnMap?.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }



    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val user = auth.currentUser
        if (user != null && user.isEmailVerified) {
            db.collection("pessoas").document(user.uid).get().addOnSuccessListener {
                if (it.getBoolean("gerente") == true) {
                    val intent = Intent(this, ManagerMainActivity::class.java)
                    startActivity(intent)
                } else {
                    val intent = Intent(this, ClientMainActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }


}
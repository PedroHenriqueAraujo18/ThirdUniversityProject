package com.example.lockit

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.lockit.databinding.ActivityManagerBinding
import com.google.firebase.auth.FirebaseAuth

class ManagerMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManagerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.flRealizar.setOnClickListener {
            cameraProviderResult.launch(android.Manifest.permission.CAMERA)
        }

        binding.flFinalizar.setOnClickListener {
            val intent = Intent(this, ReadNFCActivity::class.java)
            startActivity(intent)
        }

        binding.btnSair.setOnClickListener {
            sair()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }


    }

    private val cameraProviderResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()){
            if(it){
                OpenCameraPreview()
            }else{
                Toast.makeText(baseContext, "VOCE NÃO DEU PERMISSÃO PARA A CAMERA", Toast.LENGTH_LONG).show()
            }
        }

    //manda pra cameraView
    private fun OpenCameraPreview(){
        val Camera = Intent(this,QrCodeScannerActivity::class.java)
        startActivity(Camera)
    }

    private fun sair(){
        Toast.makeText(baseContext,"Logout efetuado com sucesso.", Toast.LENGTH_SHORT).show()
        FirebaseAuth.getInstance().signOut()
    }

}

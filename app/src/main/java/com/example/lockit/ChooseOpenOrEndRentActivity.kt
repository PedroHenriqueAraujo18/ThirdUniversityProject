package com.example.lockit

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.example.lockit.databinding.ActivityChooseOpenOrEndRentBinding

class ChooseOpenOrEndRentActivity : WriteNFCActivity() {

    private lateinit var binding: ActivityChooseOpenOrEndRentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChooseOpenOrEndRentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivVoltar.setOnClickListener{
            Log.d(TAG,"Clicou")
            val intent = Intent(this,DisplayClientPhotoActivity::class.java)
            startActivity(intent)
        }

        binding.encerrarLocacao.setOnClickListener {
            Log.d(TAG, "Clicou")
            val intent=Intent(this, EndLocationActivity::class.java)
            startActivity(intent)
        }

        binding.abrirArmarioMomentaneamente.setOnClickListener {
            Log.d(TAG, "Clicou")
            val intent = Intent(this, OpenLockerActivity::class.java)
            startActivity(intent)
        }
    }


}

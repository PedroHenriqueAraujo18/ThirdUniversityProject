package com.example.lockit

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.lockit.databinding.ActivityReadNfcactivityBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.nio.charset.Charset

class ReadNFCActivity : NfcActivity() {
    private var placeName: String? = null
    private val db = Firebase.firestore
    private lateinit var binding: ActivityReadNfcactivityBinding
    private var idLocacao: String? = null
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityReadNfcactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVerInformacoes.isEnabled = false

        binding.ivVoltar.setOnClickListener{
            val intent = Intent(this, ManagerMainActivity::class.java)
            startActivity(intent)
        }

        binding.btnVerInformacoes.setOnClickListener {
            val intent = Intent(this, DisplayClientPhotoActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onTagDiscovered(tag: Tag?) {
        tag?.let {
            val message = readNdefMessage(it)
            message?.let {
                val records = it.records
                for (record in records) {
                    val rawPayload = String(record.payload, Charset.forName("UTF-8")).trim()
                    val dataPayload = extractDataFromPayload(rawPayload)
                    runOnUiThread {
                        Log.d(TAG, "Dados da locação lidos: $dataPayload")
                        Toast.makeText(this, "Dados da locação lidos: $dataPayload", Toast.LENGTH_LONG).show()
                        idLocacao = dataPayload

                        val sharedPreferences = getSharedPreferences("idLocacao", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putString("idLocacao", idLocacao) // Aqui o ID de locação é salvo
                        editor.apply()
                        fetchAndDisplayLocacaoData(dataPayload)
                        checkIfLockerExists(dataPayload) { exists ->
                            if (exists) {
                                binding.btnVerInformacoes.isEnabled = true

                            } else {
                                runOnUiThread {
                                    Log.e("ReadNFCActivity", "Locker não encontrado")
                                }
                            }
                        }

                    }
                }
            } ?: runOnUiThread {
                Log.d(TAG, "Nenhum dado encontrado na tag NFC")
                Toast.makeText(this, "Nenhum dado encontrado na tag NFC", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun extractDataFromPayload(payload: String): String {
        // Remove o primeiro caractere se for um caractere de controle 'Start of Text' (ASCII 02)
        var adjustedPayload = if (payload.startsWith("\u0002")) {
            payload.substring(1)
        } else {
            payload
        }

        // Remove os caracteres 'pt' se estiverem presentes no início do DATA ajustado
        if (adjustedPayload.startsWith("pt")) {
            adjustedPayload = adjustedPayload.substring(2)
        }

        return adjustedPayload
    }



    override fun readNdefMessage(tag: Tag): NdefMessage? {
        val ndef = android.nfc.tech.Ndef.get(tag)
        return if (ndef != null) {
            ndef.connect()
            val message = ndef.ndefMessage
            ndef.close()
            message
        } else {
            null
        }
    }

    private fun fetchAndDisplayLocacaoData(idLocacao: String) {
        db.collection("locação").document(idLocacao).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Acessa apenas os campos desejados
                    userId = documentSnapshot.getString("userId") ?: "Id do usuário não disponível"
                    val dataLocacao = documentSnapshot.getString("dataLocacao") ?: "Não disponível"
                    placeName = documentSnapshot.getString("placeName") ?: "Não disponível"
                    val tempoLocacao = documentSnapshot.getString("tempoLocacao") ?: "Não disponível"
                    val precoAPagar = documentSnapshot.getLong("precoAPagar")?.toString() ?: "Não disponível"

                    // Demonstra os dados
                    Log.d(TAG, "Data da Locação: $dataLocacao")
                    Log.d(TAG, "Nome do Local: $placeName")
                    Log.d(TAG, "Tempo de Locação: $tempoLocacao")
                    Log.d(TAG, "Preço Pago: $precoAPagar")


                } else {
                    Log.d(TAG, "Documento não encontrado.")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Erro ao buscar documento: ", exception)
            }
    }

    private fun checkIfLockerExists(idLocacao: String, callback:(Boolean) -> Unit) {
        db.collection("locação")
            .document(idLocacao)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    callback(true)
                    Log.d(TAG, "Locker ID: $idLocacao")
                } else {
                    Log.d(TAG, "Documento não encontrado.")
                    callback(false)
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Erro ao buscar documento: ", exception)
            }
    }


}

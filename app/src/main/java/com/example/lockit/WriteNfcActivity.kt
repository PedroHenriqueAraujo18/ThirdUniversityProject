package com.example.lockit

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.lockit.databinding.ActivityWriteNfcactivityBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

open class WriteNFCActivity : NfcActivity()  {

    var detectedTag: Tag? = null
    private lateinit var locacaoId: String
    private val db = Firebase.firestore
    var userId: String? = null
    var placeName: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityWriteNfcactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnMostraArmario.visibility= android.view.View.GONE
        binding.btnMostraArmario.isEnabled= false

        val sharedPreferences = getSharedPreferences("idLocacao", Context.MODE_PRIVATE)
        locacaoId = sharedPreferences.getString("idLocacao", null) ?: ""

        binding.btnWriteNFC.setOnClickListener {
            detectedTag?.let {
                val payload = locacaoId // Define o payload como locacaoId puro
                val ndefMessage = createNdefMessage(payload)
                writeNdefMessage(it, ndefMessage)
            }
            binding.btnMostraArmario.visibility= android.view.View.VISIBLE
            binding.btnMostraArmario.isEnabled= true

            updateLocacaoStatus(locacaoId)
        }

        fetchLocacaoData(locacaoId)

        binding.btnMostraArmario.setOnClickListener {

            debitPaymentFromUser{ isBalanceUpdated ->
                if(isBalanceUpdated){
                    Log.i(TAG, "Cobrança realizada com sucesso")
                    Toast.makeText(this, "Cobrado caução do cartão do usuário", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, RentalCompletedActivity::class.java)
                    startActivity(intent)
                }else{
                    Log.e(TAG, "Erro ao debitar caução")
                    Toast.makeText(this, "Erro ao debitar caução", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.ivVoltar.setOnClickListener {
            val intent = Intent(this, ManagerMainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun debitPaymentFromUser(callback: (Boolean) -> Unit) {
        getActiveCardBalance(userId!!) { balance, cardId ->
            fetchPriceCaucao(placeName!!) { price ->
                val newBalance = (balance?.toDouble())?.minus(price.toDouble())
                Log.i(TAG, "Novo saldo: $newBalance")
                db.collection("pessoas")
                    .document(userId!!)
                    .collection("cartoes")
                    .document(cardId!!)
                    .update("balance", newBalance.toString())
                    .addOnSuccessListener {
                        Log.i(TAG, "Cobrança realizada com sucesso")
                        Toast.makeText(this, "Cobrado $price do cartão do usuário. Novo saldo: $newBalance", Toast.LENGTH_SHORT).show()
                        callback(true)
                    }.addOnFailureListener{
                        callback(false)
                        Log.e(TAG, "Erro ao debitar caução")
                        Toast.makeText(this, "Erro ao debitar caução", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun getActiveCardBalance(userId: String, callback: (String?, String?) -> Unit) {
        Log.d(TAG, "getUserBalance - userId: $userId")
        db.collection("pessoas")
            .document(userId)
            .collection("cartoes")
            .whereEqualTo("active", true)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val cartao = documents.documents[0]
                    val balance = cartao.getString("balance") ?: "0.0"
                    Log.d(TAG, "Active card balance: $balance")
                    callback(balance, cartao.id)
                } else {
                    Log.e(TAG, "Nenhum cartao encontrado para $userId")
                    callback(null, null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting user balance", exception)
                callback(null, null)
            }
    }

    private fun fetchLocacaoData(idLocacao: String) {
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

    private fun fetchPriceCaucao(placeName: String, callback: (String) -> Unit) {
        db.collection("estabelecimentos")
            .whereEqualTo("nome", placeName)
            .get()
            .addOnSuccessListener { documents ->
                for(document in documents){
                    val price = document.getString("ate_18h")!!
                    callback(price)
                }
            }
            .addOnFailureListener{
                Log.e(TAG, "Erro ao buscar preço do caução")
            }
    }

    private fun updateLocacaoStatus(locacaoId: String) {
        db.collection("locação")
            .document(locacaoId)
            .update("status", "Ativo")
            .addOnSuccessListener {
                Log.i(TAG, "Status da locação atualizado para ativo")
            }
            .addOnFailureListener(){
                Log.e(TAG, "Erro ao atualizar o status da locação")
            }
    }

    override fun onTagDiscovered(tag: Tag?) {
        tag?.let {
            detectedTag = it
            runOnUiThread {
                Toast.makeText(this, "Tag NFC detectada, pronta para escrita", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun createNdefMessage(payload: String): NdefMessage {
        val ndefRecord = NdefRecord.createTextRecord(null, payload)
        return NdefMessage(arrayOf(ndefRecord))
    }


    open fun createEmptyNdefMessage(): NdefMessage {
        val emptyPayload = ByteArray(0)
        val emptyNdefRecord = NdefRecord(NdefRecord.TNF_EMPTY, null, null, emptyPayload)
        return NdefMessage(arrayOf(emptyNdefRecord))
    }


    override fun writeNdefMessage(tag: Tag, ndefMessage: NdefMessage) {
        try {
            val ndef = android.nfc.tech.Ndef.get(tag)
            ndef.connect()
            if (ndef.isWritable) {
                ndef.writeNdefMessage(ndefMessage)
                ndef.close()
                runOnUiThread {
                    if(this::class.java == EndLocationActivity::class.java){
                        Toast.makeText(this, "Dados apagados", Toast.LENGTH_LONG).show()
                        return@runOnUiThread}
                    Toast.makeText(this, "Mensagem escrita com sucesso na tag NFC", Toast.LENGTH_LONG).show()
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "Tag NFC não é gravável", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "Erro ao gravar a mensagem NDEF", Toast.LENGTH_LONG).show()
            }
            }
        }
}
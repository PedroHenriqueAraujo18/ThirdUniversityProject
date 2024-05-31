package com.example.lockit

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.lockit.databinding.ActivityEndLocationBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.Int

class EndLocationActivity : WriteNFCActivity() {
    private lateinit var binding: ActivityEndLocationBinding
    private val db = Firebase.firestore
    private lateinit var idLocacao: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEndLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLimparDados.isEnabled = false
        binding.btnLimparDados.visibility = View.GONE

        val sharedPreferences = getSharedPreferences("idLocacao", Context.MODE_PRIVATE)
        idLocacao = sharedPreferences.getString("idLocacao", null) ?: ""
        Log.d(TAG, "idLocacao from SharedPreferences: $idLocacao")

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        binding.ivVoltar.setOnClickListener{
            val intent =Intent(this,ManagerMainActivity::class.java)
            startActivity(intent)
        }


        binding.btnRealizarEstorno.setOnClickListener {
            estorno{isEstornoConcluido ->
                updateLocacao(idLocacao){isRentalFinished ->
                    updateNumbersInPlace { isUpdated ->
                        if(isEstornoConcluido && isRentalFinished && isUpdated){
                            binding.btnRealizarEstorno.visibility = View.GONE
                            binding.btnRealizarEstorno.isEnabled = false
                            binding.btnLimparDados.visibility = View.VISIBLE
                            binding.btnLimparDados.isEnabled = true
                        }
                        else{
                            Toast.makeText(this, "Erro ao realizar estorno", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

        }
        binding.btnLimparDados.setOnClickListener {
            if (detectedTag != null) {
                cleanNFCData()
            } else {
                Toast.makeText(this, "Tag NFC não detectada", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun cleanNFCData() {
        detectedTag?.let {
            // Crie uma mensagem NDEF vazia
            val ndefMessage = createEmptyNdefMessage()

            // Escreva a mensagem na tag NFC
            writeNdefMessage(it, ndefMessage)
        } ?: run {
            Toast.makeText(this, "Tag NFC não detectada", Toast.LENGTH_SHORT).show()
        }
    }




    private fun updateLocacao(idLocacao: String?, callback: (Boolean) -> Unit) {
        db.collection("locação")
            .document(idLocacao!!)
            .update("status", "Finalizada")
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    private fun updateNumbersInPlace( callback: (Boolean) -> Unit) {
        db.collection("unidades de locação")
            .whereEqualTo("name", placeName)
            .get()
            .addOnSuccessListener { unidades ->
                if (!unidades.isEmpty) {
                    val unidade = unidades.documents[0]
                    var freeLockers = unidade.getLong("freeLockers")?.toInt() ?: 0
                    freeLockers++
                    unidade.reference.update("freeLockers", freeLockers)
                    callback(true)

                }else{
                    Log.d(TAG, "Nenhuma unidade de locação encontrada")
                    callback(false)
                }
            }
    }

    private fun estorno(callback: (Boolean) -> Unit){

        val sharedPreferences = getSharedPreferences("idLocacao", Context.MODE_PRIVATE)
        val locacaoId = sharedPreferences.getString("idLocacao", null)

        Log.d(TAG, "endLocacao - locacaoId: $locacaoId")
        if (locacaoId != null && locacaoId.isNotEmpty()) {
            db.collection("locação")
                .document(locacaoId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {

                        val placeName = document.getString("placeName")
                        val dataLocacao = document.getString("dataLocacao")
                        val precoAPagar = document.getLong("precoAPagar")?.toInt() ?: 0
                        val tempoLocacao = document.getString("tempoLocacao")
                        val userId = document.getString("userId")
                        Log.d(TAG, "dataLocacao: $dataLocacao, precoAPagar: $precoAPagar, tempoLocacao: $tempoLocacao, userId: $userId")
                        if (dataLocacao != null && tempoLocacao != null && userId != null ) {

                            fetchRentalPrices(placeName) { prices ->

                                if (tempoLocacao != "ate_18h") {

                                    val dateFormat =
                                        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR"))
                                    dateFormat.timeZone = TimeZone.getTimeZone("America/Sao_Paulo")

                                    val inicioDate = dateFormat.parse(dataLocacao)
                                    val fimDate = Date()

                                    Log.i(TAG, "Inicio: $inicioDate, fim: $fimDate")

                                    val diffInHours = ((fimDate.time - inicioDate.time) / (1000 * 60 * 60)).toFloat()

                                    Log.i(TAG, "diffInHours: $diffInHours")
                                    var diffInHoursInt: Int

                                    if (diffInHours <= 0.5) {
                                        diffInHoursInt = 0
                                    } else {
                                        diffInHoursInt = ceil(diffInHours).toInt()
                                    }

                                    val horasUsadas: String = when (diffInHoursInt) {
                                        0 -> "30 minutos"
                                        1 -> "1 hora"
                                        2 -> "2 horas"
                                        3 -> "4 horas"
                                        4 -> "4 horas"
                                        else -> "+ de 4 horas"
                                    }


                                    val horasLocadas = when (tempoLocacao) {
                                        "30 minutos" -> 0f
                                        "1 hora" -> 1f
                                        "2 horas" -> 2f
                                        "4 horas" -> 4f
                                        else -> 0f
                                    }

                                    if(diffInHoursInt <= horasLocadas ){
                                        val estorno = (prices["ate_18h"]?.minus(precoAPagar) ?: 0).toDouble() //retorna quanto deverá ser adicionado no balance
                                        getActiveCardBalance(userId){ balance, cardId ->
                                            val newBalance = balance?.toDouble()?.plus(estorno)
                                            updateUserBalance(userId, cardId, newBalance.toString()){balanceUpdated ->
                                                if(balanceUpdated){
                                                    callback(true)
                                                }else{
                                                    callback(false)
                                                }
                                            }
                                        }
                                    }else{
                                        prices[horasUsadas].let{ priceHorasUsadas ->
                                            if(horasUsadas == "+ de 4 horas"){
                                                //manter caução de até as 18h
                                                Log.i(TAG, "Tempo de locação maior que 4 horas, portanto será cobrado o valor do caução.")
                                                Toast.makeText(this, "Tempo de locação maior que 4 horas, portanto será cobrado o valor do caução.", Toast.LENGTH_SHORT).show()
                                            }else{

                                                val estorno = (prices["ate_18h"]?.minus(priceHorasUsadas!!) ?: 0)
                                                Log.i(TAG, "Estorno: $estorno")

                                                getActiveCardBalance(userId){ balance, cardId ->
                                                    val newBalance = balance?.plus(estorno.toDouble())
                                                    Log.i(TAG, "Novo balance: $newBalance")
                                                    updateUserBalance(userId, cardId, newBalance){ balanceUpdated ->
                                                        if(balanceUpdated){
                                                            callback(true)
                                                        }else{
                                                            callback(false)
                                                        }
                                                    }
                                                }
                                            }

                                        }
                                    }
                                }else {
                                    Log.i(TAG, "Tempo de locação é até as 18h, portanto não existe estorno")
                                    Toast.makeText(this, "Tempo de locação é até as 18h, portanto não existe estorno", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(this, "Dados de locação incompletos", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Locação não encontrada", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao buscar locação: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Erro ao buscar locação")
                }
        }else{
            Log.e(TAG, "ID de locação inválido")
            Toast.makeText(this, "ID de locação inválido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchRentalPrices(placeName: String?, callback: (Map<String, Int>) -> Unit) {

        val prices = mutableMapOf<String, Int>()
        db.collection("estabelecimentos").whereEqualTo("nome", placeName)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    prices["1h"] = document.getString("1h")?.toInt() ?: 0
                    prices["2h"] = document.getString("2h")?.toInt() ?: 0
                    prices["30min"] = document.getString("30min")?.toInt() ?: 0
                    prices["4h"] = document.getString("4h")?.toInt() ?: 0
                    prices["ate_18h"] = document.getString("ate_18h")?.toInt() ?: 0
                }
                Log.d(TAG, "Preços de aluguel recuperados: $prices")
                callback(prices)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao buscar preços de aluguel", e)
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
                    val balance = cartao.getString("balance")?: "0.0"
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


    private fun updateUserBalance(userId: String, cardId: String?, newBalance: String?, callback: (Boolean) -> Unit) {
        Log.d(TAG, "updateUserBalance - userId: $userId, newBalance: $newBalance")
        db.collection("pessoas")
            .document(userId)
            .collection("cartoes")
            .document(cardId!!)
            .update("balance", newBalance)
            .addOnSuccessListener {
                Log.i(TAG, "Balance atualizado com sucesso")
                Toast.makeText(this, "Estorno do caução devolvido.", Toast.LENGTH_SHORT).show()
                callback(true)
            }.addOnFailureListener{
                callback(false)
                Log.e(TAG, "Erro ao atualizar balance", it)
                Toast.makeText(this, "Erro ao realizar estorno", Toast.LENGTH_SHORT).show()
            }
    }
}
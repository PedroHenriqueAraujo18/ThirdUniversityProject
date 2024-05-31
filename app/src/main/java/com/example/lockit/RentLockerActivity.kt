package com.example.lockit

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import com.example.lockit.databinding.ActivityRentLockerBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone


open class RentLockerActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityRentLockerBinding
    private var opcao: Int = 0
    private var cartaoAtivo: CartaoAtivo? = null
    private var placeName: String? = null
    private lateinit var countDownTimer: CountDownTimer
    private var ultimoArmario: Armario? = null

    data class Armario(
        var idLocacao: String,
        val userId: String,
        val placeName: String,
        val tempoLocacao: String,
        val precoAPagar: Int,
        val dataLocacao: String,
        var numDeUsuarios: Int?,
        var status: String?,
        var fotoId: String?,
        var number: Int?
    )

    data class CartaoAtivo(
        var cardId: String,
        val cardHolderId: String,
        val cardNumber: String,
        var expirationDate: String,
        var balance: String,
        var cvv: String
    )

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRentLockerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        val currentUser = auth.currentUser


        checkTime()

        binding.ivVoltar.setOnClickListener {
            val intent = Intent(this, ClientMainActivity::class.java)
            startActivity(intent)
        }


        binding.btnConfirmar.setOnClickListener {
            getCartao(currentUser?.uid) { cartaoEncontrado, balance, cardHolderId ->
                if (!cartaoEncontrado) {
                    binding.tvCartaoNaoSelecionado.visibility = android.view.View.VISIBLE
                } else {
                    binding.tvCartaoNaoSelecionado.visibility = android.view.View.GONE
                    if (opcao != -1) {
                        fetchRentalPrices { prices ->
                            // Quando os preços forem obtidos, prosseguimos com a transação
                            val requiredAmount = when (opcao) {
                                0 -> prices["30min"]
                                1 -> prices["1h"]
                                2 -> prices["2h"]
                                3 -> prices["4h"]
                                4 -> prices["ate_18h"]
                                else -> null
                            }

                            if (requiredAmount != null && currentUser != null && cartaoAtivo != null && balance!=null) {

                                //já faz a verificação de saldo do usuário
                                checkUserBalance(currentUser.uid, requiredAmount, balance) { hasBalance ->
                                    if (hasBalance) {
                                        continueRent(currentUser.uid, requiredAmount)
                                    } else {
                                        Toast.makeText(
                                            this,
                                            "Saldo insuficiente. Por favor, adicione mais fundos ao seu cartão.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }

                    } else {
                        Toast.makeText(this, "Selecione uma opção de locação", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }


        // Listener para os RadioButtons para atualizar a variável 'opcao'
        if(binding.rgOpcoes.checkedRadioButtonId == -1){
            opcao = -1
        }
        val radioButtons = listOf(binding.rb30m, binding.rb1h, binding.rb2h, binding.rb4h, binding.rbAte18h)
        radioButtons.forEachIndexed { index, radioButton ->
            radioButton.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) opcao = index
            }
        }


        //leva para o layout
        fetchRentalPrices { prices ->
            binding.rb30m.text = "30 min - R$${prices["30min"]}"
            binding.rb1h.text = "1 hora - R$${prices["1h"]}"
            binding.rb2h.text = "2 horas - R$${prices["2h"]}"
            binding.rb4h.text = "4 horas - R$${prices["4h"]}"
            binding.rbAte18h.text = "Até as 18h - R$${prices["ate_18h"]}"

        }
    }


    private fun getCartao(uid: String?, callback: (Boolean, String?, String?) -> Unit) {
        db.collection("pessoas")
            .document(uid!!)
            .collection("cartoes")
            .whereEqualTo("active", true)
            .limit(1)
            .get()
            .addOnSuccessListener { cartoes ->
                if (!cartoes.isEmpty){
                    Log.d(TAG, "Sucesso ao buscar cartao ativo")
                    val cartao = cartoes.documents[0]
                    cartaoAtivo = CartaoAtivo(
                        cardId = cartao.getString("cardId") ?: "",
                        cardHolderId = cartao.getString("cardHolderId") ?: "",
                        cardNumber = cartao.getString("cardNumber") ?: "",
                        expirationDate = cartao.getString("expDate") ?: "",
                        balance = cartao.getString("balance") ?: "1000.0",
                        cvv = cartao.getString("cvv") ?: "123"
                    )
                    Log.w(TAG, "Cartão ativo: $cartaoAtivo")
                    callback(true, cartaoAtivo!!.balance, cartaoAtivo!!.cardHolderId) // Indica sucesso através do callback
                } else {
                    Log.d(TAG, "Nenhum cartão ativo encontrado")
                    callback(false, "0.0", "") // Indica falha através do callback
                }
            }.addOnFailureListener() { e ->
                Log.e(TAG, "Erro ao buscar cartão ativo", e)
                callback(false,"0.0", "") // Indica falha através do callback
            }
    }


    private fun checkUserBalance(uId: String, requiredAmount: Int, balance: String?, callback: (Boolean) -> Unit) {

        if (requiredAmount< balance?.toDouble() ?: 0.0){
            callback(true)
        } else {
            callback(false)
        }
    }


    private fun continueRent(uId: String, amount: Int) {

        //pega o que será o número do armário e instancia um objeto Armario
        getLockerNumber { number ->
            if(number != -1){
                ultimoArmario = Armario(
                    idLocacao = "",
                    userId = uId,
                    placeName = placeName ?: "",
                    tempoLocacao = when (opcao) {
                        0 -> "30 minutos"
                        1 -> "1 hora"
                        2 -> "2 horas"
                        3 -> "4 horas"
                        4 -> "Até as 18h"
                        else -> ""
                    },
                    precoAPagar = amount,
                    dataLocacao = getCurrentDateTime(),
                    numDeUsuarios = null,
                    status = "Pendente",
                    fotoId = "",
                    number = number
                )
                saveLocacaoToFirestore(ultimoArmario!!)
            }else{
                Toast.makeText(
                    applicationContext,
                    "Nenhum armário disponível. Tente novamente mais tarde.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR"))
        dateFormat.timeZone = TimeZone.getTimeZone("America/Sao_Paulo") // Define o fuso horário para São Paulo, Brasil
        val date = Date()
        return dateFormat.format(date)
    }




    private fun saveLocacaoToFirestore(locacao: Armario) {

        db.collection("locação")
            .add(locacao)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Locação salva com ID: ${documentReference.id}")
                /*Toast.makeText(
                    applicationContext,
                    "Locação bem-sucedida. ID da locação: ${documentReference.id}",
                    Toast.LENGTH_SHORT
                ).show()*/

                val sharedPreferences = getSharedPreferences("getLocacao", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("idLocacao", documentReference.id) // Aqui o ID de locação é salvo
                editor.apply()

                val intent = Intent(this, DisplayLockerInfoActivity::class.java)
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao salvar locação", e)
                Toast.makeText(
                    applicationContext,
                    "Erro ao salvar locação. Tente novamente mais tarde.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun getLockerNumber( callback: (Int) -> Unit) {

        updateNumbersInPlace{ isPlaceUpdated ->
            if(isPlaceUpdated){
                db.collection("unidades de locação")
                    .whereEqualTo("name", placeName)
                    .get()
                    .addOnSuccessListener {unidades ->
                        if(!unidades.isEmpty){
                            val unidade = unidades.documents[0]
                            val totalLockers = unidade.getLong("totalLockers")?.toInt() ?: 0
                            val freeLockers = unidade.getLong("freeLockers")?.toInt() ?: 0
                            val lockerNumber = totalLockers-freeLockers
                            callback(lockerNumber)
                        }
                    }
            }else{
                callback(-1) //retorna -1 caso nenhum armário esteja livre
            }
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
                    if(freeLockers>0){
                        freeLockers--
                        unidade.reference.update("freeLockers", freeLockers)
                        callback(true)
                    }else{
                        Log.d(TAG, "Nenhum armário livre")
                        callback(false)
                    }
                }else{
                    Log.d(TAG, "Nenhuma unidade de locação encontrada")
                    callback(false)
                }
            }
    }

    private fun fetchRentalPrices(callback: (Map<String, Int>) -> Unit) {
        placeName = intent.getStringExtra("placeName")
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

    private fun checkTime(){
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        binding.rb30m.isEnabled = true
        binding.rb1h.isEnabled = true
        binding.rb2h.isEnabled = true
        binding.rb4h.isEnabled = true
        binding.rbAte18h.isEnabled = false

        if(hour in 7..8 ){
            binding.rbAte18h.isEnabled = true
        }else{
            binding.rbAte18h.isEnabled = false
        }

        if(hour in 14 .. 18 ){
            binding.rb30m.isEnabled = true
            binding.rb1h.isEnabled = true
            binding.rb2h.isEnabled = true
            binding.rb4h.isEnabled = false
            binding.rbAte18h.isEnabled = false
        }

         if (hour in 16..18 ){
            binding.rb30m.isEnabled = true
            binding.rb1h.isEnabled = true
            binding.rb2h.isEnabled = false
            binding.rb4h.isEnabled = false
            binding.rbAte18h.isEnabled = false
        }

         if (hour in 17..18){
            binding.rb30m.isEnabled = true
            binding.rb1h.isEnabled = false
            binding.rb2h.isEnabled = false
            binding.rb4h.isEnabled = false
            binding.rbAte18h.isEnabled = false
        }
         if( hour == 17 && minute <= 30 && minute >= 0){
            binding.rb30m.isEnabled = true
            binding.rb1h.isEnabled = false
            binding.rb2h.isEnabled = false
            binding.rb4h.isEnabled = false
            binding.rbAte18h.isEnabled = false
        }

        if (hour >=18){
            binding.rb30m.isEnabled = false
            binding.rb1h.isEnabled = false
            binding.rb2h.isEnabled = false
            binding.rb4h.isEnabled = false
            binding.rbAte18h.isEnabled = false
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer.cancel()
        }
}
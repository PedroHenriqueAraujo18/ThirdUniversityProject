package com.example.lockit

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lockit.databinding.ActivityLockerInfoBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter

class DisplayLockerInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockerInfoBinding
    private val db = Firebase.firestore
    var placeName: String? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLockerInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivVoltar.setOnClickListener {
            val intent = Intent(this, ClientMainActivity::class.java)
            startActivity(intent)
        }

        val sharedPreferences = getSharedPreferences("getLocacao", Context.MODE_PRIVATE)
        val idLocacao = sharedPreferences.getString("idLocacao", null)

        val bitmap = qrCodeMaker(idLocacao!!)
        binding.ivQRCode.setImageBitmap(bitmap)

        fetchLocacaoFromFirestore(idLocacao) { armario ->
            armario?.let { locacao ->
                placeName = locacao.placeName
                Log.i(TAG, "Locação encontrada: $locacao")

                if(locacao.status == "Pendente"){
                    binding.btnCancelarAluguel.isEnabled = true
                    val infoLocacao = "Local: ${locacao.placeName}\n" +
                        "Tempo de locação: ${locacao.tempoLocacao}\n" +
                        "Preço a pagar: R$${locacao.precoAPagar}\n" +
                        "Data de locação: ${locacao.dataLocacao}\n" +
                        "Número do armário: ${locacao.number}\n" +
                        "Status: ${locacao.status}"
                    binding.tvInfoLocacao.text = infoLocacao
                }else if(locacao.status == "Ativo"){
                    binding.btnCancelarAluguel.isEnabled = false
                    binding.btnCancelarAluguel.visibility = View.GONE
                    val infoLocacao = "Local: ${locacao.placeName}\n" +
                            "Tempo de locação: ${locacao.tempoLocacao}\n" +
                            "Preço pago: R$${locacao.precoAPagar}\n" +
                            "Data de locação: ${locacao.dataLocacao}\n" +
                            "Número do armário: ${locacao.number}\n" +
                            "Status: ${locacao.status}"
                    binding.tvInfoLocacao.text = infoLocacao
                }

            } ?: run {
                binding.tvInfoLocacao.text = "Nenhuma informação de locação disponível."
            }
        }


        binding.btnCancelarAluguel.setOnClickListener {
            deleteLocacao(idLocacao!!)
            Toast.makeText(this, "Armario desalugado com sucesso.", Toast.LENGTH_LONG).show()
            updateNumbersInPlace { success ->
                if (success) {
                    Log.i(TAG, "Número de armários atualizado com sucesso.")
                    val intent = Intent(this, ClientMainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Log.e(TAG, "Erro ao atualizar número de armários.")
                }
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
                    freeLockers++
                    unidade.reference.update("freeLockers", freeLockers)
                    callback(true)
                }else{
                    Log.d(TAG, "Nenhuma unidade de locação encontrada")
                    callback(false)
                }
            }
    }

    private fun qrCodeMaker(idLocacao: String): Bitmap {
        val writer = QRCodeWriter()
        try {
            val bitMatrix = writer.encode(idLocacao, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                    )
                }
            }
            return bitmap
        } catch (e: WriterException) {
            Log.e(TAG, "Erro ao gerar QR Code", e)
            return Bitmap.createBitmap(0, 0, Bitmap.Config.RGB_565)
        }
    }


    private fun fetchLocacaoFromFirestore(idLocacao: String?, callback: (RentLockerActivity.Armario?) -> Unit) {
        db.collection("locação")
            .document(idLocacao!!)
            .get()
            .addOnSuccessListener { locacao ->
                if (locacao.exists()) {
                    Log.i(TAG, "Locação encontrada: $locacao")
                    val locacao = RentLockerActivity.Armario(
                        idLocacao = locacao.id,
                        userId = locacao.getString("userId") ?: "",
                        placeName = locacao.getString("placeName") ?: "",
                        tempoLocacao = locacao.getString("tempoLocacao") ?: "",
                        precoAPagar = locacao.getLong("precoAPagar")?.toInt() ?: 0,
                        dataLocacao = locacao.getString("dataLocacao") ?: "",
                        numDeUsuarios = locacao.getLong("numDeUsuarios")?.toInt() ?: 0,
                        status = locacao.getString("status") ?: "",
                        fotoId = locacao.getString("fotoId") ?: "",
                        number = locacao.getLong("number")?.toInt() ?: 0
                    )
                    Log.i(TAG, "Locação instanciada: $locacao")
                    callback(locacao)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(ContentValues.TAG, "Erro ao buscar informações de locação", e)
                callback(null)
            }
    }

    private fun deleteLocacao(idLocacao: String) {
        db.collection("locação")
            .document(idLocacao)
            .delete()
            .addOnSuccessListener {
                Log.d(ContentValues.TAG, "Locação deletada com sucesso.")
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(ContentValues.TAG, "Erro ao deletar locação", e)
                }
        }

}
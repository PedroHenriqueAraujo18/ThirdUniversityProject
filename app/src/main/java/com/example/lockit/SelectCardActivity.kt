package com.example.lockit

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.example.lockit.databinding.ActivitySelectCardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class SelectCardActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectCardBinding
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    private var cartaoSelecionado: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectCardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupCardList()

        binding.btnDeleteCard.setOnClickListener {
            if (cartaoSelecionado != null) {
                deletCard(cartaoSelecionado!!)
                Toast.makeText(this, "Cartão deletado com sucesso.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Selecione um cartão para deletar.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnRegisterCard.setOnClickListener {
            val intent = Intent(this, RegisterCardActivity::class.java)
            startActivity(intent)
        }

        binding.ivVoltar.setOnClickListener {
            val intent = Intent(this, ClientMainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun updateCardSelection(currentUser: FirebaseUser, selectedCardId: String, documents: QuerySnapshot) {
        db.collection("pessoas")
            .document(currentUser.uid)
            .collection("cartoes")
            .document(selectedCardId)
            .update("active", true)
            .addOnSuccessListener {
                Log.d("SelectCard", "Cartão selecionado atualizado com sucesso.")
            }
            .addOnFailureListener { exception ->
                Log.d("SelectCard", "Erro ao atualizar o cartão selecionado", exception)
            }

        for (doc in documents) {
            if (doc.id != selectedCardId) {
                db.collection("pessoas")
                    .document(currentUser.uid)
                    .collection("cartoes")
                    .document(doc.id)
                    .update("active", false)
                    .addOnSuccessListener {
                        Log.d("SelectCard", "Cartão desativado com sucesso.")
                    }
                    .addOnFailureListener { exception ->
                        Log.d("SelectCard", "Erro ao desativar o cartão", exception)
                    }
            }
        }
    }

    private fun setupCardList() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("pessoas")
                .document(currentUser.uid)
                .collection("cartoes")
                .get()
                .addOnSuccessListener { documents ->
                    val cartoes = ArrayList<String>()
                    var activeCardPosition = -1

                    if (documents.isEmpty) {
                        binding.tvNoCardsMessage.visibility = View.VISIBLE
                    } else {
                        binding.tvNoCardsMessage.visibility = View.GONE
                        documents.forEachIndexed{ index, document ->
                            val numeroCartao = document.getString("number")
                            val nomeTitular = document.getString("cardHolderName")
                            val numeroCartaoMascarado = maskCardNumber(numeroCartao)
                            val opcao = "$numeroCartaoMascarado - $nomeTitular"
                            if (document.getBoolean("active") == true) {
                                activeCardPosition = index
                            }
                            cartoes.add(opcao)
                        }
                    }

                    val adapter = CardArrayAdapter(this, cartoes, activeCardPosition)
                    binding.listViewCartoes.adapter = adapter
                    binding.listViewCartoes.choiceMode = ListView.CHOICE_MODE_SINGLE
                    binding.listViewCartoes.setOnItemClickListener { parent, view, position, id ->
                        cartaoSelecionado = documents.documents[position].id
                        activeCardPosition= position
                        updateCardSelection(currentUser, cartaoSelecionado!!, documents)
                        binding.listViewCartoes.adapter = CardArrayAdapter(this, cartoes, activeCardPosition)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("SelectCard", "Erro ao recuperar os cartões do usuário", exception)
                    Toast.makeText(this, "Erro ao recuperar os cartões do usuário.", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun deletCard(cardId: String) {
        db.collection("pessoas")
            .document(auth.currentUser!!.uid)
            .collection("cartoes")
            .document(cardId)
            .delete()
            .addOnSuccessListener {
                Log.d("SelectCard", "Cartão deletado com sucesso.")
                setupCardList()
            }
            .addOnFailureListener { exception ->
                Log.d("SelectCard", "Erro ao deletar o cartão", exception)
            }
    }

    private fun maskCardNumber(numeroCartao: String?): String {
        val primeirosNum = numeroCartao?.substring(0, 6)
        val restante = numeroCartao?.substring(6)?.replace("\\d".toRegex(), "*")
        return "$primeirosNum$restante"
    }
}

class CardArrayAdapter(context: Context, private val cartoes: List<String>, private val activeCardPosition: Int)
    : ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, cartoes) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView
        if (position == activeCardPosition) {
            view.setBackgroundColor(Color.GRAY)  // Destaque para o cartão ativo
        } else {
            view.setBackgroundColor(Color.WHITE)  // Destaque para o cartão ativo
        }
        return view
    }
}


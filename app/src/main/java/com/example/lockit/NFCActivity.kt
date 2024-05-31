package com.example.lockit
import android.app.Activity
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.widget.Toast


open class NfcActivity : Activity(), NfcAdapter.ReaderCallback {

    lateinit var nfcAdapter: NfcAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC não é suportado neste dispositivo.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        nfcAdapter.enableReaderMode(this, this,
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS, null)
    }

    override fun onTagDiscovered(tag: Tag?) {
        //Override esse método nas subclasses em que a tag NFC é utilizada
    }

    protected open fun readNdefMessage(tag: Tag): NdefMessage? {
        val ndef = Ndef.get(tag)
        return if (ndef != null) {
            ndef.connect()
            val message = ndef.ndefMessage
            ndef.close()
            message
        } else {
            null
        }
    }

    // Função para criar uma mensagem NDEF com base nos
    open fun createNdefMessage(payload: String): NdefMessage {
        val ndefRecord = NdefRecord.createTextRecord("", payload)
        return NdefMessage(arrayOf(ndefRecord))
    }


    //função de escrita dos dados criados
    open fun writeNdefMessage(tag: Tag, ndefMessage: NdefMessage) {
        val ndef = Ndef.get(tag)
        ndef?.let {
            it.connect()
            if (it.isWritable) {
                it.writeNdefMessage(ndefMessage)
                runOnUiThread {
                    Toast.makeText(this, "Tag NFC escrita com sucesso", Toast.LENGTH_LONG).show()
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "Tag NFC não é gravável", Toast.LENGTH_LONG).show()
                }
            }
            it.close()
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter.disableReaderMode(this)
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter.enableReaderMode(this, this,
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS, null)
    }


}
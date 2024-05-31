package com.example.lockit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.lockit.databinding.ActivityQrCodeScannerBinding
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QrCodeScannerActivity : AppCompatActivity() {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var imgCaptureExecutor: ExecutorService
    private lateinit var binding: ActivityQrCodeScannerBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var cameraSelector: CameraSelector
    private lateinit var meuHandler: Handler
    private var imageAnalysis: ImageAnalysis? = null
    private var qrCodeValue: String? = null
    private val db = FirebaseFirestore.getInstance()
    private var toastShown: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityQrCodeScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        meuHandler = Handler(Looper.myLooper()!!)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        imgCaptureExecutor = Executors.newSingleThreadExecutor()
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        barcodeScanner = BarcodeScanning.getClient()
        cameraExecutor = Executors.newSingleThreadExecutor()

        startCamera()

        binding.ivVoltar.setOnClickListener{
            val intent = Intent(this, ManagerMainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun startCamera() {
        cameraProviderFuture.addListener({

            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, { imageProxy -> analyzeImage(imageProxy) })
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (e: Exception) {
                Log.e("CameraPreview", "Erro de camera")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analyzeImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    qrCodeValue = barcode.rawValue
                    Log.d("QrCodeScanner", "QR Code Value: $qrCodeValue")
                    verifyLocacao(qrCodeValue!!)
                }
                imageProxy.close()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Falha ao detectar QR Code", Toast.LENGTH_SHORT).show()
                imageProxy.close()
            }
    }


    private fun verifyLocacao(qrCodeValue: String){
        db.collection("locação")
            .document(qrCodeValue)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {

                    val sharedPreferences = getSharedPreferences("idLocacao", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()

                    editor.putString("idLocacao", qrCodeValue) // Aqui o ID de locação é salvo
                    editor.apply()

                    if (!toastShown) {
                        Toast.makeText(this, "Locação Encontrada", Toast.LENGTH_LONG).show()
                        toastShown = true
                    }

                    Log.d("QrCodeScanner", "Locação encontrada")
                    val intent = Intent(this, SelectNumberOfUsersActivity::class.java).apply {
                        putExtra("locacaoId", qrCodeValue)
                    }
                    startActivity(intent)
                }else{

                    Toast.makeText(this, "Locação não encontrada ", Toast.LENGTH_SHORT).show()
                    Log.e("QrCodeScanner", "Locação não encontrada")
                }
            }
            .addOnFailureListener { exception ->
                Log.w("QrCodeScanner", "Erro ao buscar localização", exception)
            }
    }


    override fun onDestroy() {
        super.onDestroy()
        imgCaptureExecutor.shutdown()
        cameraExecutor.shutdown()
    }
}

package com.example.lockit

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.lockit.databinding.ActivityCameraPreviewBinding
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraPreviewActivity : AppCompatActivity() {

    private val permissionId = 1001

    private lateinit var binding: ActivityCameraPreviewBinding

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraSelector: CameraSelector
    private var imageCapture: ImageCapture? = null
    private lateinit var imgCaptureExecutor: ExecutorService

    private val storage = FirebaseStorage.getInstance()
    private var capturedBitmap: Bitmap? = null
    private lateinit var locacaoId: String
    private val db = Firebase.firestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCameraPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        imgCaptureExecutor = Executors.newSingleThreadExecutor()
        locacaoId = intent.getStringExtra("locacaoId") ?: ""

        if (checkPermissionsCamera()) {
            startCamera()
        } else {
            requestCameraPermission()
        }

        binding.btnTakePhoto.setOnClickListener {
            takePhoto()
        }

        binding.btnSalvarFoto.setOnClickListener {
            capturedBitmap?.let {
                handleImageBitmap(it)
            }
            val intent = Intent(this, WriteNFCActivity::class.java)
            startActivity(intent)
        }

        binding.btnTirarNovamente.setOnClickListener {
            restartCamera()
        }

        binding.ivVoltar.setOnClickListener{
            val intent = Intent(this, ManagerMainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun startCamera() {
        cameraProviderFuture.addListener({
            imageCapture = ImageCapture.Builder().build()

            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.e("CameraPreview", "Erro ao inicializar a câmera", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(imgCaptureExecutor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = imageProxyToBitmap(image)
                capturedBitmap = correctImageOrientation(bitmap)
                runOnUiThread {
                    binding.previewView.visibility = View.GONE
                    binding.ivFotoTirada.setImageBitmap(capturedBitmap)
                    binding.ivFotoTirada.visibility = View.VISIBLE
                    binding.btnTakePhoto.visibility = View.GONE
                    binding.btnSalvarFoto.visibility = View.VISIBLE
                    binding.btnTirarNovamente.visibility = View.VISIBLE
                }
                image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraPreview", "Erro ao capturar imagem: ${exception.message}", exception)
            }
        })
    }

    private fun restartCamera() {
        binding.previewView.visibility = View.VISIBLE
        binding.ivFotoTirada.visibility = View.GONE
        binding.btnTakePhoto.visibility = View.VISIBLE
        binding.btnSalvarFoto.visibility = View.GONE
        binding.btnTirarNovamente.visibility = View.GONE
        capturedBitmap = null
        startCamera()
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun correctImageOrientation(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(90f) // Ajuste de rotação para correção de orientação
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun handleImageBitmap(imageBitmap: Bitmap) {
        val fileName = "image_${System.currentTimeMillis()}.jpg"
        val imageId = UUID.randomUUID().toString()
        val storageRef = storage.reference.child("images/$imageId.jpg")
        val baos = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val uploadTask = storageRef.putBytes(data)
        uploadTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {

                val sharedPreferences = getSharedPreferences("idLocacao", Context.MODE_PRIVATE)
                val idLocacao = sharedPreferences.getString("idLocacao", null)

                updateFotoIdLocacao(idLocacao!!, imageId) {
                    if(it){
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                            val downloadUrl = uri.toString()
                            Log.d("CameraPreview", "Download URL: $downloadUrl")

                            Glide.with(this)
                                .load(downloadUrl)
                                .error(R.drawable.ic_launcher_background)
                                .into(binding.ivFotoTirada)

                            val toast = Toast.makeText(this, "Imagem salva com sucesso", Toast.LENGTH_SHORT)
                            toast.show()

                            Handler(Looper.getMainLooper()).postDelayed({
                                // Redireciona após o Toast ser exibido
                                val intent = Intent(this, WriteNFCActivity::class.java).apply {
                                    putExtra("locacaoId", locacaoId)}
                                startActivity(intent) // Opcional: Finaliza a atividade atual se necessário
                            }, toast.duration.toLong() + 500)
                        }
                    }else{
                        Toast.makeText(this, "Erro ao atualizar foto da locação", Toast.LENGTH_SHORT).show()
                    }
                }

            } else {
                val exception = task.exception
                Log.e("CameraPreview", "Erro ao fazer upload da imagem", exception)
            }
        }
    }

    private fun updateFotoIdLocacao(idLocacao: String, fotoId: String, callback: (Boolean) -> Unit) {
        db.collection("locação").document(idLocacao)
            .update("fotoId", fotoId)
            .addOnSuccessListener {
                Log.d("CameraPreview", "Documento salvo!")
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.w("CameraPreview", "Erro em salvar o documento", e)
                callback(false)
            }

    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.CAMERA),
            permissionId
        )
    }

    private fun isCameraPermissionEnabled(): Boolean {
        val permission = android.Manifest.permission.CAMERA
        val result = ContextCompat.checkSelfPermission(this, permission)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun checkPermissionsCamera(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            permissionId -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    startCamera()
                } else {
                    Toast.makeText(this, "Permissão de acesso à câmera negada.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
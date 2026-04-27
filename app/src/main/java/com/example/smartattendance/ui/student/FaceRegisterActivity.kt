package com.example.smartattendance.ui.student

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.smartattendance.R
import com.example.smartattendance.ai.FaceRecognitionHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.io.File

class FaceRegisterActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var btnRegister: Button
    private lateinit var imageCapture: ImageCapture
    private lateinit var faceHelper: FaceRecognitionHelper

    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraReady = false
    private var isCapturing = false

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) previewView.post { startCamera() } else finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_register)

        val mode = intent.getStringExtra("mode") ?: "register"

        previewView = findViewById(R.id.previewView)
        btnRegister = findViewById(R.id.btnRegister)
        faceHelper = FaceRecognitionHelper(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            previewView.post { startCamera() }
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }

        btnRegister.setOnClickListener {

            if (!cameraReady) {
                Toast.makeText(this, "Camera chưa sẵn sàng, chờ 1 giây", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isCapturing) return@setOnClickListener
            isCapturing = true
            btnRegister.isEnabled = false

            captureAndRegister(mode) {
                isCapturing = false
                btnRegister.isEnabled = true
            }
        }
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)

        future.addListener({
            cameraProvider = future.get()

            val rotation = previewView.display.rotation

            val preview = Preview.Builder()
                .setTargetRotation(rotation)
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            imageCapture = ImageCapture.Builder()
                .setTargetRotation(rotation)
                .build()

            try {
                cameraProvider?.unbindAll()

                val camera = cameraProvider?.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    imageCapture
                )

                camera?.cameraInfo?.cameraState?.observe(this) { state ->
                    cameraReady = (state.type == CameraState.Type.OPEN)
                }

            } catch (e: Exception) {
                cameraReady = false
                Toast.makeText(this, "Camera lỗi: ${e.message}", Toast.LENGTH_LONG).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureAndRegister(mode: String, onDone: () -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            onDone()
            finish()
            return
        }

        val file = File(externalCacheDir, "register_face.jpg")
        val options = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(
            options,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap == null) {
                        Toast.makeText(this@FaceRegisterActivity, "Đọc ảnh thất bại", Toast.LENGTH_SHORT).show()
                        onDone()
                        return
                    }

                    val embedding = faceHelper.getEmbedding(bitmap)

                    val userRef = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)

                    userRef.set(
                        mapOf("faceEmbedding" to embedding.toList()),
                        SetOptions.merge()
                    ).addOnSuccessListener {
                        Toast.makeText(
                            this@FaceRegisterActivity,
                            if (mode == "update_face") "Cập nhật khuôn mặt thành công"
                            else "Đăng ký khuôn mặt thành công",
                            Toast.LENGTH_LONG
                        ).show()
                        onDone()
                        finish()
                    }.addOnFailureListener {
                        Toast.makeText(this@FaceRegisterActivity, "Lưu thất bại", Toast.LENGTH_LONG).show()
                        onDone()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        this@FaceRegisterActivity,
                        "Chụp ảnh thất bại: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    onDone()
                }
            }
        )
    }

    override fun onStop() {
        super.onStop()
        cameraProvider?.unbindAll()
        cameraReady = false
    }
}

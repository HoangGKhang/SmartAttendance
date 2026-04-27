package com.example.smartattendance.ui.student

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.smartattendance.R
import com.example.smartattendance.ai.FaceRecognitionHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.File
import java.util.concurrent.Executors

class FaceVerifyActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var btnCapture: Button
    private lateinit var tvLivenessHint: TextView

    private lateinit var imageCapture: ImageCapture
    private lateinit var imageAnalysis: ImageAnalysis

    private lateinit var faceHelper: FaceRecognitionHelper
    private var cameraProvider: ProcessCameraProvider? = null

    private var cameraReady = false
    private var isCapturing = false

    private enum class BlinkState { WAIT_OPEN, WAIT_CLOSE, WAIT_OPEN_AGAIN, PASSED }

    private var blinkState = BlinkState.WAIT_OPEN
    private var livenessPassed = false

    private val challengeWindowMs = 5000L
    private var challengeStartElapsed = 0L

    private val OPEN_TH = 0.75f
    private val CLOSE_TH = 0.35f

    private var lastDetectElapsed = 0L
    private val detectIntervalMs = 180L

    private val analysisExecutor = Executors.newSingleThreadExecutor()

    private val faceDetector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // cần eye prob
            .enableTracking()
            .build()
        FaceDetection.getClient(options)
    }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) previewView.post { startCamera() } else finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_verify)

        previewView = findViewById(R.id.previewView)
        btnCapture = findViewById(R.id.btnCapture)
        tvLivenessHint = findViewById(R.id.tvLivenessHint)

        faceHelper = FaceRecognitionHelper(this)

        resetBlinkChallenge()

        val mode = intent.getStringExtra("mode") ?: "attendance"
        val sessionId = if (mode == "attendance") {
            intent.getStringExtra("sessionId") ?: run { finish(); return }
        } else ""

        btnCapture.isEnabled = false

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            previewView.post { startCamera() }
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }

        btnCapture.setOnClickListener {
            if (!cameraReady) {
                Toast.makeText(this, "Camera chưa sẵn sàng", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!livenessPassed) {
                Toast.makeText(this, "Vui lòng blink trước", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (isCapturing) return@setOnClickListener

            isCapturing = true
            btnCapture.isEnabled = false

            captureAndVerify(mode, sessionId) {
                isCapturing = false
                btnCapture.isEnabled = livenessPassed
            }
        }
    }

    private fun resetBlinkChallenge() {
        livenessPassed = false
        blinkState = BlinkState.WAIT_OPEN
        challengeStartElapsed = SystemClock.elapsedRealtime()
        tvLivenessHint.text = "Hãy CHỚP MẮT 1 lần trong 5 giây"
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            cameraProvider = future.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                analyzeLivenessBlink(imageProxy)
            }

            try {
                cameraProvider?.unbindAll()

                cameraProvider?.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    imageCapture,
                    imageAnalysis
                )

                cameraReady = true

            } catch (e: Exception) {
                cameraReady = false
                Toast.makeText(this, "Camera lỗi: ${e.message}", Toast.LENGTH_LONG).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    @OptIn(ExperimentalGetImage::class)
    private fun analyzeLivenessBlink(imageProxy: ImageProxy) {
        try {
            val now = SystemClock.elapsedRealtime()

            // throttle
            if (now - lastDetectElapsed < detectIntervalMs) {
                imageProxy.close()
                return
            }
            lastDetectElapsed = now

            if (livenessPassed) {
                imageProxy.close()
                return
            }

            // timeout
            if (now - challengeStartElapsed > challengeWindowMs) {
                runOnUiThread {
                    Toast.makeText(this, "Hết thời gian, hãy thử lại", Toast.LENGTH_SHORT).show()
                    resetBlinkChallenge()
                }
                imageProxy.close()
                return
            }

            val mediaImage = imageProxy.image ?: run {
                imageProxy.close()
                return
            }

            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    val face = faces.firstOrNull()
                    if (face == null) {
                        imageProxy.close()
                        return@addOnSuccessListener
                    }

                    val l = face.leftEyeOpenProbability ?: -1f
                    val r = face.rightEyeOpenProbability ?: -1f

                    val bothOpen = l >= OPEN_TH && r >= OPEN_TH
                    val bothClosed = (l in 0f..CLOSE_TH) && (r in 0f..CLOSE_TH)

                    when (blinkState) {
                        BlinkState.WAIT_OPEN -> {
                            if (bothOpen) blinkState = BlinkState.WAIT_CLOSE
                        }
                        BlinkState.WAIT_CLOSE -> {
                            if (bothClosed) blinkState = BlinkState.WAIT_OPEN_AGAIN
                        }
                        BlinkState.WAIT_OPEN_AGAIN -> {
                            if (bothOpen) {
                                blinkState = BlinkState.PASSED
                                livenessPassed = true
                                runOnUiThread {
                                    tvLivenessHint.text = "Liveness PASS (Blink)"
                                    Toast.makeText(this, "Blink OK, Bạn có thể chụp", Toast.LENGTH_SHORT).show()
                                    btnCapture.isEnabled = true
                                }
                            }
                        }
                        BlinkState.PASSED -> Unit
                    }

                    imageProxy.close()
                }
                .addOnFailureListener {
                    imageProxy.close()
                }

        } catch (_: Exception) {
            imageProxy.close()
        }
    }

    private fun captureAndVerify(
        mode: String,
        sessionId: String,
        onDone: () -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            onDone()
            finish()
            return
        }

        val file = File(externalCacheDir, "verify_face.jpg")
        val options = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(
            options,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: run {
                        Toast.makeText(this@FaceVerifyActivity, "Không đọc được ảnh", Toast.LENGTH_LONG).show()
                        onDone()
                        return
                    }

                    val liveEmbedding = faceHelper.getEmbedding(bitmap)

                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .get()
                        .addOnSuccessListener { doc ->
                            val savedListAny = doc.get("faceEmbedding") as? List<*>
                            val savedList = savedListAny
                                ?.mapNotNull { (it as? Number)?.toDouble() }
                                ?: run {
                                    Toast.makeText(this@FaceVerifyActivity, "Chưa đăng ký khuôn mặt", Toast.LENGTH_LONG).show()
                                    onDone()
                                    return@addOnSuccessListener
                                }

                            val savedEmbedding = savedList.map { it.toFloat() }.toFloatArray()
                            val score = faceHelper.cosineSimilarity(liveEmbedding, savedEmbedding)

                            if (score >= 0.8f) {
                                if (mode == "update_face") {
                                    cameraProvider?.unbindAll()

                                    Toast.makeText(
                                        this@FaceVerifyActivity,
                                        "Xác minh thành công. Chụp khuôn mặt mới",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    startActivity(
                                        Intent(this@FaceVerifyActivity, FaceRegisterActivity::class.java)
                                            .putExtra("mode", "update_face")
                                    )
                                    finish()
                                } else {
                                    recordAttendance(sessionId, onDone)
                                }
                            } else {
                                Toast.makeText(
                                    this@FaceVerifyActivity,
                                    "Khuôn mặt không khớp",
                                    Toast.LENGTH_LONG
                                ).show()
                                onDone()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this@FaceVerifyActivity, "Lỗi Firestore: ${e.message}", Toast.LENGTH_LONG).show()
                            onDone()
                        }
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        this@FaceVerifyActivity,
                        "Chụp ảnh thất bại: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    onDone()
                }
            }
        )
    }

    private fun recordAttendance(sessionId: String, onDone: () -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run { onDone(); return }
        val db = FirebaseFirestore.getInstance()

        val docId = "${sessionId}_$uid"

        db.collection("attendance")
            .document(docId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    Toast.makeText(this, "Bạn đã điểm danh rồi", Toast.LENGTH_SHORT).show()
                    onDone()
                    finish()
                    return@addOnSuccessListener
                }

                db.collection("attendance")
                    .document(docId)
                    .set(
                        mapOf(
                            "sessionId" to sessionId,
                            "studentId" to uid,
                            "timestamp" to System.currentTimeMillis(),
                            "verifiedBy" to "qr+gps+face+blink"
                        )
                    )
                    .addOnSuccessListener {
                        Toast.makeText(this, "Điểm danh thành công", Toast.LENGTH_SHORT).show()
                        onDone()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Lỗi điểm danh: ${e.message}", Toast.LENGTH_LONG).show()
                        onDone()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi kiểm tra: ${e.message}", Toast.LENGTH_LONG).show()
                onDone()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            cameraProvider?.unbindAll()
        } catch (_: Exception) {}
        try {
            faceDetector.close()
        } catch (_: Exception) {}
        analysisExecutor.shutdown()
    }
}

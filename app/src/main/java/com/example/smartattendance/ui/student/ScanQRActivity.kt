package com.example.smartattendance.ui.student


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

// =======================
// FIREBASE
// =======================
import com.example.smartattendance.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

// =======================
// QR
// =======================
import com.journeyapps.barcodescanner.DecoratedBarcodeView

// =======================
// GPS
// =======================
import com.google.android.gms.location.*

class ScanQRActivity : AppCompatActivity() {


    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var progressBar: ProgressBar


    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null


    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                barcodeView.resume()
            } else {
                toast("Cần quyền Camera để quét QR")
                finish()
            }
        }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                toast("Cần quyền GPS để điểm danh")
                finish()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_qr)

        barcodeView = findViewById(R.id.barcodeView)
        progressBar = findViewById(R.id.progressBar)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkCameraPermission()

        val user = FirebaseAuth.getInstance().currentUser ?: run {
            toast("Chưa đăng nhập")
            finish()
            return
        }


        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.contains("faceEmbedding")) {
                    startActivity(Intent(this, FaceRegisterActivity::class.java))
                    finish()
                    return@addOnSuccessListener
                }
            }
            .addOnFailureListener {
                toast("Lỗi kiểm tra khuôn mặt")
                finish()
            }
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
        startScanQR()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }


    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }


    private fun startScanQR() {
        barcodeView.decodeContinuous { result ->
            barcodeView.pause()

            val sessionId = result.text?.trim()
            if (sessionId.isNullOrEmpty()) {
                barcodeView.resume()
                return@decodeContinuous
            }

            Log.d("QR_DEBUG", "QR = $sessionId")
            progressBar.visibility = View.VISIBLE

            checkSessionAndJoin(sessionId)
        }
    }


    private fun checkSessionAndJoin(sessionId: String) {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        val db = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser!!.uid


        db.collection("sessions")
            .document(sessionId)
            .get()
            .addOnSuccessListener { sessionDoc ->

                if (!sessionDoc.exists() || sessionDoc.getBoolean("isOpen") != true) {
                    toast("Buổi học không hợp lệ")
                    finish()
                    return@addOnSuccessListener
                }

                val classId = sessionDoc.getString("classId") ?: run {
                    toast("Session lỗi dữ liệu")
                    finish()
                    return@addOnSuccessListener
                }

                val createdAt = sessionDoc.getLong("createdAt") ?: 0L
                val durationMs = sessionDoc.getLong("durationMs") ?: 60_000L
                val expireAt = createdAt + durationMs

                val now = System.currentTimeMillis()

                if (now > expireAt) {
                    sessionDoc.reference.update("isOpen", false)
                    toast("Buổi học đã đóng")
                    finish()
                    return@addOnSuccessListener
                }

                val joinId = "${classId}_$uid"
                val joinRef = db.collection("class_students").document(joinId)

                joinRef.get().addOnSuccessListener {
                    if (!it.exists()) {
                        joinRef.set(
                            mapOf(
                                "classId" to classId,
                                "studentId" to uid,
                                "joinedAt" to System.currentTimeMillis()
                            )
                        ).addOnSuccessListener {
                            checkGPSAndContinue(sessionDoc, sessionId)
                        }
                    } else {
                        checkGPSAndContinue(sessionDoc, sessionId)
                    }
                }

            }
            .addOnFailureListener {
                toast("Lỗi mạng")
                finish()
            }
    }

    private fun checkGPSAndContinue(
        sessionDoc: DocumentSnapshot,
        sessionId: String
    ) {
        val classLat = sessionDoc.getDouble("latitude")!!
        val classLng = sessionDoc.getDouble("longitude")!!
        val radius = sessionDoc.getLong("radius") ?: 500000

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L
        ).setMaxUpdates(3).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {

                val location = result.lastLocation ?: return
                val results = FloatArray(1)

                Location.distanceBetween(
                    location.latitude,
                    location.longitude,
                    classLat,
                    classLng,
                    results
                )

                val distance = results[0]
                val accuracy = location.accuracy

                Log.d(
                    "GPS_FIX",
                    """
                    USER: ${location.latitude}, ${location.longitude}
                    ACCURACY: $accuracy
                    CLASS: $classLat, $classLng
                    DISTANCE: $distance
                    RADIUS: $radius
                    """.trimIndent()
                )

                if (accuracy > 35) return

                fusedLocationClient.removeLocationUpdates(this)

                if (isFakeLocation(location)) {
                    toast("Phát hiện Fake GPS")
                    finish()
                    return
                }


                if (distance <= radius + accuracy) {
                    startActivity(
                        Intent(this@ScanQRActivity, FaceVerifyActivity::class.java)
                            .putExtra("sessionId", sessionId)
                    )
                    finish()
                } else {
                    toast("Bạn không ở trong khu vực lớp học!")
                    finish()
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback!!,
            mainLooper
        )
    }

    fun isFakeLocation(location: Location): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}

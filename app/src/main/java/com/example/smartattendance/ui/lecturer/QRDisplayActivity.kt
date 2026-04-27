package com.example.smartattendance.ui.lecturer

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.smartattendance.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

class QRDisplayActivity : AppCompatActivity() {

    private lateinit var imgQR: ImageView
    private lateinit var btnViewAttendance: Button
    private lateinit var btnCloseSession: Button
    private lateinit var tvTimer: TextView

    private lateinit var tvClassNmae: TextView
    private var expireAt: Long = 0L
    private var isClosed = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_display)


        val sessionId = intent.getStringExtra("sessionId")
        if (sessionId == null) {
            Log.e("QR_DEBUG", "sessionId NULL")
            finish()
            return
        }


        imgQR = findViewById(R.id.imgQR)
        btnViewAttendance = findViewById(R.id.btnViewAttendance)
        btnCloseSession = findViewById(R.id.btnCloseSession)
        tvTimer = findViewById(R.id.tvTimer)
        tvClassNmae = findViewById(R.id.tvClassName)

        generateQr(sessionId)


        FirebaseFirestore.getInstance()
            .collection("sessions")
            .document(sessionId)
            .get()
            .addOnSuccessListener { doc ->
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                val durationMs = doc.getLong("durationMs") ?: 60_000L
                expireAt = createdAt + durationMs

                startCountdown(sessionId)
            }


        FirebaseFirestore.getInstance()
            .collection("sessions")
            .document(sessionId)
            .get()
            .addOnSuccessListener { sessionDoc ->

                val classId = sessionDoc.getString("classId")
                    ?: return@addOnSuccessListener

                FirebaseFirestore.getInstance()
                    .collection("classes")
                    .document(classId)
                    .get()
                    .addOnSuccessListener { classDoc ->

                        val className =
                            classDoc.getString("className") ?: "Tên lớp"

                        tvClassNmae.text = className
                    }
            }


        btnViewAttendance.setOnClickListener {
            startActivity(
                Intent(this, AttendanceListActivity::class.java)
                    .putExtra("sessionId", sessionId)
            )
        }


        btnCloseSession.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Kết thúc điểm danh?")
                .setMessage("Sinh viên sẽ không thể điểm danh nữa.")
                .setPositiveButton("Kết thúc") { _, _ ->
                    closeSession(sessionId)
                }
                .setNegativeButton("Huỷ", null)
                .show()
        }

        val prefs = getSharedPreferences("qr_session", MODE_PRIVATE)
        prefs.edit()
            .putString("active_session_id", sessionId)
            .apply()

    }


    private fun generateQr(sessionId: String) {
        val encoder = BarcodeEncoder()
        val bitmap = encoder.encodeBitmap(
            sessionId,
            BarcodeFormat.QR_CODE,
            600,
            600
        )
        imgQR.setImageBitmap(bitmap)
    }


    private fun startCountdown(sessionId: String) {
        handler.post(object : Runnable {
            override fun run() {
                val remaining = expireAt - System.currentTimeMillis()

                if (remaining <= 0 && !isClosed) {
                    closeSession(sessionId)
                    return
                }

                val sec = (remaining / 1000).toInt().coerceAtLeast(0)
                tvTimer.text = String.format(
                    "Mã làm mới sau: %02d:%02d",
                    sec / 60,
                    sec % 60
                )

                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun closeSession(sessionId: String) {

        if (isClosed) return
        isClosed = true

        FirebaseFirestore.getInstance()
            .collection("sessions")
            .document(sessionId)
            .update(
                mapOf(
                    "isOpen" to false,
                    "closedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {


                getSharedPreferences("qr_session", MODE_PRIVATE)
                    .edit()
                    .remove("active_session_id")
                    .apply()

                Toast.makeText(this, "Buổi học đã kết thúc", Toast.LENGTH_SHORT).show()
                finish()
            }
    }


    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}

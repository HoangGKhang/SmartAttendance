package com.example.smartattendance.ui.student

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartattendance.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StudentDashboardActivity : AppCompatActivity() {

    private lateinit var tvEmail: TextView
    private lateinit var tvFaceStatus: TextView
    private lateinit var btnScanQR: Button
    private lateinit var btnFace: Button
    private lateinit var btnLogout: Button

    private var hasFace = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_dashboard)

        tvEmail = findViewById(R.id.tvEmail)
        tvFaceStatus = findViewById(R.id.tvFaceStatus)
        btnScanQR = findViewById(R.id.btnScanQR)
        btnFace = findViewById(R.id.btnFace)
        btnLogout = findViewById(R.id.btnLogout)

        val user = FirebaseAuth.getInstance().currentUser ?: run {
            finish()
            return
        }

        tvEmail.text = user.email

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                hasFace = doc.contains("faceEmbedding")
                if (doc.contains("faceEmbedding")) {
                    tvFaceStatus.text = "Khuôn mặt: Đã đăng ký"
                    btnFace.text = "Cập nhật khuôn mặt"
                } else {
                    tvFaceStatus.text = "Khuôn mặt: Chưa đăng ký"
                    btnFace.text = "Đăng ký khuôn mặt"
                }
            }

        btnScanQR.setOnClickListener {
            startActivity(
                Intent(this, ScanQRActivity::class.java)
            )
        }

        btnFace.setOnClickListener {
            if (!hasFace) {
                startActivity(Intent(this, FaceRegisterActivity::class.java))
            } else {
                startActivity(
                    Intent(this, FaceVerifyActivity::class.java)
                        .putExtra("mode", "update_face")
                )
            }
        }


        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}

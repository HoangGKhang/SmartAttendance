package com.example.smartattendance.ui.lecturer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.smartattendance.R
import com.example.smartattendance.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LecturerHomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lecturer_home)

        val tvEmail: TextView = findViewById(R.id.tvEmail)
        val btnCreateClass: Button = findViewById(R.id.btnCreateClass)
        val btnLogout: Button = findViewById(R.id.btnLogout)
        val btnViewAttendance: Button = findViewById(R.id.btnViewAttendance)

        val prefs = getSharedPreferences("qr_session", MODE_PRIVATE)
        val sessionId = prefs.getString("active_session_id", null)

        if (sessionId != null) {
            FirebaseFirestore.getInstance()
                .collection("sessions")
                .document(sessionId)
                .get()
                .addOnSuccessListener { doc ->

                    val isOpen = doc.getBoolean("isOpen") ?: false
                    val createdAt = doc.getLong("createdAt") ?: 0L
                    val durationMs = doc.getLong("durationMs") ?: 0L
                    val expireAt = createdAt + durationMs
                    val now = System.currentTimeMillis()

                    if (isOpen && now < expireAt) {
                        startActivity(
                            Intent(this, QRDisplayActivity::class.java)
                                .putExtra("sessionId", sessionId)
                        )
                        finish()
                    } else {
                        FirebaseFirestore.getInstance()
                            .collection("sessions")
                            .document(sessionId)
                            .update(
                                mapOf(
                                    "isOpen" to false,
                                    "closedAt" to now
                                )
                            )

                        prefs.edit().remove("active_session_id").apply()
                    }
                }
        }



        val user = FirebaseAuth.getInstance().currentUser ?: return
        tvEmail.text = user.email

        btnCreateClass.setOnClickListener {
            startActivity(Intent(this, CreateClassActivity::class.java))
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }



        btnViewAttendance.setOnClickListener {
            startActivity(
                Intent(this, LecturerSessionListActivity::class.java)
            )
        }

    }
}

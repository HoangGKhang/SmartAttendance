package com.example.smartattendance.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smartattendance.ui.lecturer.LecturerHomeActivity
import com.example.smartattendance.ui.student.StudentDashboardActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        checkLoginState()
    }

    private fun checkLoginState() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            goToLogin()
            return
        }

        val uid = currentUser.uid

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                val role = document.getString("role")

                when (role) {
                    "student" -> goToStudentHome()
                    "lecturer" -> goToLecturerHome()
                    else -> goToLogin()
                }
            }
            .addOnFailureListener {
                goToLogin()
            }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun goToStudentHome() {
        startActivity(Intent(this, StudentDashboardActivity::class.java))
        finish()
    }

    private fun goToLecturerHome() {
        startActivity(Intent(this, LecturerHomeActivity::class.java))
        finish()
    }
}

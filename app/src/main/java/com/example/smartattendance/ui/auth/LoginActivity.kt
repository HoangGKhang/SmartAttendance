package com.example.smartattendance.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.smartattendance.R
import com.example.smartattendance.ui.lecturer.LecturerHomeActivity
import com.example.smartattendance.ui.student.StudentDashboardActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

//        auth = FirebaseAuth.getInstance()
//        db = FirebaseFirestore.getInstance()
//
//        val edtEmail = findViewById<EditText>(R.id.edtEmail)
//        val edtPassword = findViewById<EditText>(R.id.edtPassword)
//        val btnLogin = findViewById<Button>(R.id.btnLogin)
//        val txtRegister = findViewById<TextView>(R.id.txtRegister)
//        val txtForgotPassword = findViewById<TextView>(R.id.txtForgotPassword)
//
//
//        txtRegister.setOnClickListener {
//            startActivity(Intent(this, RegisterActivity::class.java))
//        }
//
//        btnLogin.setOnClickListener {
//            val email = edtEmail.text.toString()
//            val password = edtPassword.text.toString()
//
//            if (email.isEmpty() || password.isEmpty()) {
//                toast("Vui lòng nhập đầy đủ thông tin")
//                return@setOnClickListener
//            }
//
//            auth.signInWithEmailAndPassword(email, password)
//                .addOnSuccessListener {
//                    val uid = it.user!!.uid
//                    loadUserRole(uid)
//                }
//                .addOnFailureListener {
//                    toast("Sai tài khoản hoặc mật khẩu")
//                }
//        }
//        txtForgotPassword.setOnClickListener {
//
//            val email = edtEmail.text.toString().trim()
//
//            if (email.isEmpty()) {
//                toast("Nhập email trước khi quên mật khẩu")
//                return@setOnClickListener
//            }
//
//            auth.sendPasswordResetEmail(email)
//                .addOnSuccessListener {
//                    toast("Đã gửi email đặt lại mật khẩu")
//                }
//                .addOnFailureListener {
//                    toast("Email không tồn tại hoặc lỗi mạng")
//                }
//        }

    }


    private fun loadUserRole(uid: String) {
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->

                val role = doc.getString("role")

                when (role) {
                    "student" -> {
                        startActivity(
                            Intent(this, StudentDashboardActivity::class.java)
                        )
                    }

                    "lecturer" -> {
                        startActivity(
                            Intent(this, LecturerHomeActivity::class.java)
                        )
                    }

                    else -> {
                        toast("Tài khoản chưa được phân quyền")
                        return@addOnSuccessListener
                    }
                }

                finish()
            }
            .addOnFailureListener {
                toast("Không lấy được thông tin người dùng")
            }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}

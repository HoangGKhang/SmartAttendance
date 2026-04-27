package com.example.smartattendance.ui.auth

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.smartattendance.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var roleGroup: RadioGroup
    private lateinit var btnRegister: Button

    private lateinit var edtConfirmPassword: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        roleGroup = findViewById(R.id.roleGroup)
        btnRegister = findViewById(R.id.btnRegister)
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword)
        btnRegister.setOnClickListener {
            handleRegister()
        }
    }

    private fun handleRegister() {
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString().trim()
        val roleId = roleGroup.checkedRadioButtonId

        if (email.isEmpty() || password.isEmpty() || roleId == -1) {
            toast("Vui lòng nhập đầy đủ thông tin")
            return
        }

        if (!email.endsWith("@gm.uit.edu.vn")&&!email.endsWith("@gm.iuh.edu.vn")) {
            toast("Chỉ cho phép email trường")
            return
        }
        val confirmPassword = edtConfirmPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || roleId == -1) {
            toast("Vui lòng nhập đầy đủ thông tin")
            return
        }

        if (password != confirmPassword) {
            toast("Mật khẩu không khớp")
            return
        }

        if (password.length < 6) {
            toast("Mật khẩu tối thiểu 6 ký tự")
            return
        }

        val role = if (roleId == R.id.rbStudent) "student" else "lecturer"

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val uid = it.user!!.uid

                val userData = hashMapOf(
                    "email" to email,
                    "role" to role,
                    "createdAt" to System.currentTimeMillis()
                )

                db.collection("users").document(uid)
                    .set(userData)
                    .addOnSuccessListener {
                        toast("Đăng ký thành công")
                        finish()
                    }
            }
            .addOnFailureListener {
                toast(it.message ?: "Đăng ký thất bại")
            }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}

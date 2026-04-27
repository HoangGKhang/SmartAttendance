package com.example.smartattendance.ui.lecturer


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat


import com.example.smartattendance.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import com.google.android.gms.location.*

class CreateClassActivity : AppCompatActivity() {

    private lateinit var edtClassName: EditText
    private lateinit var edtSemester: EditText
    private lateinit var btnCreate: Button

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val CLASS_RADIUS =    500000
    private val SESSION_DURATION_MS = 60_000L

    private val db = FirebaseFirestore.getInstance()

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                getLocationAndCreateClass()
            } else {
                toast("Cần quyền GPS để mở lớp")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_class)

        edtClassName = findViewById(R.id.edtClassName)
        edtSemester = findViewById(R.id.edtSemester)
        btnCreate = findViewById(R.id.btnCreate)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        btnCreate.setOnClickListener {
            checkLocationPermission()
        }
    }


    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            getLocationAndCreateClass()
        }
    }


    private fun getLocationAndCreateClass() {

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location == null || location.accuracy > 30) {
                    toast("Không lấy được GPS chính xác, thử lại")
                    return@addOnSuccessListener
                }

                Log.d(
                    "TEACHER_GPS",
                    "LAT=${location.latitude}, LNG=${location.longitude}, ACC=${location.accuracy}"
                )

                createClassAndSession(location)
            }
            .addOnFailureListener {
                toast("Lỗi lấy GPS")
            }
    }


    private fun createClassAndSession(location: Location) {

        val className = edtClassName.text.toString().trim()
        val semester = edtSemester.text.toString().trim()
        val teacherId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        if (className.isEmpty() || semester.isEmpty()) {
            toast("Vui lòng nhập đầy đủ thông tin")
            return
        }

        val classData = hashMapOf(
            "className" to className,
            "semester" to semester,
            "teacherId" to teacherId,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("classes")
            .add(classData)
            .addOnSuccessListener { classDoc ->

                val sessionData = hashMapOf(
                    "classId" to classDoc.id,
                    "teacherId" to teacherId,
                    "isOpen" to true,
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "radius" to CLASS_RADIUS,
                    "createdAt" to System.currentTimeMillis(),
                    "durationMs" to SESSION_DURATION_MS,
                )

                db.collection("sessions")
                    .add(sessionData)
                    .addOnSuccessListener { sessionDoc ->
                        startActivity(
                            Intent(this, QRDisplayActivity::class.java)
                                .putExtra("sessionId", sessionDoc.id)
                        )
                        finish()
                    }
            }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}

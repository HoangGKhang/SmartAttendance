package com.example.smartattendance.ui.lecturer

import android.os.Bundle
import android.widget.ListView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartattendance.R
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

class AttendanceListActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var adapter: AttendanceListAdapter
    private val list = mutableListOf<AttendanceItem>()

    private var className = ""
    private var semester = ""
    private var sessionDate = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance_list)

        val btnExport = findViewById<Button>(R.id.btnExportCsv)

        btnExport.setOnClickListener {
            exportAttendanceToCsv()
        }

        listView = findViewById(R.id.listViewAttendance)
        adapter = AttendanceListAdapter(this, list)
        listView.adapter = adapter

        val sessionId = intent.getStringExtra("sessionId") ?: return

        FirebaseFirestore.getInstance()
            .collection("sessions")
            .document(sessionId)
            .get()
            .addOnSuccessListener { sessionDoc ->

                val classId = sessionDoc.getString("classId") ?: return@addOnSuccessListener
                val createdAt = sessionDoc.getLong("createdAt") ?: 0L

                sessionDate = java.text.SimpleDateFormat(
                    "dd/MM/yyyy",
                    java.util.Locale.getDefault()
                ).format(java.util.Date(createdAt))

                FirebaseFirestore.getInstance()
                    .collection("classes")
                    .document(classId)
                    .get()
                    .addOnSuccessListener { classDoc ->
                        className = classDoc.getString("className") ?: ""
                        semester = classDoc.getString("semester") ?: ""
                    }
            }


        FirebaseFirestore.getInstance()
            .collection("attendance")
            .whereEqualTo("sessionId", sessionId)
            .addSnapshotListener { snapshot, _ ->
                list.clear()

                snapshot?.documents?.forEach {
                    val item = AttendanceItem(
                        sessionId = it.getString("sessionId") ?: "",
                        studentId = it.getString("studentId") ?: "",
                        timestamp = it.getLong("timestamp") ?: 0L,
                        verifiedBy = it.getString("verifiedBy") ?: ""
                    )

                    list.add(item)

                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(item.studentId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            item.studentEmail =
                                userDoc.getString("email") ?: item.studentId
                            adapter.notifyDataSetChanged()
                        }

                }

                adapter.notifyDataSetChanged()
            }


    }
    private fun exportAttendanceToCsv() {

        if (list.isEmpty()) {
            Toast.makeText(this, "Chưa có dữ liệu điểm danh", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = "attendance_${System.currentTimeMillis()}.csv"
        val file = File(getExternalFilesDir(null), fileName)

        val timeFormatter = java.text.SimpleDateFormat(
            "HH:mm:ss",
            java.util.Locale.getDefault()
        )

        file.bufferedWriter().use { writer ->

            writer.write("Tên lớp,Học kỳ,Ngày học,Email sinh viên,Giờ điểm danh,Hình thức\n")

            list.forEach { item ->

                val time = timeFormatter.format(java.util.Date(item.timestamp))

                val verifiedText = when (item.verifiedBy) {
                    "qr+gps+face" -> "QR + GPS + Face"
                    "qr" -> "QR"
                    "face" -> "Face"
                    else -> item.verifiedBy
                }

                writer.write(
                    "$className," +
                            "$semester," +
                            "$sessionDate," +
                            "${item.studentEmail}," +
                            "$time," +
                            "$verifiedText\n"
                )
            }
        }

        Toast.makeText(
            this,
            "Đã xuất danh sách điểm danh\n${file.absolutePath}",
            Toast.LENGTH_LONG
        ).show()
    }



}

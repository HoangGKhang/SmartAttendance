package com.example.smartattendance.ui.lecturer

import android.content.Intent
import android.os.Bundle
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.example.smartattendance.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LecturerSessionListActivity : AppCompatActivity() {

    private val list = mutableListOf<SessionItem>()
    private lateinit var adapter: SessionListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_list)

        val listView = findViewById<ListView>(R.id.listViewSessions)
        adapter = SessionListAdapter(this, list)
        listView.adapter = adapter

        val teacherId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val now = System.currentTimeMillis()

        db.collection("sessions")
            .whereEqualTo("teacherId", teacherId)
            .get()
            .addOnSuccessListener { snapshot ->

                list.clear()

                snapshot.documents.forEach { sessionDoc ->

                    val createdAt = sessionDoc.getLong("createdAt") ?: 0L
                    val durationMs = sessionDoc.getLong("durationMs") ?: 0L
                    val expireAt = createdAt + durationMs

                    var isOpen = sessionDoc.getBoolean("isOpen") ?: false


                    if (isOpen && now > expireAt) {
                        sessionDoc.reference.update(
                            mapOf(
                                "isOpen" to false,
                                "closedAt" to now
                            )
                        )
                        isOpen = false
                    }

                    val item = SessionItem(
                        sessionId = sessionDoc.id,
                        classId = sessionDoc.getString("classId") ?: "",
                        isOpen = isOpen
                    )

                    list.add(item)

                    db.collection("classes")
                        .document(item.classId)
                        .get()
                        .addOnSuccessListener { classDoc ->

                            val className =
                                classDoc.getString("className") ?: "Không tên"
                            val semester =
                                classDoc.getString("semester") ?: ""

                            item.className =
                                if (semester.isNotEmpty())
                                    "$className - $semester"
                                else className

                            adapter.notifyDataSetChanged()
                        }
                }
            }

        listView.setOnItemClickListener { _, _, position, _ ->
            startActivity(
                Intent(this, AttendanceListActivity::class.java)
                    .putExtra("sessionId", list[position].sessionId)
            )
        }
    }
}

package com.example.smartattendance.ui.lecturer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.smartattendance.R
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AttendanceListAdapter(
    context: Context,
    private val list: MutableList<AttendanceItem>
) : ArrayAdapter<AttendanceItem>(context, 0, list) {

    private val emailCache = mutableMapOf<String, String>()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_attendance, parent, false)

        val item = list[position]

        val tvStudent = view.findViewById<TextView>(R.id.tvStudent)
        val tvTime = view.findViewById<TextView>(R.id.tvTime)

        tvTime.text = SimpleDateFormat(
            "HH:mm:ss  dd/MM/yyyy",
            Locale.getDefault()
        ).format(Date(item.timestamp))

        if (emailCache.containsKey(item.studentId)) {
            tvStudent.text = emailCache[item.studentId]
        } else {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(item.studentId)
                .get()
                .addOnSuccessListener { userDoc ->
                    val email =
                        userDoc.getString("email") ?: item.studentId

                    emailCache[item.studentId] = email
                    tvStudent.text = email
                }
        }

        return view
    }

}

package com.example.smartattendance.ui.lecturer


data class AttendanceItem(
    val sessionId: String,
    val studentId: String,
    var studentEmail: String = "",
    val timestamp: Long,
    val verifiedBy: String
)

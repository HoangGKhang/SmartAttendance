package com.example.smartattendance.ui.lecturer

data class SessionItem(
    val sessionId: String,
    val classId: String,
    var className: String = "",
    val isOpen: Boolean
)

package com.example.smartattendance.ui.attendance

enum class StudentGpsStatus {
    IDLE,
    VERIFYING,
    PASS,
    FAIL,
    ERROR
}

data class StudentGpsUiState(
    val status: StudentGpsStatus = StudentGpsStatus.IDLE,
    val sessionId: String? = null,
    val errorMessage: String? = null
)

package com.example.smartattendance.ui.attendance

import com.example.smartattendance.data.remote.dto.VerifyAttendanceQrResponseDto
import com.example.smartattendance.data.repository.VerifyQrFailureReason

enum class StudentQrScanStatus {
    SCANNING,
    VERIFYING,
    SUCCESS,
    ERROR
}

data class StudentQrScanUiState(
    val status: StudentQrScanStatus =
        StudentQrScanStatus.SCANNING,

    val result: VerifyAttendanceQrResponseDto? = null,

    val failureReason: VerifyQrFailureReason? = null
)

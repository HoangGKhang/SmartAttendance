
package com.example.smartattendance.ui.attendance

import java.time.Instant

data class AttendanceQrUiState(
    val sessionId: String? = null,
    val qrToken: String? = null,
    val qrExpiresAt: Instant? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

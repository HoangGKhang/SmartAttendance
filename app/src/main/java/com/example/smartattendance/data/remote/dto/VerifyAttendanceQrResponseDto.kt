
package com.example.smartattendance.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class VerifyAttendanceQrResponseDto(
    val verified: Boolean,
    val sessionId: String,
    val meetingId: String,
    val verifiedAt: String
)

@Serializable
data class VerifyAttendanceQrErrorDto(
    val error: String
)

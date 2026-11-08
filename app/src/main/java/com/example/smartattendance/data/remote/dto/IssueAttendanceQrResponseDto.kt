
package com.example.smartattendance.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class IssueAttendanceQrResponseDto(
    val qrToken: String,
    val qrExpiresAt: String
)

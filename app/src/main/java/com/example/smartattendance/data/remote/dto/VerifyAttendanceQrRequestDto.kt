
package com.example.smartattendance.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class VerifyAttendanceQrRequestDto(
    val qrToken: String
)

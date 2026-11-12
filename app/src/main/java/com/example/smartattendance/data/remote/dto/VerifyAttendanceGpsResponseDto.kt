package com.example.smartattendance.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class VerifyAttendanceGpsResponseDto(
    val verified: Boolean
)
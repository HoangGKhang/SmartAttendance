package com.example.smartattendance.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class VerifyAttendanceGpsRequestDto(
    val meetingId: String,
    val sessionId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double
)
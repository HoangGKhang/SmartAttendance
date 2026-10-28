
package com.example.smartattendance.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateAttendanceSessionRequestDto(
    val meetingId: String,
    val durationMinutes: Int = 15
)

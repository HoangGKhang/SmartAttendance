
package com.example.smartattendance.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AttendanceSessionDto(
    val id: String,

    @SerialName("meeting_id")
    val meetingId: String,

    @SerialName("created_by")
    val createdBy: String,

    val status: String,

    @SerialName("opened_at")
    val openedAt: String? = null,

    @SerialName("expires_at")
    val expiresAt: String? = null,

    @SerialName("closed_at")
    val closedAt: String? = null
)

@Serializable
data class CreateAttendanceSessionResponseDto(
    val session: AttendanceSessionDto
)

package com.example.smartattendance.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AttendanceSession(
    @SerialName("session_id")
    val sessionId: String? = null,

    @SerialName("course_id")
    val courseId: String,

    @SerialName("teacher_id")
    val teacherId: String? = null,

    val title: String? = null,

    @SerialName("session_date")
    val sessionDate: String? = null,

    @SerialName("start_time")
    val startTime: String? = null,

    @SerialName("end_time")
    val endTime: String? = null,

    val status: String = "open",

    val latitude: Double? = null,

    val longitude: Double? = null,

    @SerialName("radius_meters")
    val radiusMeters: Int = 100,

    @SerialName("duration_ms")
    val durationMs: Int = 600000,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("closed_at")
    val closedAt: String? = null
)
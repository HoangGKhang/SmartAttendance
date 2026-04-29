package com.example.smartattendance.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AttendanceRecord(
    @SerialName("record_id")
    val recordId: String? = null,

    @SerialName("session_id")
    val sessionId: String,

    @SerialName("student_id")
    val studentId: String,

    val status: String = "absent",

    @SerialName("checkin_time")
    val checkinTime: String? = null,

    @SerialName("verified_by")
    val verifiedBy: String? = null,

    @SerialName("gps_distance_meters")
    val gpsDistanceMeters: Double? = null,

    @SerialName("face_similarity")
    val faceSimilarity: Double? = null,

    @SerialName("is_liveness_passed")
    val isLivenessPassed: Boolean = false,

    val note: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
)

package com.example.smartattendance.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateAttendanceSessionRequestDto(

    val meetingId: String,

    val durationMinutes: Int,

    // Tọa độ trung tâm điểm danh.
    val latitude: Double,
    val longitude: Double,

    // Bán kính cho phép, đơn vị mét.
    val radiusMeters: Int,

    // Độ chính xác vị trí do Android báo cáo.
    val accuracyMeters: Double
)

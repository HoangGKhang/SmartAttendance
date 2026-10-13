package com.example.smartattendance.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateMeetingRequest(
    val courseId: String,
    val meetingNumber: Int,
    val meetingDate: String,
    val title: String?,
    val startTime: String,
    val endTime: String,
    val room: String?
)
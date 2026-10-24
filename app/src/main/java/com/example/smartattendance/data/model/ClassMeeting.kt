package com.example.smartattendance.data.model

import java.time.LocalDate
import java.time.LocalTime

data class ClassMeeting(
    val id: String,
    val courseId: String,
    val meetingNumber: Int,
    val meetingDate: LocalDate,
    val title: String?,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val room: String?,
    val status: MeetingStatus
)

enum class MeetingStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED
}
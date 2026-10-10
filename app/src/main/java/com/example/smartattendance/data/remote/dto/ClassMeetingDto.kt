package com.example.smartattendance.data.remote.dto

import com.example.smartattendance.data.model.ClassMeeting
import com.example.smartattendance.data.model.MeetingStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime

@Serializable
data class ClassMeetingDto(
    val id: String,

    @SerialName("course_id")
    val courseId: String,

    @SerialName("meeting_number")
    val meetingNumber: Int,

    @SerialName("meeting_date")
    val meetingDate: String,

    val title: String? = null,

    @SerialName("start_time")
    val startTime: String? = null,

    @SerialName("end_time")
    val endTime: String? = null,

    val room: String? = null,

    val status: String
)

fun ClassMeetingDto.toClassMeeting(): ClassMeeting {
    return ClassMeeting(
        id = id,
        courseId = courseId,
        meetingNumber = meetingNumber,
        meetingDate = LocalDate.parse(meetingDate),
        title = title,
        startTime = startTime?.let(LocalTime::parse),
        endTime = endTime?.let(LocalTime::parse),
        room = room,
        status = MeetingStatus.valueOf(status)
    )
}
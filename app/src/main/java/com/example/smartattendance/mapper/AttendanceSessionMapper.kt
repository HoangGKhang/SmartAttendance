
package com.example.smartattendance.data.mapper

import com.example.smartattendance.data.model.AttendanceSession
import com.example.smartattendance.data.model.AttendanceSessionStatus
import com.example.smartattendance.data.remote.dto.AttendanceSessionDto

import java.time.Instant
import java.time.OffsetDateTime

private fun parseSessionTime(value: String): Instant {
    return OffsetDateTime.parse(value).toInstant()
}

fun AttendanceSessionDto.toAttendanceSession(): AttendanceSession {
    return AttendanceSession(
        id = id,
        meetingId = meetingId,
        createdBy = createdBy,
        status = AttendanceSessionStatus.valueOf(status),

        openedAt = openedAt?.let(::parseSessionTime),
        expiresAt = expiresAt?.let(::parseSessionTime),
        closedAt = closedAt?.let(::parseSessionTime)
    )
}

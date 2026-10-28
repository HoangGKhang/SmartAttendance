
package com.example.smartattendance.data.mapper

import com.example.smartattendance.data.model.AttendanceSession
import com.example.smartattendance.data.model.AttendanceSessionStatus
import com.example.smartattendance.data.remote.dto.AttendanceSessionDto
import java.time.Instant

fun AttendanceSessionDto.toAttendanceSession(): AttendanceSession {
    return AttendanceSession(
        id = id,
        meetingId = meetingId,
        createdBy = createdBy,
        status = AttendanceSessionStatus.valueOf(status),
        openedAt = openedAt?.let(Instant::parse),
        expiresAt = expiresAt?.let(Instant::parse),
        closedAt = closedAt?.let(Instant::parse)
    )
}

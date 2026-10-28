
package com.example.smartattendance.data.model

import java.time.Instant

data class AttendanceSession(
    val id: String,
    val meetingId: String,
    val createdBy: String,
    val status: AttendanceSessionStatus,
    val openedAt: Instant?,
    val expiresAt: Instant?,
    val closedAt: Instant?
) {
    fun isActive(now: Instant = Instant.now()): Boolean {
        return status == AttendanceSessionStatus.OPEN &&
                openedAt != null &&
                !openedAt.isAfter(now) &&
                expiresAt != null &&
                now.isBefore(expiresAt)
    }
}

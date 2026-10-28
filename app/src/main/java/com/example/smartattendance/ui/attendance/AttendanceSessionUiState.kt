
package com.example.smartattendance.ui.attendance

import com.example.smartattendance.data.model.AttendanceSession

data class AttendanceSessionUiState(
    val isLoading: Boolean = false,
    val session: AttendanceSession? = null,
    val errorMessage: String? = null
)

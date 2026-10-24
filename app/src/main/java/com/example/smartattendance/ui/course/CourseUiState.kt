package com.example.smartattendance.ui.course

import com.example.smartattendance.data.model.Course

sealed interface CourseUiState {

    data object Idle : CourseUiState

    data object Loading : CourseUiState

    data class Success(
        val courses: List<Course>
    ) : CourseUiState

    data class Error(
        val message: String
    ) : CourseUiState
}
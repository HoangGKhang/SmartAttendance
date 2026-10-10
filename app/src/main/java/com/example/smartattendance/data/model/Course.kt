package com.example.smartattendance.data.model

import java.time.LocalDate

data class Course(
    val id: String,
    val moodleCourseId: Long?,
    val courseCode: String,
    val classCode: String,
    val name: String,
    val semester: String,
    val academicYear: String,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val isActive: Boolean
)
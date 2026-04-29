package com.example.smartattendance.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CourseStudent(
    val id: String? = null,

    @SerialName("course_id")
    val courseId: String,

    @SerialName("student_id")
    val studentId: String,

    @SerialName("joined_at")
    val joinedAt: String? = null
)
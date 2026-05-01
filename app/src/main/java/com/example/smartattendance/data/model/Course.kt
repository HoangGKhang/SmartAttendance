package com.example.smartattendance.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Course(
    @SerialName("course_id")
    val courseId: String? = null,

    @SerialName("course_name")
    val courseName: String,

    @SerialName("course_code")
    val courseCode: String? = null,

    val semester: String? = null,

    @SerialName("teacher_id")
    val teacherId: String? = null,

    @SerialName("moodle_course_id")
    val moodleCourseId: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null
)
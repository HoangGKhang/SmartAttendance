package com.example.smartattendance.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Student(
    @SerialName("student_id")
    val studentId: String? = null,

    @SerialName("student_code")
    val studentCode: String? = null,

    @SerialName("full_name")
    val fullName: String,

    val email: String? = null,

    @SerialName("moodle_user_id")
    val moodleUserId: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null
)
package com.example.smartattendance.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("user_id")
    val userId: String,

    val email: String,

    @SerialName("full_name")
    val fullName: String,

    val role: String,

    @SerialName("moodle_user_id")
    val moodleUserId: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null
)
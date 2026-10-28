package com.example.smartattendance.data.model

data class Profile(
    val id: String,
    val studentCode: String?,
    val fullName: String,
    val role: UserRole,
    val moodleUserId: Long?
)
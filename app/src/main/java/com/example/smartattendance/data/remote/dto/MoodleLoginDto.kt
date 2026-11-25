
package com.example.smartattendance.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MoodleLoginRequestDto(
    val username: String,
    val password: String
)

@Serializable
data class MoodleLoginResponseDto(
    val success: Boolean,
    val user: MoodleLoginUserDto? = null,
    val session: MoodleLoginSessionDto? = null,
    val error: String? = null
)

@Serializable
data class MoodleLoginUserDto(
    @SerialName("moodle_user_id")
    val moodleUserId: Int,

    @SerialName("supabase_user_id")
    val supabaseUserId: String,

    val role: String
)

@Serializable
data class MoodleLoginSessionDto(
    @SerialName("access_token")
    val accessToken: String,

    @SerialName("refresh_token")
    val refreshToken: String,

    @SerialName("expires_in")
    val expiresIn: Int,

    @SerialName("expires_at")
    val expiresAt: Long? = null
)

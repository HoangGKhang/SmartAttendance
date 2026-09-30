// DTO = Data Transfer object

package com.example.smartattendance.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.example.smartattendance.data.model.Profile
import com.example.smartattendance.data.model.UserRole

@Serializable
data class ProfileDto(

    val id: String,

    @SerialName("student_code")
    val studentCode: String?,

    @SerialName("full_name")
    val fullName: String,

    val role: String,

    @SerialName("moodle_user_id")
    val moodleUserId: Long?
)

fun ProfileDto.toProfile(): Profile {

    return Profile(
        id = id,
        studentCode = studentCode,
        fullName = fullName,
        role = UserRole.valueOf(role),
        moodleUserId = moodleUserId
    )
}
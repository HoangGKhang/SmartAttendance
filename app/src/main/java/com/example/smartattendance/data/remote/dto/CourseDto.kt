package com.example.smartattendance.data.remote.dto

import com.example.smartattendance.data.model.Course
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class CourseDto(

    val id: String,

    @SerialName("moodle_course_id")
    val moodleCourseId: Long?,

    @SerialName("course_code")
    val courseCode: String,

    @SerialName("class_code")
    val classCode: String,

    val name: String,

    val semester: String,

    @SerialName("academic_year")
    val academicYear: String,

    @SerialName("start_date")
    val startDate: String?,

    @SerialName("end_date")
    val endDate: String?,

    @SerialName("is_active")
    val isActive: Boolean
)

fun CourseDto.toCourse(): Course {
    return Course(
        id = id,
        moodleCourseId = moodleCourseId,
        courseCode = courseCode,
        classCode = classCode,
        name = name,
        semester = semester,
        academicYear = academicYear,
        startDate = startDate?.let(LocalDate::parse),
        endDate = endDate?.let(LocalDate::parse),
        isActive = isActive
    )
}
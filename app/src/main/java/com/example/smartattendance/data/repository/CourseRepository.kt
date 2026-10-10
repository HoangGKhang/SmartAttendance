package com.example.smartattendance.data.repository

import com.example.smartattendance.data.model.Course
import com.example.smartattendance.data.remote.SupabaseProvider
import com.example.smartattendance.data.remote.dto.CourseDto
import com.example.smartattendance.data.remote.dto.toCourse
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class CourseRepository {

    private val supabase = SupabaseProvider.client

    suspend fun getCourses(): Result<List<Course>> {

        return try {

            val courseDtos = supabase
                .from("courses")
                .select(
                    columns = Columns.list(
                        "id",
                        "moodle_course_id",
                        "course_code",
                        "class_code",
                        "name",
                        "semester",
                        "academic_year",
                        "start_date",
                        "end_date",
                        "is_active"
                    )
                ) {
                    filter {
                        eq("is_active", true)
                    }
                }
                .decodeList<CourseDto>()

            val courses = courseDtos.map { courseDto ->
                courseDto.toCourse()
            }

            Result.success(courses)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }
}
package com.example.smartattendance.data.repository

import com.example.smartattendance.data.model.ClassMeeting
import com.example.smartattendance.data.remote.SupabaseProvider
import com.example.smartattendance.data.remote.dto.ClassMeetingDto
import com.example.smartattendance.data.remote.dto.toClassMeeting
import io.github.jan.supabase.postgrest.from

class ClassMeetingRepository {

    private val supabase = SupabaseProvider.client

    suspend fun getMeetings(
        courseId: String
    ): Result<List<ClassMeeting>> {

        return runCatching {

            val response = supabase
                .from("class_meetings")
                .select {
                    filter {
                        eq("course_id", courseId)
                    }
                }

            val meetingDtos =
                response.decodeList<ClassMeetingDto>()

            meetingDtos.map { dto ->
                dto.toClassMeeting()
            }
        }
    }
}
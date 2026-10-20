
package com.example.smartattendance.data.repository

import com.example.smartattendance.data.model.ClassMeeting
import com.example.smartattendance.data.remote.SupabaseProvider
import com.example.smartattendance.data.remote.dto.ClassMeetingDto
import com.example.smartattendance.data.remote.dto.toClassMeeting
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException

class ClassMeetingRepository {

    private val supabase = SupabaseProvider.client

    suspend fun getMeetings(
        courseId: String
    ): Result<List<ClassMeeting>> {

        return try {
            val response = supabase
                .from("class_meetings")
                .select {
                    filter {
                        eq("course_id", courseId)
                    }

                    order(
                        "meeting_date",
                        Order.DESCENDING
                    )

                    order(
                        "meeting_number",
                        Order.DESCENDING
                    )
                }

            val meetingDtos =
                response.decodeList<ClassMeetingDto>()

            Result.success(
                meetingDtos.map { dto ->
                    dto.toClassMeeting()
                }
            )

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

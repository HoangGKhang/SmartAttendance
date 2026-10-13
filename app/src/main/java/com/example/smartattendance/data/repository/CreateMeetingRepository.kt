package com.example.smartattendance.data.repository

import com.example.smartattendance.data.remote.SupabaseProvider
import com.example.smartattendance.data.remote.dto.CreateMeetingRequest
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException

class CreateMeetingRepository {

    private val supabase = SupabaseProvider.client

    suspend fun createMeeting(
        request: CreateMeetingRequest
    ): Result<Unit> {

        return try {

            // 1. Kiểm tra có session đăng nhập.
            val session = supabase.auth.currentSessionOrNull()

            if (session == null) {
                return Result.failure(
                    IllegalStateException(
                        "Vui lòng đăng nhập lại"
                    )
                )
            }

            // 2. Gửi dữ liệu tới Edge Function.
            val response = supabase.functions.invoke(
                function = "create-class-meeting",
                body = request,
                headers = headersOf(
                    HttpHeaders.ContentType,
                    "application/json"
                )
            )

            // 3. Kiểm tra HTTP response.
            if (response.status.value == 201) {

                Result.success(Unit)

            } else {

                Result.failure(
                    IllegalStateException(
                        "Không thể tạo buổi học. " +
                                "HTTP ${response.status.value}"
                    )
                )
            }

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.example.smartattendance.data.repository

import com.example.smartattendance.data.mapper.toAttendanceSession
import com.example.smartattendance.data.model.AttendanceSession
import com.example.smartattendance.data.remote.SupabaseProvider
import com.example.smartattendance.data.remote.dto.CreateAttendanceSessionRequestDto
import com.example.smartattendance.data.remote.dto.CreateAttendanceSessionResponseDto
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException

class AttendanceSessionRepository {

    private val supabase = SupabaseProvider.client

    suspend fun createSession(
        meetingId: String,
        durationMinutes: Int = 15
    ): Result<AttendanceSession> {

        return try {

            // 1. Kiểm tra phiên đăng nhập.
            val session = supabase.auth.currentSessionOrNull()

            if (session == null) {
                return Result.failure(
                    IllegalStateException(
                        "Vui lòng đăng nhập lại"
                    )
                )
            }

            // 2. Tạo request DTO.
            val request = CreateAttendanceSessionRequestDto(
                meetingId = meetingId,
                durationMinutes = durationMinutes
            )

            // 3. Gọi Supabase Edge Function.
            val response = supabase.functions.invoke(
                function = "create-attendance-session",
                body = request,
                headers = headersOf(
                    HttpHeaders.ContentType,
                    "application/json"
                )
            )

            // 4. Kiểm tra HTTP status.
            if (response.status.value != 201) {
                return Result.failure(
                    IllegalStateException(
                        "Không thể mở phiên điểm danh. " +
                                "HTTP ${response.status.value}"
                    )
                )
            }

            // 5. Giải mã response JSON thành DTO.
            val responseDto =
                response.body<CreateAttendanceSessionResponseDto>()

            // 6. Chuyển DTO sang Domain Model.
            val attendanceSession =
                responseDto.session.toAttendanceSession()

            // 7. Trả kết quả thành công.
            Result.success(attendanceSession)

        } catch (e: CancellationException) {
            // Không được nuốt lỗi hủy Coroutine.
            throw e

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

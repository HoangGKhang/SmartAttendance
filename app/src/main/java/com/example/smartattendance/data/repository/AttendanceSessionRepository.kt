
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

import com.example.smartattendance.data.remote.dto.GetCurrentAttendanceSessionRequestDto
import com.example.smartattendance.data.remote.dto.GetCurrentAttendanceSessionResponseDto

import com.example.smartattendance.data.model.AttendanceSessionStatus
import com.example.smartattendance.data.remote.dto.CloseAttendanceSessionRequestDto
import com.example.smartattendance.data.remote.dto.CloseAttendanceSessionResponseDto


class AttendanceSessionRepository {

    private val supabase = SupabaseProvider.client

    suspend fun createSession(
        meetingId: String,
        durationMinutes: Int = 1
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


    suspend fun getCurrentSession(
        meetingId: String
    ): Result<AttendanceSession?> {

        return try {
            // 1. Kiểm tra phiên đăng nhập hiện tại.
            val authSession = supabase.auth.currentSessionOrNull()

            if (authSession == null) {
                return Result.failure(
                    IllegalStateException(
                        "Vui lòng đăng nhập lại"
                    )
                )
            }

            // 2. Tạo request DTO.
            val request = GetCurrentAttendanceSessionRequestDto(
                meetingId = meetingId
            )

            // 3. Gọi Edge Function.
            val response = supabase.functions.invoke(
                function = "get-current-attendance-session",
                body = request,
                headers = headersOf(
                    HttpHeaders.ContentType,
                    "application/json"
                )
            )

            // 4. Kiểm tra HTTP status.
            if (response.status.value != 200) {
                return Result.failure(
                    IllegalStateException(
                        "Không thể tải phiên điểm danh. " +
                                "HTTP ${response.status.value}"
                    )
                )
            }

            // 5. Deserialize JSON response.
            val responseDto =
                response.body<GetCurrentAttendanceSessionResponseDto>()

            // 6. Chuyển DTO sang Domain Model.
            // Nếu session là null thì kết quả cũng là null.
            val attendanceSession =
                responseDto.session?.toAttendanceSession()

            // 7. Trả kết quả thành công.
            Result.success(attendanceSession)

        } catch (e: CancellationException) {
            // Không được nuốt lỗi hủy coroutine.
            throw e

        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun closeSession(
        sessionId: String
    ): Result<AttendanceSession> {

        // 1. Kiểm tra đầu vào cơ bản.
        if (sessionId.isBlank()) {
            return Result.failure(
                IllegalArgumentException(
                    "Session ID không được để trống"
                )
            )
        }

        return try {
            // 2. Kiểm tra phiên đăng nhập.
            val authSession = supabase.auth.currentSessionOrNull()

            if (authSession == null) {
                return Result.failure(
                    IllegalStateException(
                        "Vui lòng đăng nhập lại"
                    )
                )
            }

            // 3. Tạo request DTO.
            val request = CloseAttendanceSessionRequestDto(
                sessionId = sessionId
            )

            // 4. Gửi HTTP request đến Edge Function.
            val response = supabase.functions.invoke(
                function = "close-attendance-session",
                body = request,
                headers = headersOf(
                    HttpHeaders.ContentType,
                    "application/json"
                )
            )

            // 5. Kiểm tra HTTP status.
            if (response.status.value != 200) {
                return Result.failure(
                    IllegalStateException(
                        "Không thể đóng phiên điểm danh. " +
                                "HTTP ${response.status.value}"
                    )
                )
            }

            // 6. Giải mã JSON response.
            val responseDto =
                response.body<CloseAttendanceSessionResponseDto>()

            // 7. Chuyển DTO sang Domain Model.
            val attendanceSession =
                responseDto.session.toAttendanceSession()

            // 8. Kiểm tra trạng thái trả về.
            if (
                attendanceSession.status !=
                AttendanceSessionStatus.CLOSED ||
                attendanceSession.closedAt == null
            ) {
                return Result.failure(
                    IllegalStateException(
                        "Dữ liệu phiên đã đóng không hợp lệ"
                    )
                )
            }

            // 9. Trả kết quả thành công.
            Result.success(attendanceSession)

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {
            Result.failure(e)
        }
    }


}

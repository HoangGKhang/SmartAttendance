
package com.example.smartattendance.data.repository

import com.example.smartattendance.data.remote.SupabaseProvider
import com.example.smartattendance.data.remote.dto.VerifyAttendanceGpsRequestDto
import com.example.smartattendance.data.remote.dto.VerifyAttendanceGpsResponseDto

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf

import kotlinx.coroutines.CancellationException

class AttendanceGpsRepository {

    private val supabase = SupabaseProvider.client

    suspend fun verifyGps(
        meetingId: String,
        sessionId: String,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double
    ): Result<Boolean> {

        return try {

            // 1. Kiểm tra trạng thái đăng nhập.
            val authSession =
                supabase.auth.currentSessionOrNull()

            if (authSession == null) {
                return Result.failure(
                    IllegalStateException(
                        "Vui lòng đăng nhập lại"
                    )
                )
            }

            // 2. Kiểm tra dữ liệu trước khi gửi.
            if (
                !latitude.isFinite() ||
                !longitude.isFinite() ||
                !accuracyMeters.isFinite() ||
                latitude !in -90.0..90.0 ||
                longitude !in -180.0..180.0 ||
                accuracyMeters !in 0.0..30.0
            ) {
                return Result.failure(
                    IllegalArgumentException(
                        "Dữ liệu GPS không hợp lệ"
                    )
                )
            }

            // 3. Tạo Request DTO.
            val request = VerifyAttendanceGpsRequestDto(
                meetingId = meetingId,
                sessionId = sessionId,
                latitude = latitude,
                longitude = longitude,
                accuracyMeters = accuracyMeters
            )

            // 4. Gọi Supabase Edge Function.
            val response = supabase.functions.invoke(
                function = "verify-attendance-gps",
                body = request,
                headers = headersOf(
                    HttpHeaders.ContentType,
                    "application/json"
                )
            )

            // 5. Xử lý HTTP status.
            if (response.status.value != 200) {
                return Result.failure(
                    IllegalStateException(
                        "Xác minh GPS thất bại. HTTP ${response.status.value}"
                    )
                )
            }

            // 6. Chuyển JSON response thành Kotlin DTO.
            val responseDto =
                response.body<VerifyAttendanceGpsResponseDto>()

            // 7. Trả kết quả xác minh.
            Result.success(responseDto.verified)

        } catch (e: CancellationException) {

            // Giữ nguyên cơ chế hủy Coroutine.
            throw e

        } catch (e: Exception) {

            Result.failure(e)
        }
    }
}

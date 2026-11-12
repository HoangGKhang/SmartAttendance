
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

import io.github.jan.supabase.exceptions.RestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class GpsVerificationException(
    val httpStatus: Int,
    val errorCode: String?
) : Exception()

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
                return Result.failure(decodeGpsError(response))
            }

            // 6. Chuyển JSON response thành Kotlin DTO.
            val responseDto =
                response.body<VerifyAttendanceGpsResponseDto>()

            // 7. Trả kết quả xác minh.
            Result.success(responseDto.verified)

        } catch (e: CancellationException) {
            throw e
        } catch (e: RestException) {
            Result.failure(
                GpsVerificationException(e.statusCode, parseGpsErrorCode(e.error))
            )
        } catch (e: ResponseException) {
            Result.failure(decodeGpsError(e.response))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun decodeGpsError(
        response: HttpResponse
    ): GpsVerificationException {
        val errorCode = try {
            parseGpsErrorCode(response.bodyAsText())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

        return GpsVerificationException(response.status.value, errorCode)
    }

    private fun parseGpsErrorCode(body: String): String? {
        val json = try {
            Json.parseToJsonElement(body) as? JsonObject
        } catch (e: IllegalArgumentException) {
            null
        }

        return (json?.get("error") as? JsonPrimitive)?.content
    }
}


package com.example.smartattendance.data.repository

import com.example.smartattendance.data.remote.SupabaseProvider
import com.example.smartattendance.data.remote.dto.VerifyAttendanceQrRequestDto
import com.example.smartattendance.data.remote.dto.VerifyAttendanceQrResponseDto
import com.example.smartattendance.data.remote.dto.VerifyAttendanceQrErrorDto

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions

import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf

import kotlinx.coroutines.CancellationException

import java.time.Instant

import io.github.jan.supabase.exceptions.RestException
import kotlinx.serialization.json.Json

enum class VerifyQrFailureReason {
    LOGIN_REQUIRED,
    INVALID_QR,
    QR_EXPIRED,
    SESSION_UNAVAILABLE,
    NOT_ENROLLED,
    WRONG_MEETING,
    SERVER_ERROR,
    INVALID_RESPONSE
}


class VerifyAttendanceQrException(
    val reason: VerifyQrFailureReason
) : IllegalStateException(
    when (reason) {
        VerifyQrFailureReason.LOGIN_REQUIRED ->
            "Vui lòng đăng nhập lại"

        VerifyQrFailureReason.INVALID_QR ->
            "Mã QR không hợp lệ"

        VerifyQrFailureReason.QR_EXPIRED ->
            "Mã QR đã hết hạn. Vui lòng quét mã mới"

        VerifyQrFailureReason.SESSION_UNAVAILABLE ->
            "Phiên điểm danh không khả dụng"

        VerifyQrFailureReason.NOT_ENROLLED ->
            "Bạn không có quyền điểm danh trong lớp này"

        VerifyQrFailureReason.WRONG_MEETING ->
            "Mã QR không thuộc buổi học đang chọn"

        VerifyQrFailureReason.SERVER_ERROR ->
            "Không thể xác minh QR. Vui lòng thử lại"

        VerifyQrFailureReason.INVALID_RESPONSE ->
            "Dữ liệu xác minh từ máy chủ không hợp lệ"
    }
)


class AttendanceQrVerificationRepository {

    private val supabase = SupabaseProvider.client


    suspend fun verifyQr(
        qrToken: String,
        expectedMeetingId: String
    ): Result<VerifyAttendanceQrResponseDto> {

        // 1. Kiểm tra đầu vào.
        if (
            qrToken.isBlank() ||
            qrToken.length > 256 ||
            !qrToken.startsWith("sa1.")
        ) {
            return Result.failure(
                VerifyAttendanceQrException(
                    VerifyQrFailureReason.INVALID_QR
                )
            )
        }

        if (expectedMeetingId.isBlank()) {
            return Result.failure(
                VerifyAttendanceQrException(
                    VerifyQrFailureReason.WRONG_MEETING
                )
            )
        }

        return try {

            // 2. Kiểm tra trạng thái đăng nhập.
            val authSession =
                supabase.auth.currentSessionOrNull()

            if (authSession == null) {
                return Result.failure(
                    VerifyAttendanceQrException(
                        VerifyQrFailureReason.LOGIN_REQUIRED
                    )
                )
            }

            // 3. Tạo JSON request.
            val request = VerifyAttendanceQrRequestDto(
                qrToken = qrToken
            )

            // 4. Gọi backend Verify.
            val response = supabase.functions.invoke(
                function = "verify-attendance-qr",
                body = request,
                headers = headersOf(
                    HttpHeaders.ContentType,
                    "application/json"
                )
            )

            // 5. Xử lý status không thành công.
            if (response.status.value != 200) {
                return Result.failure(
                    decodeApiError(response)
                )
            }

            // 6. Giải mã response.
            val result =
                response.body<VerifyAttendanceQrResponseDto>()

            // 7. Backend bắt buộc xác minh thành công.
            if (!result.verified) {
                return Result.failure(
                    VerifyAttendanceQrException(
                        VerifyQrFailureReason.INVALID_RESPONSE
                    )
                )
            }

            // 8. QR phải thuộc Meeting đang chọn.
            if (result.meetingId != expectedMeetingId) {
                return Result.failure(
                    VerifyAttendanceQrException(
                        VerifyQrFailureReason.WRONG_MEETING
                    )
                )
            }

            // 9. Kiểm tra response có đầy đủ dữ liệu.
            val verifiedAt = runCatching {
                Instant.parse(result.verifiedAt)
            }.getOrNull()

            if (
                result.sessionId.isBlank() ||
                result.meetingId.isBlank() ||
                verifiedAt == null
            ) {
                return Result.failure(
                    VerifyAttendanceQrException(
                        VerifyQrFailureReason.INVALID_RESPONSE
                    )
                )
            }

            // 10. Trả về kết quả.
            Result.success(result)

        } catch (e: CancellationException) {
            // Không nuốt sự kiện hủy coroutine.
            throw e

        } catch (e: RestException) {
            val errorCode = runCatching {
                Json.decodeFromString(
                    VerifyAttendanceQrErrorDto.serializer(),
                    e.error
                ).error
            }.getOrNull()

            Result.failure(classifyApiError(errorCode, e.statusCode))

        } catch (e: ResponseException) {
            // Ktor có thể cung cấp HTTP response khi lỗi.
            Result.failure(
                decodeApiError(e.response)
            )

        } catch (e: Exception) {
            // Không đưa exception gốc lên giao diện.
            // Nó có thể chứa URL hoặc HTTP headers.
            Result.failure(
                VerifyAttendanceQrException(
                    VerifyQrFailureReason.SERVER_ERROR
                )
            )
        }
    }


    /**
     * Đọc lỗi HTTP có cấu trúc từ backend.
     */
    private suspend fun decodeApiError(
        response: HttpResponse
    ): VerifyAttendanceQrException {
        val errorCode = try {
            response.body<VerifyAttendanceQrErrorDto>().error
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

        return classifyApiError(errorCode, response.status.value)
    }

    private fun classifyApiError(
        errorCode: String?,
        status: Int
    ): VerifyAttendanceQrException {
        val reason = when (errorCode) {
            "QR_EXPIRED" -> VerifyQrFailureReason.QR_EXPIRED

            "QR_NOT_AVAILABLE",
            "SESSION_NOT_FOUND" -> VerifyQrFailureReason.SESSION_UNAVAILABLE

            "FORBIDDEN" -> VerifyQrFailureReason.NOT_ENROLLED

            "UNAUTHORIZED" -> VerifyQrFailureReason.LOGIN_REQUIRED

            "INVALID_QR_REQUEST",
            "INVALID_QR_TOKEN",
            "INVALID_QR_SIGNATURE",
            "UNSUPPORTED_QR_VERSION",
            "INVALID_REQUEST_BODY" -> VerifyQrFailureReason.INVALID_QR

            else -> when (status) {
                400 -> VerifyQrFailureReason.INVALID_QR
                401 -> VerifyQrFailureReason.LOGIN_REQUIRED
                403 -> VerifyQrFailureReason.NOT_ENROLLED
                404 -> VerifyQrFailureReason.SESSION_UNAVAILABLE
                else -> VerifyQrFailureReason.SERVER_ERROR
            }
        }

        return VerifyAttendanceQrException(reason)
    }
}

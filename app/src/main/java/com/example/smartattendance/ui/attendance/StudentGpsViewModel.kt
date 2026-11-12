
package com.example.smartattendance.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.example.smartattendance.data.repository.AttendanceGpsRepository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import com.example.smartattendance.data.repository.GpsVerificationException

class StudentGpsViewModel(
    private val repository: AttendanceGpsRepository =
        AttendanceGpsRepository()
) : ViewModel() {

    // Trạng thái nội bộ, chỉ ViewModel được thay đổi.
    private val _uiState = MutableStateFlow(
        StudentGpsUiState()
    )

    // Activity chỉ được quan sát trạng thái.
    val uiState: StateFlow<StudentGpsUiState> =
        _uiState.asStateFlow()

    // Coroutine đang xác minh GPS.
    private var verificationJob: Job? = null

    fun verifyGps(
        meetingId: String,
        sessionId: String,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double
    ) {

        // 1. Ngăn gửi request trùng khi đang xác minh.
        if (
            _uiState.value.status ==
            StudentGpsStatus.VERIFYING
        ) {
            return
        }

        // 2. Kiểm tra định danh tối thiểu.
        if (
            meetingId.isBlank() ||
            sessionId.isBlank()
        ) {
            _uiState.value = StudentGpsUiState(
                status = StudentGpsStatus.ERROR,
                errorMessage = "Thiếu thông tin phiên điểm danh"
            )
            return
        }

        // 3. Chuyển UI sang trạng thái đang xử lý.
        _uiState.value = StudentGpsUiState(
            status = StudentGpsStatus.VERIFYING,
            sessionId = sessionId
        )

        // 4. Gọi Repository trong Coroutine.
        verificationJob = viewModelScope.launch {

            try {
                val result = repository.verifyGps(
                    meetingId = meetingId,
                    sessionId = sessionId,
                    latitude = latitude,
                    longitude = longitude,
                    accuracyMeters = accuracyMeters
                )

                if (!isActive) return@launch

                result.fold(

                    onSuccess = { verified ->

                        // Backend quyết định GPS PASS/FAIL.
                        _uiState.value = StudentGpsUiState(
                            status = if (verified) {
                                StudentGpsStatus.PASS
                            } else {
                                StudentGpsStatus.FAIL
                            },
                            sessionId = sessionId
                        )
                    },

                    onFailure = { error ->

                        _uiState.value = StudentGpsUiState(
                            status = StudentGpsStatus.ERROR,
                            sessionId = sessionId,
                            errorMessage = gpsErrorMessage(error),
                            errorCode = (error as? GpsVerificationException)?.errorCode

                        )

                    }
                )

            } catch (e: CancellationException) {
                // Hủy Coroutine không phải lỗi GPS.
                throw e

            } catch (e: Exception) {

                if (!isActive) return@launch

                _uiState.value = StudentGpsUiState(
                    status = StudentGpsStatus.ERROR,
                    sessionId = sessionId,
                    errorMessage =
                        "Đã xảy ra lỗi khi xác minh GPS"
                )
            }
        }
    }

    private fun gpsErrorMessage(error: Throwable): String {
        val gpsError = error as? GpsVerificationException
            ?: return "Không thể gửi hoặc đọc kết quả xác minh GPS. Vui lòng thử lại."

        return when (gpsError.errorCode) {
            "SESSION_UNAVAILABLE" ->
                "Phiên hoặc buổi học không còn khả dụng. Hãy quét mã của phiên đang mở."

            "SESSION_NOT_FOUND" ->
                "Không tìm thấy phiên điểm danh. Hãy quét lại mã QR."

            "GPS_NOT_CONFIGURED" ->
                "Phiên chưa có cấu hình GPS hợp lệ. Hãy báo giảng viên."

            "UNAUTHORIZED" ->
                "Phiên đăng nhập đã hết hiệu lực. Vui lòng đăng nhập lại."

            "FORBIDDEN" ->
                "Tài khoản của bạn không có quyền điểm danh trong lớp này."

            "INVALID_GPS_DATA" ->
                "Dữ liệu vị trí không hợp lệ. Hãy thử lại vị trí."

            else ->
                "Xác minh vị trí thất bại (HTTP ${gpsError.httpStatus})."
        }
    }

    // Đặt lại trạng thái khi quét QR mới
    // hoặc chuyển sang phiên khác.
    fun resetGpsVerification() {

        verificationJob?.cancel()
        verificationJob = null

        _uiState.value = StudentGpsUiState()
    }
}

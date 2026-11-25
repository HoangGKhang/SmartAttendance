package com.example.smartattendance.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.example.smartattendance.data.repository.AttendanceGpsRepository
import com.example.smartattendance.data.repository.GpsVerificationException

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


class StudentGpsViewModel(
    private val repository: AttendanceGpsRepository =
        AttendanceGpsRepository()
) : ViewModel() {

    // 1. Internal UI State.
    private val _uiState = MutableStateFlow(
        StudentGpsUiState()
    )

    // 2. Read-only UI State.
    val uiState: StateFlow<StudentGpsUiState> =
        _uiState.asStateFlow()

    // 3. Active verification coroutine.
    private var verificationJob: Job? = null


    fun verifyGps(
        meetingId: String,
        sessionId: String,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double
    ) {

        // 1. Prevent duplicate requests.
        val currentState = _uiState.value

        if (
            currentState.status == StudentGpsStatus.VERIFYING
        ) {
            return
        }

        // 2. Do not verify again after completed attendance.
        if (
            currentState.sessionId == sessionId &&
            (
                    currentState.attendanceStatus == "PRESENT" ||
                            currentState.errorCode == "ALREADY_CHECKED_IN"
                    )
        ) {
            return
        }

        // 3. Validate required identifiers.
        if (
            meetingId.isBlank() ||
            sessionId.isBlank()
        ) {
            _uiState.value = StudentGpsUiState(
                status = StudentGpsStatus.ERROR,
                sessionId = sessionId,
                errorMessage = "Thiếu thông tin phiên điểm danh"
            )

            return
        }

        // 4. Set verifying state.
        _uiState.value = StudentGpsUiState(
            status = StudentGpsStatus.VERIFYING,
            sessionId = sessionId
        )

        // 5. Call Repository.
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

                    // 6A. Backend response received.
                    onSuccess = { response ->

                        _uiState.value = StudentGpsUiState(
                            status = if (response.verified) {
                                StudentGpsStatus.PASS
                            } else {
                                StudentGpsStatus.FAIL
                            },

                            sessionId = sessionId,

                            attendanceStatus =
                                response.attendanceStatus
                        )
                    },

                    // 6B. Backend or network error.
                    onFailure = { error ->

                        val errorCode =
                            (error as? GpsVerificationException)
                                ?.errorCode

                        _uiState.value = StudentGpsUiState(
                            status = StudentGpsStatus.ERROR,

                            sessionId = sessionId,

                            errorMessage = gpsErrorMessage(error),

                            errorCode = errorCode,

                            attendanceStatus =
                                if (errorCode == "ALREADY_CHECKED_IN") {
                                    "PRESENT"
                                } else {
                                    null
                                }
                        )
                    }
                )

            } catch (e: CancellationException) {

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


    private fun gpsErrorMessage(
        error: Throwable
    ): String {

        val gpsError =
            error as? GpsVerificationException
                ?: return "Không thể xác minh GPS. Vui lòng thử lại."

        return when (gpsError.errorCode) {

            "SESSION_UNAVAILABLE" ->
                "Phiên điểm danh không còn khả dụng. Hãy quét lại QR."

            "SESSION_NOT_FOUND" ->
                "Không tìm thấy phiên điểm danh. Hãy quét lại QR."

            "GPS_NOT_CONFIGURED" ->
                "Phiên chưa có cấu hình GPS hợp lệ. Hãy báo giảng viên."

            "UNAUTHORIZED" ->
                "Phiên đăng nhập đã hết hiệu lực. Vui lòng đăng nhập lại."

            "FORBIDDEN" ->
                "Bạn không có quyền điểm danh trong lớp này."

            "INVALID_GPS_DATA",
            "INVALID_LATITUDE",
            "INVALID_LONGITUDE",
            "INVALID_ACCURACY" ->
                "Dữ liệu vị trí không hợp lệ. Hãy thử lại vị trí."

            "ATTENDANCE_RECORD_NOT_FOUND" ->
                "Không tìm thấy bản ghi điểm danh. Hãy báo giảng viên."

            "QR_VERIFICATION_REQUIRED" ->
                "Bạn chưa xác minh QR của phiên này. Hãy quét lại QR."

            "ALREADY_CHECKED_IN" ->
                "Bạn đã điểm danh buổi học này trước đó."

            "INVALID_RESPONSE" ->
                "Dữ liệu phản hồi từ máy chủ không hợp lệ."

            else ->
                "Xác minh vị trí thất bại (HTTP ${gpsError.httpStatus})."
        }
    }


    // Reset state when scanning another QR.
    fun resetGpsVerification() {

        verificationJob?.cancel()

        verificationJob = null

        _uiState.value = StudentGpsUiState()
    }
}
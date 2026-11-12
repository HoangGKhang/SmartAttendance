
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

                    onFailure = {
                        _uiState.value = StudentGpsUiState(
                            status = StudentGpsStatus.ERROR,
                            sessionId = sessionId,
                            errorMessage =
                                "Không thể xác minh vị trí GPS"
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

    // Đặt lại trạng thái khi quét QR mới
    // hoặc chuyển sang phiên khác.
    fun resetGpsVerification() {

        verificationJob?.cancel()
        verificationJob = null

        _uiState.value = StudentGpsUiState()
    }
}

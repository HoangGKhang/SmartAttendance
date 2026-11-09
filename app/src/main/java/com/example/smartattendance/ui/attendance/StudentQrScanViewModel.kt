
package com.example.smartattendance.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.example.smartattendance.data.repository.AttendanceQrVerificationRepository
import com.example.smartattendance.data.repository.VerifyAttendanceQrException
import com.example.smartattendance.data.repository.VerifyQrFailureReason

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class StudentQrScanViewModel : ViewModel() {

    private val repository =
        AttendanceQrVerificationRepository()

    private val _uiState =
        MutableStateFlow(StudentQrScanUiState())

    val uiState: StateFlow<StudentQrScanUiState> =
        _uiState.asStateFlow()


    /**
     * Gọi backend xác minh QR.
     */
    fun verifyQr(
        qrToken: String,
        meetingId: String
    ) {

        // 1. Chỉ nhận QR khi đang SCANNING.
        if (
            _uiState.value.status !=
            StudentQrScanStatus.SCANNING
        ) {
            return
        }

        // 2. Chuyển sang VERIFYING ngay,
        // tránh request trùng lặp.
        _uiState.value = StudentQrScanUiState(
            status = StudentQrScanStatus.VERIFYING
        )

        // 3. Gọi Repository bất đồng bộ.
        viewModelScope.launch {

            try {

                val result = repository.verifyQr(
                    qrToken = qrToken,
                    expectedMeetingId = meetingId
                )

                result.fold(

                    // 4A. Backend xác minh thành công.
                    onSuccess = { response ->

                        _uiState.value =
                            StudentQrScanUiState(
                                status =
                                    StudentQrScanStatus.SUCCESS,
                                result = response
                            )
                    },

                    // 4B. Backend từ chối hoặc lỗi mạng.
                    onFailure = { error ->

                        val reason =
                            (error as? VerifyAttendanceQrException)
                                ?.reason
                                ?: VerifyQrFailureReason.SERVER_ERROR

                        _uiState.value =
                            StudentQrScanUiState(
                                status =
                                    StudentQrScanStatus.ERROR,
                                failureReason = reason
                            )
                    }
                )

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {

                _uiState.value =
                    StudentQrScanUiState(
                        status = StudentQrScanStatus.ERROR,
                        failureReason =
                            VerifyQrFailureReason.SERVER_ERROR
                    )
            }
        }
    }


    /**
     * Cho phép quét lại sau lỗi có thể khắc phục.
     */
    fun resetForRetry(): Boolean {

        val state = _uiState.value

        if (state.status != StudentQrScanStatus.ERROR) {
            return false
        }

        // Đăng nhập sai hoặc không thuộc Course:
        // quét lại QR không khắc phục được.
        if (state.failureReason == VerifyQrFailureReason.LOGIN_REQUIRED) {
            return false
        }

        _uiState.value = StudentQrScanUiState(
            status = StudentQrScanStatus.SCANNING
        )

        return true
    }
}

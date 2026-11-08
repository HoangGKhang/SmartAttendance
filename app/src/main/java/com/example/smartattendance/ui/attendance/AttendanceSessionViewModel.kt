
package com.example.smartattendance.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartattendance.data.repository.AttendanceSessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.example.smartattendance.data.model.AttendanceSessionStatus


import java.time.Instant
import kotlinx.coroutines.Job


class AttendanceSessionViewModel(
    private val repository: AttendanceSessionRepository =
        AttendanceSessionRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AttendanceSessionUiState()
    )

    val uiState: StateFlow<AttendanceSessionUiState> =
        _uiState.asStateFlow()


    // Trạng thái QR tách biệt khỏi Session.
    private val _qrUiState = MutableStateFlow(
        AttendanceQrUiState()
    )

    val qrUiState: StateFlow<AttendanceQrUiState> =
        _qrUiState.asStateFlow()

    // Coroutine đang gọi API cấp QR.
    private var qrRequestJob: Job? = null


    // MỚI: Tải Session hiện tại từ Backend.
    fun loadCurrentSession(meetingId: String) {

        // Không gửi request mới khi đang có request khác.
        if (_uiState.value.isLoading) return

        // Xóa QR khi bắt đầu đồng bộ lại Session.
        clearQr()

        // Đánh dấu đang tải, chưa xác định trạng thái.
        _uiState.update {
            it.copy(
                isLoading = true,
                isInitialized = false,
                session = null,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val result = repository.getCurrentSession(
                    meetingId = meetingId
                )

                result.fold(
                    onSuccess = { session ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isInitialized = true,
                                session = session,
                                errorMessage = null
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isInitialized = false,
                                session = null,
                                errorMessage =
                                    error.message
                                        ?: "Không thể tải phiên điểm danh"
                            )
                        }
                    }
                )

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isInitialized = false,
                        session = null,
                        errorMessage =
                            e.message ?: "Đã xảy ra lỗi khi tải phiên"
                    )
                }
            }
        }
    }

    // CŨ: Tạo Session, bổ sung kiểm tra trạng thái.
    fun createSession(
        meetingId: String,
        durationMinutes: Int = 15
    ) {
        val currentState = _uiState.value

        // Không tạo khi:
        // - Đang xử lý request.
        // - Chưa tải thành công trạng thái từ Backend.
        // - Đã có Session OPEN.
        if (
            currentState.isLoading ||
            !currentState.isInitialized ||
            currentState.session != null
        ) {
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val result = repository.createSession(
                    meetingId = meetingId,
                    durationMinutes = durationMinutes
                )

                result.fold(
                    onSuccess = { session ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isInitialized = true,
                                session = session,
                                errorMessage = null
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage =
                                    error.message
                                        ?: "Không thể mở phiên điểm danh"
                            )
                        }
                    }
                )

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage =
                            e.message ?: "Đã xảy ra lỗi"
                    )
                }
            }
        }
    }


    fun closeSession() {

        // 1. Đọc trạng thái hiện tại.
        val currentState = _uiState.value
        val session = currentState.session

        // 2. Không cho phép đóng khi:
        // - Đang có request.
        // - Chưa tải Session thành công.
        // - Không có Session.
        if (
            currentState.isLoading ||
            !currentState.isInitialized ||
            session == null
        ) {
            return
        }

        // 3. Chỉ Session OPEN mới được yêu cầu đóng.
        if (session.status != AttendanceSessionStatus.OPEN) {
            return
        }

        // 4. Đánh dấu đang xử lý trước khi chạy coroutine.
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                // 5. Gọi Repository.
                val result = repository.closeSession(
                    sessionId = session.id
                )

                result.fold(
                    onSuccess = { closedSession ->
                        // 6A. Backend xác nhận đã đóng.
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isInitialized = true,
                                session = closedSession,
                                errorMessage = null
                            )
                        }
                    },
                    onFailure = { error ->
                        // 6B. Thất bại: giữ Session hiện tại.
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage =
                                    error.message
                                        ?: "Không thể đóng phiên điểm danh"
                            )
                        }
                    }
                )

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage =
                            e.message ?: "Đã xảy ra lỗi khi đóng phiên"
                    )
                }
            }
        }
    }



    private fun isSessionEligibleForQr(
        expectedSessionId: String
    ): Boolean {

        val state = _uiState.value
        val session = state.session ?: return false

        return (
                state.isInitialized &&
                        !state.isLoading &&
                        session.id == expectedSessionId &&
                        session.status == AttendanceSessionStatus.OPEN &&
                        session.expiresAt?.isAfter(Instant.now()) == true
                )
    }


    fun requestQr() {

        val state = _uiState.value
        val session = state.session ?: return

        // 1. Chỉ cấp QR cho Session hợp lệ.
        if (!isSessionEligibleForQr(session.id)) {
            clearQr()
            return
        }

        val currentQr = _qrUiState.value

        // 2. Không gửi request trùng khi đang tải.
        if (currentQr.isLoading) return

        // 3. QR hiện tại còn hạn thì chưa cần xin mã mới.
        if (
            currentQr.sessionId == session.id &&
            currentQr.qrToken != null &&
            currentQr.qrExpiresAt?.isAfter(Instant.now()) == true
        ) {
            return
        }

        // 4. Hủy request trước đó nếu còn tồn tại.
        qrRequestJob?.cancel()

        // 5. Xóa QR cũ trong lúc tải mã mới.
        _qrUiState.value = AttendanceQrUiState(
            sessionId = session.id,
            isLoading = true
        )

        // 6. Gọi API bất đồng bộ.
        qrRequestJob = viewModelScope.launch {

            val result = repository.issueQr(
                sessionId = session.id
            )

            // 7. Session có thể đã đóng trong lúc chờ API.
            if (!isSessionEligibleForQr(session.id)) {
                _qrUiState.value = AttendanceQrUiState()
                return@launch
            }

            result.fold(
                onSuccess = { response ->

                    val expiresAt = runCatching {
                        Instant.parse(response.qrExpiresAt)
                    }.getOrNull()

                    // 8. Không lưu QR đã hết hạn.
                    if (
                        expiresAt == null ||
                        !expiresAt.isAfter(Instant.now())
                    ) {
                        _qrUiState.value = AttendanceQrUiState(
                            sessionId = session.id,
                            errorMessage = "Mã QR đã hết hạn"
                        )
                        return@fold
                    }

                    // 9. Cập nhật QR thành công.
                    _qrUiState.value = AttendanceQrUiState(
                        sessionId = session.id,
                        qrToken = response.qrToken,
                        qrExpiresAt = expiresAt,
                        isLoading = false,
                        errorMessage = null
                    )
                },

                onFailure = { error ->

                    // 10. Lỗi QR không làm thay đổi Session.
                    _qrUiState.value = AttendanceQrUiState(
                        sessionId = session.id,
                        isLoading = false,
                        errorMessage = error.message
                            ?: "Không thể tải mã QR"
                    )
                }
            )
        }
    }


    fun clearQr() {
        // Hủy request đang chờ.
        qrRequestJob?.cancel()
        qrRequestJob = null

        // Xóa cả token và thời điểm hết hạn.
        _qrUiState.value = AttendanceQrUiState()
    }


    fun clearError() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }


}

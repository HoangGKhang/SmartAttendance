
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

class AttendanceSessionViewModel(
    private val repository: AttendanceSessionRepository =
        AttendanceSessionRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AttendanceSessionUiState()
    )

    val uiState: StateFlow<AttendanceSessionUiState> =
        _uiState.asStateFlow()

    // MỚI: Tải Session hiện tại từ Backend.
    fun loadCurrentSession(meetingId: String) {

        // Không gửi request mới khi đang có request khác.
        if (_uiState.value.isLoading) return

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


    fun clearError() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }


}

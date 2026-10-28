
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

class AttendanceSessionViewModel(
    private val repository: AttendanceSessionRepository =
        AttendanceSessionRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AttendanceSessionUiState()
    )

    val uiState: StateFlow<AttendanceSessionUiState> =
        _uiState.asStateFlow()

    fun createSession(
        meetingId: String,
        durationMinutes: Int = 15
    ) {
        // Tránh gửi nhiều request khi request trước chưa xong.
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

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

    fun clearError() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }
}

package com.example.smartattendance.ui.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartattendance.data.remote.dto.CreateMeetingRequest
import com.example.smartattendance.data.repository.CreateMeetingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateMeetingViewModel(
    private val repository: CreateMeetingRepository =
        CreateMeetingRepository()
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<CreateMeetingUiState>(
            CreateMeetingUiState.Idle
        )

    val uiState: StateFlow<CreateMeetingUiState> =
        _uiState.asStateFlow()

    fun createMeeting(request: CreateMeetingRequest) {

        if (_uiState.value == CreateMeetingUiState.Loading) {
            return
        }

        viewModelScope.launch {

            _uiState.value = CreateMeetingUiState.Loading

            repository.createMeeting(request)
                .onSuccess {

                    _uiState.value =
                        CreateMeetingUiState.Success
                }
                .onFailure { exception ->

                    _uiState.value =
                        CreateMeetingUiState.Error(
                            exception.message
                                ?: "Không thể tạo buổi học"
                        )
                }
        }
    }
}
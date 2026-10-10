package com.example.smartattendance.ui.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartattendance.data.repository.ClassMeetingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClassMeetingViewModel(
    private val repository: ClassMeetingRepository =
        ClassMeetingRepository()
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<ClassMeetingUiState>(
            ClassMeetingUiState.Idle
        )

    val uiState: StateFlow<ClassMeetingUiState> =
        _uiState.asStateFlow()

    fun loadMeetings(courseId: String) {

        viewModelScope.launch {

            _uiState.value = ClassMeetingUiState.Loading

            repository
                .getMeetings(courseId)
                .onSuccess { meetings ->

                    _uiState.value =
                        ClassMeetingUiState.Success(meetings)
                }
                .onFailure { exception ->

                    _uiState.value =
                        ClassMeetingUiState.Error(
                            exception.message
                                ?: "Không thể tải danh sách buổi học"
                        )
                }
        }
    }
}
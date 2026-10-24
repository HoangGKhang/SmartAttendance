package com.example.smartattendance.ui.meeting

sealed interface CreateMeetingUiState {

    data object Idle : CreateMeetingUiState

    data object Loading : CreateMeetingUiState

    data object Success : CreateMeetingUiState

    data class Error(
        val message: String
    ) : CreateMeetingUiState
}
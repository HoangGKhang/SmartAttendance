package com.example.smartattendance.ui.meeting

import com.example.smartattendance.data.model.ClassMeeting

sealed interface ClassMeetingUiState {

    data object Idle : ClassMeetingUiState

    data object Loading : ClassMeetingUiState

    data class Success(
        val meetings: List<ClassMeeting>
    ) : ClassMeetingUiState

    data class Error(
        val message: String
    ) : ClassMeetingUiState
}
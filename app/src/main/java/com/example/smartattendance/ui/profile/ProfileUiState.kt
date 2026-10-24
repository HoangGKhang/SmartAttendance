package com.example.smartattendance.ui.profile

import com.example.smartattendance.data.model.Profile

sealed interface ProfileUiState {

    data object Idle : ProfileUiState

    data object Loading : ProfileUiState

    data class Success(
        val profile: Profile
    ) : ProfileUiState

    data class Error(
        val message: String
    ) : ProfileUiState
}
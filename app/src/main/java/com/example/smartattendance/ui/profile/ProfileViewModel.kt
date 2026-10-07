package com.example.smartattendance.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartattendance.data.remote.SupabaseProvider
import com.example.smartattendance.data.repository.ProfileRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileRepository: ProfileRepository =
        ProfileRepository()
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<ProfileUiState>(
            ProfileUiState.Idle
        )

    val uiState: StateFlow<ProfileUiState> =
        _uiState.asStateFlow()

    fun loadCurrentUserProfile() {

        if (
            _uiState.value is ProfileUiState.Loading ||
            _uiState.value is ProfileUiState.Success
        ) {
            return
        }

        viewModelScope.launch {

            _uiState.value =
                ProfileUiState.Loading

            val session =
                SupabaseProvider.client.auth
                    .currentSessionOrNull()

            val userId =
                session?.user?.id

            if (userId == null) {

                _uiState.value =
                    ProfileUiState.Error(
                        "Không tìm thấy phiên đăng nhập"
                    )

                return@launch
            }

            val result =
                profileRepository.getProfile(
                    userId = userId
                )

            result
                .onSuccess { profile ->

                    _uiState.value =
                        ProfileUiState.Success(
                            profile
                        )
                }
                .onFailure { exception ->

                    _uiState.value =
                        ProfileUiState.Error(
                            exception.message
                                ?: "Không thể tải profile"
                        )
                }
        }
    }
}
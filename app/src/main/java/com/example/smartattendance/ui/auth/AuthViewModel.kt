package com.example.smartattendance.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartattendance.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState()
    )

    val uiState: StateFlow<AuthUiState> =
        _uiState.asStateFlow()

    fun login(
        email: String,
        password: String
    ) {

        if (_uiState.value.isLoading) {
            return
        }

        viewModelScope.launch {

            _uiState.value = AuthUiState(
                isLoading = true
            )

            val result = authRepository.login(
                email = email,
                password = password
            )

            result
                .onSuccess {

                    _uiState.value = AuthUiState(
                        isLoginSuccess = true
                    )
                }
                .onFailure { exception ->

                    _uiState.value = AuthUiState(
                        errorMessage =
                            exception.message
                                ?: "Đăng nhập thất bại"
                    )
                }
        }
    }

    fun loginHandled() {

        _uiState.value = _uiState.value.copy(
            isLoginSuccess = false
        )
    }

    fun logout() {

        viewModelScope.launch {

            val result =
                authRepository.logout()

            result.onFailure { exception ->

                _uiState.value =
                    AuthUiState(
                        errorMessage =
                            exception.message
                                ?: "Đăng xuất thất bại"
                    )
            }
        }
    }
}
package com.example.smartattendance.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.smartattendance.MainActivity
import com.example.smartattendance.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(
            layoutInflater
        )

        setContentView(binding.root)

        setupListeners()

        observeUiState()
    }

    private fun setupListeners() {

        binding.btnLogin.setOnClickListener {

            clearErrors()

            val email = binding.etEmail.text
                ?.toString()
                ?.trim()
                .orEmpty()

            val password = binding.etPassword.text
                ?.toString()
                .orEmpty()

            val isValid = validateInput(
                email = email,
                password = password
            )

            if (isValid) {

                viewModel.login(
                    email = email,
                    password = password
                )
            }
        }
    }

    private fun observeUiState() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                viewModel.uiState.collect { state ->

                    showLoading(
                        state.isLoading
                    )

                    showError(
                        state.errorMessage
                    )

                    if (state.isLoginSuccess) {

                        openMainActivity()

                        viewModel.loginHandled()
                    }
                }
            }
        }
    }

    private fun validateInput(
        email: String,
        password: String
    ): Boolean {

        if (email.isBlank()) {

            binding.emailInputLayout.error =
                "Vui lòng nhập email"

            return false
        }

        if (password.isBlank()) {

            binding.passwordInputLayout.error =
                "Vui lòng nhập mật khẩu"

            return false
        }

        if (password.length < 6) {

            binding.passwordInputLayout.error =
                "Mật khẩu phải có ít nhất 6 ký tự"

            return false
        }

        return true
    }

    private fun clearErrors() {

        binding.emailInputLayout.error = null
        binding.passwordInputLayout.error = null

        binding.tvError.visibility = View.GONE
    }

    private fun showLoading(
        isLoading: Boolean
    ) {

        if (isLoading) {

            binding.progressBar.visibility =
                View.VISIBLE

            binding.btnLogin.isEnabled = false

        } else {

            binding.progressBar.visibility =
                View.GONE

            binding.btnLogin.isEnabled = true
        }
    }

    private fun showError(
        message: String?
    ) {

        if (message.isNullOrBlank()) {

            binding.tvError.visibility =
                View.GONE

            return
        }

        binding.tvError.text = message

        binding.tvError.visibility =
            View.VISIBLE
    }

    private fun openMainActivity() {

        val intent = Intent(
            this,
            MainActivity::class.java
        )

        startActivity(intent)

        finish()
    }
}
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

//
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import com.example.smartattendance.data.remote.SupabaseProvider
//
import com.example.smartattendance.ui.profile.ProfileUiState
import com.example.smartattendance.ui.profile.ProfileViewModel

//
import com.example.smartattendance.data.model.UserRole
import com.example.smartattendance.ui.lecturer.LecturerHomeActivity
import com.example.smartattendance.ui.student.StudentHomeActivity

//
import android.text.InputType
import com.example.smartattendance.R
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val viewModel: AuthViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(
            layoutInflater
        )

        setContentView(binding.root)

        setupListeners()

        observeUiState()

        observeSession()

        observeProfileState()
    }

    private fun setupListeners() {
        binding.rgLoginMethod.setOnCheckedChangeListener { _, checkedId ->
            clearErrors()

            val isMoodle = checkedId == R.id.rbLoginMoodle

            binding.emailInputLayout.hint = getString(
                if (isMoodle) R.string.login_moodle_username else R.string.email
            )

            binding.etEmail.inputType = if (isMoodle) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            }
        }

        binding.btnLogin.setOnClickListener {
            clearErrors()

            val identifier = binding.etEmail.text?.toString().orEmpty().trim()
            val password = binding.etPassword.text?.toString().orEmpty()

            if (binding.rgLoginMethod.checkedRadioButtonId == R.id.rbLoginMoodle) {
                if (identifier.isBlank()) {
                    binding.emailInputLayout.error =
                        getString(R.string.login_moodle_username_required)
                    return@setOnClickListener
                }

                if (password.isEmpty()) {
                    binding.passwordInputLayout.error =
                        getString(R.string.login_moodle_password_required)
                    return@setOnClickListener
                }

                viewModel.loginWithMoodle(
                    username = identifier,
                    password = password
                )
                return@setOnClickListener
            }

            if (validateInput(email = identifier, password = password)) {
                viewModel.login(
                    email = identifier,
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

    private fun observeSession() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                SupabaseProvider.client.auth
                    .sessionStatus
                    .collect { status ->

                        when (status) {

                            SessionStatus.Initializing -> {

                                showLoading(true)
                            }

                            is SessionStatus.Authenticated -> {

                                profileViewModel
                                    .loadCurrentUserProfile()
                            }

                            is SessionStatus.NotAuthenticated -> {

                                showLoading(false)
                            }

                            is SessionStatus.RefreshFailure -> {

                                showLoading(false)

                                showError(
                                    "Không thể làm mới phiên đăng nhập"
                                )
                            }
                        }
                    }
            }
        }
    }
    private fun openMainActivity() {

        val intent = Intent(
            this,
            MainActivity::class.java
        )

        startActivity(intent)

        finish()
    }

    private fun navigateByRole(
        role: UserRole
    ) {

        when (role) {

            UserRole.STUDENT -> {

                val intent = Intent(
                    this,
                    StudentHomeActivity::class.java
                )

                startActivity(intent)

                finish()
            }

            UserRole.LECTURER -> {

                val intent = Intent(
                    this,
                    LecturerHomeActivity::class.java
                )

                startActivity(intent)

                finish()
            }

            UserRole.ADMIN -> {

                showError(
                    "Giao diện Admin chưa được triển khai"
                )
            }
        }
    }
    private fun observeProfileState() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                profileViewModel.uiState
                    .collect { state ->

                        when (state) {

                            ProfileUiState.Idle -> {
                                // Chưa tải profile
                            }

                            ProfileUiState.Loading -> {

                                showLoading(true)
                            }

                            is ProfileUiState.Success -> {

                                showLoading(false)

                                android.util.Log.d(
                                    "SmartAttendanceProfile",
                                    "Name = ${state.profile.fullName}, " +
                                            "Role = ${state.profile.role}"
                                )

                                navigateByRole(
                                    state.profile.role
                                )
                            }

                            is ProfileUiState.Error -> {

                                showLoading(false)

                                showError(
                                    state.message
                                )
                            }
                        }
                    }
            }
        }
    }
}
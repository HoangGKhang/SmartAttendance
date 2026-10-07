package com.example.smartattendance.ui.student

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.smartattendance.databinding.ActivityStudentHomeBinding
import com.example.smartattendance.ui.auth.AuthViewModel
import com.example.smartattendance.ui.auth.LoginActivity
import com.example.smartattendance.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

class StudentHomeActivity : AppCompatActivity() {

    private lateinit var binding:
            ActivityStudentHomeBinding

    private val authViewModel:
            AuthViewModel by viewModels()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityStudentHomeBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)

        setupListeners()

        observeSession()
    }

    private fun setupListeners() {

        binding.btnLogout.setOnClickListener {

            authViewModel.logout()
        }
    }

    private fun observeSession() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                SupabaseProvider.client.auth
                    .sessionStatus
                    .collect { status ->

                        if (
                            status is
                                    SessionStatus.NotAuthenticated
                        ) {

                            openLogin()
                        }
                    }
            }
        }
    }

    private fun openLogin() {

        val intent = Intent(
            this,
            LoginActivity::class.java
        )

        startActivity(intent)

        finish()
    }
}
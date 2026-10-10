package com.example.smartattendance.ui.lecturer

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartattendance.data.remote.SupabaseProvider
import com.example.smartattendance.databinding.ActivityLecturerHomeBinding
import com.example.smartattendance.ui.auth.AuthViewModel
import com.example.smartattendance.ui.auth.LoginActivity
import com.example.smartattendance.ui.course.CourseAdapter
import com.example.smartattendance.ui.course.CourseUiState
import com.example.smartattendance.ui.course.CourseViewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

import com.example.smartattendance.data.model.Course
import com.example.smartattendance.ui.course.CourseDetailActivity

class LecturerHomeActivity : AppCompatActivity() {

    private lateinit var binding:
            ActivityLecturerHomeBinding

    private val authViewModel:
            AuthViewModel by viewModels()

    private val courseViewModel:
            CourseViewModel by viewModels()

    private lateinit var courseAdapter:
            CourseAdapter

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityLecturerHomeBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)

        setupCourseList()

        setupListeners()

        observeSession()

        observeCourses()

        courseViewModel.loadCourses()
    }

    private fun setupCourseList() {

        courseAdapter = CourseAdapter { course ->
            openCourseDetail(course)
        }

        binding.rvCourses.apply {

            layoutManager =
                LinearLayoutManager(
                    this@LecturerHomeActivity
                )

            adapter = courseAdapter
        }
    }

    private fun setupListeners() {

        binding.btnLogout.setOnClickListener {

            authViewModel.logout()
        }
    }

    private fun observeCourses() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                courseViewModel.uiState
                    .collect { state ->

                        renderCourseState(state)
                    }
            }
        }
    }

    private fun renderCourseState(
        state: CourseUiState
    ) {

        binding.progressBar.visibility =
            View.GONE

        binding.tvError.visibility =
            View.GONE

        binding.tvEmpty.visibility =
            View.GONE

        binding.rvCourses.visibility =
            View.VISIBLE

        when (state) {

            CourseUiState.Idle -> {
                // Chưa load dữ liệu.
            }

            CourseUiState.Loading -> {

                binding.progressBar.visibility =
                    View.VISIBLE

                binding.rvCourses.visibility =
                    View.GONE
            }

            is CourseUiState.Success -> {

                courseAdapter.submitList(
                    state.courses
                )

                if (state.courses.isEmpty()) {

                    binding.tvEmpty.visibility =
                        View.VISIBLE

                    binding.rvCourses.visibility =
                        View.GONE
                }
            }

            is CourseUiState.Error -> {

                binding.tvError.text =
                    state.message

                binding.tvError.visibility =
                    View.VISIBLE

                binding.rvCourses.visibility =
                    View.GONE
            }
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

    private fun openCourseDetail(course: Course) {

        val intent = Intent(
            this,
            CourseDetailActivity::class.java
        ).apply {
            putExtra(
                CourseDetailActivity.EXTRA_COURSE_ID,
                course.id
            )

            putExtra(
                CourseDetailActivity.EXTRA_COURSE_NAME,
                course.name
            )

            putExtra(
                CourseDetailActivity.EXTRA_CLASS_CODE,
                course.classCode
            )
        }

        startActivity(intent)
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
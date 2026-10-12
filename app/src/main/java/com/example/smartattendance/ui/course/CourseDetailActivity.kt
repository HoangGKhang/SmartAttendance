package com.example.smartattendance.ui.course

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartattendance.databinding.ActivityCourseDetailBinding
import com.example.smartattendance.ui.meeting.ClassMeetingAdapter
import com.example.smartattendance.ui.meeting.ClassMeetingUiState
import com.example.smartattendance.ui.meeting.ClassMeetingViewModel
import kotlinx.coroutines.launch

import android.content.Intent
import com.example.smartattendance.ui.meeting.CreateMeetingActivity

class CourseDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCourseDetailBinding

    private val meetingViewModel:
            ClassMeetingViewModel by viewModels()

    private lateinit var meetingAdapter: ClassMeetingAdapter

    companion object {
        const val EXTRA_COURSE_ID = "extra_course_id"
        const val EXTRA_COURSE_NAME = "extra_course_name"
        const val EXTRA_CLASS_CODE = "extra_class_code"

        const val EXTRA_CAN_CREATE_MEETING =  "extra_can_create_meeting"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCourseDetailBinding.inflate(
            layoutInflater
        )

        setContentView(binding.root)

        val courseId =
            intent.getStringExtra(EXTRA_COURSE_ID)

        val courseName =
            intent.getStringExtra(EXTRA_COURSE_NAME)

        val classCode =
            intent.getStringExtra(EXTRA_CLASS_CODE)

        val canCreateMeeting =
            intent.getBooleanExtra(EXTRA_CAN_CREATE_MEETING, false)

        if (courseId.isNullOrBlank()) {
            finish()
            return
        }

        binding.tvCourseName.text =
            courseName ?: "Không có tên môn học"

        binding.tvClassCode.text =
            classCode ?: "Không có mã lớp"

        binding.btnCreateMeeting.visibility =
            if (canCreateMeeting) View.VISIBLE else View.GONE

        binding.btnCreateMeeting.setOnClickListener {

            val createIntent = Intent(
                this,
                CreateMeetingActivity::class.java
            ).apply {

                putExtra(
                    CreateMeetingActivity.EXTRA_COURSE_ID,
                    courseId
                )

                putExtra(
                    CreateMeetingActivity.EXTRA_COURSE_NAME,
                    courseName
                )
            }

            startActivity(createIntent)
        }

        setupMeetingList()

        observeMeetings()

        if (savedInstanceState == null) {
            meetingViewModel.loadMeetings(courseId)
        }
    }

    private fun setupMeetingList() {

        meetingAdapter = ClassMeetingAdapter()

        binding.rvMeetings.apply {
            layoutManager = LinearLayoutManager(
                this@CourseDetailActivity
            )

            adapter = meetingAdapter
        }
    }

    private fun observeMeetings() {

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {

                meetingViewModel.uiState.collect { state ->
                    renderMeetingState(state)
                }
            }
        }
    }

    private fun renderMeetingState(
        state: ClassMeetingUiState
    ) {

        binding.progressBar.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        binding.tvError.visibility = View.GONE
        binding.rvMeetings.visibility = View.GONE

        when (state) {

            ClassMeetingUiState.Idle -> Unit

            ClassMeetingUiState.Loading -> {
                binding.progressBar.visibility = View.VISIBLE
            }

            is ClassMeetingUiState.Success -> {

                meetingAdapter.submitList(state.meetings)

                if (state.meetings.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                } else {
                    binding.rvMeetings.visibility = View.VISIBLE
                }
            }

            is ClassMeetingUiState.Error -> {

                binding.tvError.text = state.message
                binding.tvError.visibility = View.VISIBLE
            }
        }
    }
}
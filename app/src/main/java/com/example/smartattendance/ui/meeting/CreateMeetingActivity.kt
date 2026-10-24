package com.example.smartattendance.ui.meeting

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.smartattendance.databinding.ActivityCreateMeetingBinding
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

import android.app.Activity
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.smartattendance.data.remote.dto.CreateMeetingRequest
import kotlinx.coroutines.launch

class CreateMeetingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateMeetingBinding
    private val createMeetingViewModel:
            CreateMeetingViewModel by viewModels()

    private var selectedDate: LocalDate? = null
    private var selectedStartTime: LocalTime? = null
    private var selectedEndTime: LocalTime? = null

    private val dateFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy")

    private val timeFormatter =
        DateTimeFormatter.ofPattern("HH:mm")

    companion object {
        const val EXTRA_COURSE_ID = "extra_course_id"
        const val EXTRA_COURSE_NAME = "extra_course_name"

        private const val STATE_DATE = "state_date"
        private const val STATE_START = "state_start"
        private const val STATE_END = "state_end"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCreateMeetingBinding.inflate(
            layoutInflater
        )

        setContentView(binding.root)

        val courseId = intent.getStringExtra(EXTRA_COURSE_ID)
        val courseName = intent.getStringExtra(EXTRA_COURSE_NAME)

        if (courseId.isNullOrBlank()) {
            finish()
            return
        }

        binding.tvCourseInfo.text =
            courseName ?: "Môn học"

        restoreSelections(savedInstanceState)
        setupListeners()
        observeCreateMeetingState()
    }

    private fun setupListeners() {

        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnStartTime.setOnClickListener {
            showTimePicker(isStartTime = true)
        }

        binding.btnEndTime.setOnClickListener {
            showTimePicker(isStartTime = false)
        }

        binding.btnCreateMeeting.setOnClickListener {
            validateForm()
        }
    }

    private fun showDatePicker() {

        val initialDate = selectedDate ?: LocalDate.now()

        val dialog = DatePickerDialog(
            this,
            { _, year, month, day ->

                selectedDate = LocalDate.of(
                    year,
                    month + 1,
                    day
                )

                binding.tvSelectedDate.text =
                    selectedDate!!.format(dateFormatter)
            },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        )

        dialog.show()
    }

    private fun showTimePicker(isStartTime: Boolean) {

        val initialTime =
            if (isStartTime) selectedStartTime
            else selectedEndTime

        val time = initialTime ?: LocalTime.of(7, 30)

        TimePickerDialog(
            this,
            { _, hour, minute ->

                val selected = LocalTime.of(hour, minute)

                if (isStartTime) {
                    selectedStartTime = selected

                    binding.tvStartTime.text =
                        selected.format(timeFormatter)
                } else {
                    selectedEndTime = selected

                    binding.tvEndTime.text =
                        selected.format(timeFormatter)
                }
            },
            time.hour,
            time.minute,
            true
        ).show()
    }

    private fun validateForm() {

        binding.tvFormError.visibility = View.GONE

        val number = binding.etMeetingNumber
            .text.toString().trim().toIntOrNull()

        val title = binding.etTitle
            .text.toString().trim()

        val room = binding.etRoom
            .text.toString().trim()

        val date = selectedDate
        val start = selectedStartTime
        val end = selectedEndTime

        if (number == null || number <= 0) {
            showError("Số thứ tự buổi học phải lớn hơn 0")
            return
        }

        if (date == null) {
            showError("Vui lòng chọn ngày học")
            return
        }

        if (start == null || end == null) {
            showError("Vui lòng chọn giờ bắt đầu và kết thúc")
            return
        }

        if (!start.isBefore(end)) {
            showError("Giờ kết thúc phải sau giờ bắt đầu")
            return
        }

        // title và room là hai trường không bắt buộc.
        // Giới hạn chiều dài đã được đặt ở XML.

        // Bước 4.2.8:
        // Đưa dữ liệu hợp lệ vào ViewModel.
        // Chưa gửi HTTP request trong bước này.

        val courseId =
            intent.getStringExtra(EXTRA_COURSE_ID)

        if (courseId.isNullOrBlank()) {
            showError("Không xác định được môn học")
            return
        }

        val request = CreateMeetingRequest(
            courseId = courseId,
            meetingNumber = number,
            meetingDate = date.toString(),
            title = title.ifBlank { null },
            startTime = start.format(timeFormatter),
            endTime = end.format(timeFormatter),
            room = room.ifBlank { null }
        )

        createMeetingViewModel.createMeeting(request)
    }

    private fun showError(message: String) {
        binding.tvFormError.text = message
        binding.tvFormError.visibility = View.VISIBLE
    }

    private fun restoreSelections(state: Bundle?) {
        if (state == null) return

        state.getString(STATE_DATE)?.let {
            selectedDate = LocalDate.parse(it)
            binding.tvSelectedDate.text =
                selectedDate!!.format(dateFormatter)
        }

        state.getString(STATE_START)?.let {
            selectedStartTime = LocalTime.parse(it)
            binding.tvStartTime.text =
                selectedStartTime!!.format(timeFormatter)
        }

        state.getString(STATE_END)?.let {
            selectedEndTime = LocalTime.parse(it)
            binding.tvEndTime.text =
                selectedEndTime!!.format(timeFormatter)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        selectedDate?.let {
            outState.putString(STATE_DATE, it.toString())
        }

        selectedStartTime?.let {
            outState.putString(STATE_START, it.toString())
        }

        selectedEndTime?.let {
            outState.putString(STATE_END, it.toString())
        }

        super.onSaveInstanceState(outState)
    }

    private fun observeCreateMeetingState() {

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {

                createMeetingViewModel.uiState.collect { state ->
                    renderCreateMeetingState(state)
                }
            }
        }
    }

    private fun renderCreateMeetingState(
        state: CreateMeetingUiState
    ) {

        when (state) {

            CreateMeetingUiState.Idle -> {
                binding.btnCreateMeeting.isEnabled = true
            }

            CreateMeetingUiState.Loading -> {
                binding.btnCreateMeeting.isEnabled = false
                binding.btnCreateMeeting.text = "Đang tạo..."
                binding.tvFormError.visibility = View.GONE
            }

            CreateMeetingUiState.Success -> {

                Toast.makeText(
                    this,
                    "Tạo buổi học thành công",
                    Toast.LENGTH_SHORT
                ).show()

                setResult(Activity.RESULT_OK)
                finish()
            }

            is CreateMeetingUiState.Error -> {
                binding.btnCreateMeeting.isEnabled = true
                binding.btnCreateMeeting.text = "Tạo buổi học"
                showError(state.message)
            }
        }
    }
}
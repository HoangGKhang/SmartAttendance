package com.example.smartattendance.ui.course

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smartattendance.databinding.ActivityCourseDetailBinding

class CourseDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCourseDetailBinding

    companion object {
        const val EXTRA_COURSE_ID = "extra_course_id"
        const val EXTRA_COURSE_NAME = "extra_course_name"
        const val EXTRA_CLASS_CODE = "extra_class_code"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCourseDetailBinding.inflate(
            layoutInflater
        )

        setContentView(binding.root)

        val courseId = intent.getStringExtra(EXTRA_COURSE_ID)
        val courseName = intent.getStringExtra(EXTRA_COURSE_NAME)
        val classCode = intent.getStringExtra(EXTRA_CLASS_CODE)

        if (courseId.isNullOrBlank()) {
            finish()
            return
        }

        binding.tvCourseName.text =
            courseName ?: "Không có tên môn học"

        binding.tvClassCode.text =
            classCode ?: "Không có mã lớp"
    }
}
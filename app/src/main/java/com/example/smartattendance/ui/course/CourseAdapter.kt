package com.example.smartattendance.ui.course

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartattendance.data.model.Course
import com.example.smartattendance.databinding.ItemCourseBinding

class CourseAdapter(
    private val onCourseClick: (Course) -> Unit
) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    private var courses: List<Course> = emptyList()

    fun submitList(newCourses: List<Course>) {
        courses = newCourses
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CourseViewHolder {

        val binding = ItemCourseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return CourseViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: CourseViewHolder,
        position: Int
    ) {
        holder.bind(courses[position])
    }

    override fun getItemCount(): Int {
        return courses.size
    }

    inner class CourseViewHolder(
        private val binding: ItemCourseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(course: Course) {

            binding.tvClassCode.text =
                course.classCode

            binding.tvCourseName.text =
                course.name

            binding.tvSemester.text =
                "${course.semester} • ${course.academicYear}"

            val dateText =
                if (
                    course.startDate != null &&
                    course.endDate != null
                ) {
                    "${course.startDate} → ${course.endDate}"
                } else {
                    ""
                }

            binding.tvCourseDates.text = dateText

            binding.root.setOnClickListener {
                onCourseClick(course)
            }
        }
    }
}
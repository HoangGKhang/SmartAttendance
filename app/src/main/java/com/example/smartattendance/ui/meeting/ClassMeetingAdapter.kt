
package com.example.smartattendance.ui.meeting

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartattendance.data.model.ClassMeeting
import com.example.smartattendance.data.model.MeetingStatus
import com.example.smartattendance.databinding.ItemClassMeetingBinding
import java.time.format.DateTimeFormatter

class ClassMeetingAdapter :
    ListAdapter<ClassMeeting, ClassMeetingAdapter.MeetingViewHolder>(
        MeetingDiffCallback
    ) {

    private val dateFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy")

    private val timeFormatter =
        DateTimeFormatter.ofPattern("HH:mm")

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MeetingViewHolder {

        val binding = ItemClassMeetingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return MeetingViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MeetingViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    inner class MeetingViewHolder(
        private val binding: ItemClassMeetingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(meeting: ClassMeeting) {

            binding.tvMeetingTitle.text =
                meeting.title
                    ?: "Buổi ${meeting.meetingNumber}"

            binding.tvMeetingDate.text =
                meeting.meetingDate.format(dateFormatter)

            val startTime =
                meeting.startTime?.format(timeFormatter)

            val endTime =
                meeting.endTime?.format(timeFormatter)

            binding.tvMeetingTime.text =
                if (startTime != null && endTime != null) {
                    "$startTime - $endTime"
                } else {
                    "Chưa xác định giờ học"
                }

            binding.tvMeetingRoom.text =
                meeting.room?.let { "Phòng $it" }
                    ?: "Chưa xác định phòng"

            binding.tvMeetingStatus.text =
                when (meeting.status) {
                    MeetingStatus.SCHEDULED -> "Đã lên lịch"
                    MeetingStatus.COMPLETED -> "Đã hoàn thành"
                    MeetingStatus.CANCELLED -> "Đã hủy"
                }
        }
    }

    companion object {

        private val MeetingDiffCallback =
            object : DiffUtil.ItemCallback<ClassMeeting>() {

                override fun areItemsTheSame(
                    oldItem: ClassMeeting,
                    newItem: ClassMeeting
                ): Boolean {
                    return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(
                    oldItem: ClassMeeting,
                    newItem: ClassMeeting
                ): Boolean {
                    return oldItem == newItem
                }
            }
    }
}

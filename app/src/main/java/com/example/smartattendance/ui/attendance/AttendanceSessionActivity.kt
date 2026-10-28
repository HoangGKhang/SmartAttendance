
package com.example.smartattendance.ui.attendance

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import com.example.smartattendance.R

class AttendanceSessionActivity : ComponentActivity() {

    private val viewModel: AttendanceSessionViewModel by viewModels()

    private lateinit var tvMeetingInfo: TextView
    private lateinit var tvSessionStatus: TextView
    private lateinit var tvSessionResult: TextView
    private lateinit var progressSession: ProgressBar
    private lateinit var btnStartSession: Button

    private var meetingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance_session)

        tvMeetingInfo = findViewById(R.id.tvMeetingInfo)
        tvSessionStatus = findViewById(R.id.tvSessionStatus)
        tvSessionResult = findViewById(R.id.tvSessionResult)
        progressSession = findViewById(R.id.progressSession)
        btnStartSession = findViewById(R.id.btnStartSession)

        meetingId = intent.getStringExtra(EXTRA_MEETING_ID)

        if (meetingId.isNullOrBlank()) {
            tvSessionResult.text = "Không tìm thấy buổi học"
            btnStartSession.isEnabled = false
            return
        }

        tvMeetingInfo.text = "Meeting: $meetingId"

        btnStartSession.setOnClickListener {
            val id = meetingId ?: return@setOnClickListener
            viewModel.createSession(
                meetingId = id,
                durationMinutes = 15
            )
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    progressSession.visibility =
                        if (state.isLoading) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    btnStartSession.isEnabled =
                        !state.isLoading &&
                                state.session == null

                    state.session?.let { session ->
                        tvSessionStatus.text =
                            "Trạng thái: ${session.status}"

                        tvSessionResult.text =
                            "Hết hạn: ${session.expiresAt}"
                    }

                    state.errorMessage?.let { message ->
                        tvSessionResult.text = message
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_MEETING_ID = "extra_meeting_id"
    }
}

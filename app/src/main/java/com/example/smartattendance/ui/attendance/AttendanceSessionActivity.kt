
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
import com.example.smartattendance.R
import com.example.smartattendance.data.model.AttendanceSessionStatus
import java.time.Instant
import kotlinx.coroutines.launch

class AttendanceSessionActivity : ComponentActivity() {

    private val viewModel: AttendanceSessionViewModel by viewModels()

    private lateinit var tvMeetingInfo: TextView
    private lateinit var tvSessionStatus: TextView
    private lateinit var tvSessionResult: TextView
    private lateinit var progressSession: ProgressBar

    private lateinit var btnStartSession: Button
    private lateinit var btnCloseSession: Button
    private lateinit var btnRefreshSession: Button

    private var meetingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance_session)

        // 1. Ánh xạ giao diện XML.
        tvMeetingInfo = findViewById(R.id.tvMeetingInfo)
        tvSessionStatus = findViewById(R.id.tvSessionStatus)
        tvSessionResult = findViewById(R.id.tvSessionResult)
        progressSession = findViewById(R.id.progressSession)

        btnStartSession = findViewById(R.id.btnStartSession)
        btnCloseSession = findViewById(R.id.btnCloseSession)
        btnRefreshSession = findViewById(R.id.btnRefreshSession)

        // 2. Nhận Meeting ID.
        meetingId = intent.getStringExtra(EXTRA_MEETING_ID)

        val id = meetingId

        if (id.isNullOrBlank()) {
            tvSessionStatus.text = "Không xác định"
            tvSessionResult.text = "Không tìm thấy buổi học"

            btnStartSession.isEnabled = false
            btnCloseSession.isEnabled = false
            btnRefreshSession.isEnabled = false
            return
        }

        tvMeetingInfo.text = "Meeting: $id"

        // 3. Nút mở Session.
        btnStartSession.setOnClickListener {
            val state = viewModel.uiState.value

            if (
                !state.isLoading &&
                state.isInitialized &&
                state.session == null &&
                state.errorMessage == null
            ) {
                viewModel.createSession(
                    meetingId = id,
                    durationMinutes = 15
                )
            }
        }

        // 4. Nút đóng Session.
        btnCloseSession.setOnClickListener {
            viewModel.closeSession()
        }

        // 5. Nút làm mới Session.
        btnRefreshSession.setOnClickListener {
            if (!viewModel.uiState.value.isLoading) {
                viewModel.loadCurrentSession(id)
            }
        }

        // 6. Quan sát StateFlow.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderUi(state)
                }
            }
        }

        // 7. Tự tải Session khi mở màn hình lần đầu.
        val currentState = viewModel.uiState.value

        if (
            !currentState.isInitialized &&
            !currentState.isLoading
        ) {
            viewModel.loadCurrentSession(id)
        }
    }

    private fun renderUi(state: AttendanceSessionUiState) {

        progressSession.visibility =
            if (state.isLoading) View.VISIBLE
            else View.GONE

        // Thiết lập trạng thái nút mặc định.
        btnStartSession.text = "Mở phiên điểm danh"
        btnCloseSession.text = "Đóng phiên điểm danh"
        btnRefreshSession.text = "Làm mới trạng thái"

        btnStartSession.isEnabled = false
        btnCloseSession.isEnabled = false
        btnRefreshSession.isEnabled = !state.isLoading

        // 1. Đang xử lý request.
        if (state.isLoading) {
            tvSessionStatus.text = "Đang xử lý"
            tvSessionResult.text = "Đang làm việc với máy chủ..."
            return
        }

        // 2. Chưa tải dữ liệu thành công.
        if (!state.isInitialized) {
            tvSessionStatus.text = "Chưa xác định"

            tvSessionResult.text =
                state.errorMessage
                    ?: "Chưa có thông tin phiên điểm danh"

            return
        }

        val session = state.session

        // 3. Backend không có Session OPEN.
        if (session == null) {
            tvSessionStatus.text = "Chưa có phiên đang mở"

            tvSessionResult.text =
                state.errorMessage
                    ?: "Có thể mở phiên điểm danh mới"

            // Sau lỗi tạo phiên, phải làm mới trước.
            btnStartSession.isEnabled =
                state.errorMessage == null

            return
        }

        // 4. Backend có Session.
        val now = Instant.now()

        val isExpired =
            session.expiresAt?.let { expiresAt ->
                !now.isBefore(expiresAt)
            } == true

        tvSessionStatus.text = when {
            session.status == AttendanceSessionStatus.CLOSED ->
                "Phiên đã đóng"

            session.isActive(now) ->
                "Phiên đang diễn ra"

            session.status == AttendanceSessionStatus.OPEN &&
                    isExpired ->
                "Phiên hết hạn (Database: OPEN)"

            session.status == AttendanceSessionStatus.OPEN ->
                "Phiên OPEN nhưng chưa hoạt động"

            else ->
                "Trạng thái: ${session.status}"
        }

        val sessionInfo =
            "Session ID: ${session.id}\n" +
                    "Bắt đầu: ${session.openedAt ?: "Chưa có"}\n" +
                    "Hết hạn: ${session.expiresAt ?: "Chưa có"}\n" +
                    "Đã đóng: ${session.closedAt ?: "Chưa đóng"}"

        tvSessionResult.text =
            if (state.errorMessage != null) {
                "$sessionInfo\n\nLỗi: ${state.errorMessage}"
            } else {
                sessionInfo
            }

        // Chỉ cho phép đóng Session OPEN.
        // Nếu request trước bị lỗi, yêu cầu làm mới trước.
        btnCloseSession.isEnabled =
            session.status == AttendanceSessionStatus.OPEN &&
                    state.errorMessage == null
    }

    companion object {
        const val EXTRA_MEETING_ID = "extra_meeting_id"
    }
}

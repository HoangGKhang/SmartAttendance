
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

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

import android.widget.ImageView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    private lateinit var tvSessionCountdown: TextView

    private var lastAutoRefreshSessionId: String? = null


    private lateinit var layoutQrContainer: View
    private lateinit var ivAttendanceQr: ImageView
    private lateinit var tvQrStatus: TextView

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

        tvSessionCountdown =
            findViewById(R.id.tvSessionCountdown)

        layoutQrContainer =
            findViewById(R.id.layoutQrContainer)

        ivAttendanceQr =
            findViewById(R.id.ivAttendanceQr)

        tvQrStatus =
            findViewById(R.id.tvQrStatus)
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

        observeQrUi()

        // 6. Quan sát StateFlow.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                viewModel.uiState.collectLatest { state ->

                    // 1. Hiển thị trạng thái hiện tại.
                    renderUi(state)

                    val session = state.session


                    // Khi Session OPEN, yêu cầu QR nếu chưa có mã hợp lệ.
                    if (
                        state.isInitialized &&
                        !state.isLoading &&
                        session?.status == AttendanceSessionStatus.OPEN &&
                        session.expiresAt?.isAfter(Instant.now()) == true
                    ) {
                        viewModel.requestQr()
                    }


                    // 2. Chỉ đếm ngược khi có Session OPEN.
                    if (
                        state.isLoading ||
                        !state.isInitialized ||
                        session == null ||
                        session.status != AttendanceSessionStatus.OPEN
                    ) {
                        return@collectLatest
                    }

                    // 3. Lấy thời điểm hết hạn.
                    val expiresAt = session.expiresAt

                    if (expiresAt == null) {
                        tvSessionCountdown.text =
                            "Không có thời hạn phiên"
                        return@collectLatest
                    }

                    // 4. Cập nhật countdown mỗi giây.
                    while (true) {

                        val remainingMillis =
                            expiresAt.toEpochMilli() -
                                    Instant.now().toEpochMilli()

                        // 5. Phiên đã hết hạn theo đồng hồ Android.
                        if (remainingMillis <= 0L) {

                            tvSessionCountdown.text =
                                "00:00 — Đã hết hạn"

                            // Chỉ tự refresh một lần cho cùng Session.
                            if (lastAutoRefreshSessionId != session.id) {
                                lastAutoRefreshSessionId = session.id

                                viewModel.loadCurrentSession(id)
                            }

                            return@collectLatest
                        }

                        // 6. Làm tròn lên số giây còn lại.
                        val totalSeconds =
                            (remainingMillis + 999L) / 1000L

                        val minutes = totalSeconds / 60
                        val seconds = totalSeconds % 60

                        // 7. Cập nhật giao diện.
                        tvSessionCountdown.text =
                            "Còn lại: %02d:%02d".format(
                                minutes,
                                seconds
                            )

                        // 8. Chờ 1 giây trước khi cập nhật tiếp.
                        delay(1000L)
                    }
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

            isExpired ->
                "Phiên đã hết hạn"

            session.status == AttendanceSessionStatus.OPEN ->
                "Đang điểm danh"

            else ->
                "Chưa xác định"
        }


        tvSessionCountdown.text = when {
            state.isLoading ->
                "Đang đồng bộ..."

            !state.isInitialized ->
                "--:--"

            state.session == null ->
                "Chưa có phiên đang mở"

            state.session.status == AttendanceSessionStatus.CLOSED ->
                "Phiên đã đóng"

            state.session.status == AttendanceSessionStatus.OPEN ->
                "Đang tính thời gian..."

            else ->
                "--:--"
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


    private fun observeQrUi() {

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {

                viewModel.qrUiState.collectLatest { qrState ->

                    // 1. Kiểm tra trạng thái Session.
                    val sessionState = viewModel.uiState.value
                    val session = sessionState.session

                    val sessionIsOpen =
                        sessionState.isInitialized &&
                                session?.status ==
                                AttendanceSessionStatus.OPEN &&
                                session.expiresAt
                                    ?.isAfter(Instant.now()) == true

                    // 2. Session không hợp lệ: ẩn QR.
                    if (!sessionIsOpen || session == null) {
                        layoutQrContainer.visibility = View.GONE
                        ivAttendanceQr.setImageDrawable(null)

                        return@collectLatest
                    }

                    // 3. Hiển thị khu vực QR.
                    layoutQrContainer.visibility = View.VISIBLE

                    // 4. Chỉ nhận token thuộc đúng Session.
                    val token = qrState.qrToken
                    val expiresAt = qrState.qrExpiresAt

                    val validToken =
                        qrState.sessionId == session.id &&
                                !token.isNullOrBlank() &&
                                expiresAt?.isAfter(Instant.now()) == true

                    if (!validToken) {

                        ivAttendanceQr.setImageDrawable(null)
                        ivAttendanceQr.visibility = View.GONE

                        tvQrStatus.text = when {
                            qrState.isLoading ->
                                "Đang lấy mã QR..."

                            qrState.errorMessage != null ->
                                qrState.errorMessage

                            else ->
                                "Đang chờ cấp mã QR..."
                        }


                        // Thử lại có giới hạn nhịp khi API cấp QR lỗi.
                        if (
                            !qrState.isLoading &&
                            qrState.errorMessage != null
                        ) {
                            delay(2000L)

                            val current = viewModel.uiState.value
                            val currentSession = current.session

                            if (
                                current.isInitialized &&
                                !current.isLoading &&
                                currentSession?.id == session.id &&
                                currentSession.status ==
                                AttendanceSessionStatus.OPEN &&
                                currentSession.expiresAt
                                    ?.isAfter(Instant.now()) == true
                            ) {
                                viewModel.requestQr()
                            }
                        }

                        return@collectLatest
                    }

                    // 5. Chưa hiển thị ảnh trong lúc tạo bitmap.
                    ivAttendanceQr.visibility = View.GONE
                    tvQrStatus.text = "Đang tạo hình QR..."

                    try {

                        // 6. Tạo Bitmap ở background thread.
                        val bitmap = withContext(
                            Dispatchers.Default
                        ) {
                            QrBitmapGenerator.generate(
                                token!!
                            )
                        }

                        // 7. Kiểm tra lại vì thời gian có thể
                        // đã thay đổi khi đang tạo bitmap.
                        val latestSession =
                            viewModel.uiState.value.session

                        if (
                            latestSession?.id != session.id ||
                            latestSession.status !=
                            AttendanceSessionStatus.OPEN ||
                            latestSession.expiresAt
                                ?.isAfter(Instant.now()) != true ||
                            expiresAt == null ||
                            !expiresAt.isAfter(Instant.now())
                        ) {

                            ivAttendanceQr.setImageDrawable(null)
                            ivAttendanceQr.visibility = View.GONE

                            tvQrStatus.text =
                                "QR đã hết hạn"

                            return@collectLatest
                        }

                        // 8. Hiển thị mã QR.
                        ivAttendanceQr.setImageBitmap(bitmap)
                        ivAttendanceQr.visibility = View.VISIBLE

                        tvQrStatus.text =
                            "Quét mã QR để điểm danh"


                        // 9. Tính thời gian QR còn hiệu lực.
                        val remainingQrMillis =
                            expiresAt.toEpochMilli() -
                                    Instant.now().toEpochMilli()

                        // 10. Chờ đến thời điểm QR hết hạn.
                        // Coroutine tự bị hủy khi Activity STOPPED
                        // hoặc khi collectLatest nhận QR state mới.
                        if (remainingQrMillis > 0L) {
                            delay(remainingQrMillis)
                        }

                        // 11. Xóa mã đã hết hạn khỏi màn hình.
                        ivAttendanceQr.setImageDrawable(null)
                        ivAttendanceQr.visibility = View.GONE

                        tvQrStatus.text =
                            "Đang cập nhật mã QR..."

                        // 12. Yêu cầu token thuộc cửa sổ tiếp theo.
                        // ViewModel sẽ tự kiểm tra Session còn OPEN không.
                        viewModel.requestQr()

                    } catch (e: CancellationException) {
                        throw e

                    } catch (e: Exception) {

                        ivAttendanceQr.setImageDrawable(null)
                        ivAttendanceQr.visibility = View.GONE

                        tvQrStatus.text =
                            "Không thể tạo hình mã QR"
                    }
                }
            }
        }
    }


    companion object {
        const val EXTRA_MEETING_ID = "extra_meeting_id"
    }
}

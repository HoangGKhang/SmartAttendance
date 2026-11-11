
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

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.SystemClock

import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

import java.util.Locale


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

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var tvGpsInfo: TextView
    private lateinit var btnGetLocation: Button

    private var selectedGpsLocation: Location? = null

    private var locationCancellation: CancellationTokenSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)
        setContentView(R.layout.activity_attendance_session)

        tvGpsInfo = findViewById(R.id.tvGpsInfo)
        btnGetLocation = findViewById(R.id.btnGetLocation)

        btnGetLocation.setOnClickListener {
            checkPermissionAndGetLocation()
        }


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

            // 1. Giữ nguyên kiểm tra trạng thái Session.
            if (
                state.isLoading ||
                !state.isInitialized ||
                state.session != null ||
                state.errorMessage != null
            ) {
                return@setOnClickListener
            }

            // 2. Kiểm tra quyền vị trí vẫn còn hiệu lực.
            val fineGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!fineGranted) {
                selectedGpsLocation = null
                tvGpsInfo.text =
                    "Cần cấp quyền vị trí chính xác trước khi mở phiên."
                return@setOnClickListener
            }

            // 3. Phải lấy được GPS trước.
            val location = selectedGpsLocation

            if (location == null) {
                tvGpsInfo.text =
                    "Vui lòng nhấn Lấy vị trí hiện tại trước."
                return@setOnClickListener
            }

            // 4. Kiểm tra độ chính xác.
            if (
                !location.hasAccuracy() ||
                !location.accuracy.isFinite() ||
                location.accuracy < 0f ||
                location.accuracy > 30f
            ) {
                selectedGpsLocation = null

                tvGpsInfo.text =
                    "GPS chưa đủ chính xác (yêu cầu ≤ 30 m). " +
                            "Vui lòng lấy lại vị trí."

                return@setOnClickListener
            }

            // 5. Kiểm tra tọa độ có hợp lệ.
            if (
                !location.latitude.isFinite() ||
                !location.longitude.isFinite() ||
                location.latitude !in -90.0..90.0 ||
                location.longitude !in -180.0..180.0
            ) {
                selectedGpsLocation = null
                tvGpsInfo.text = "Tọa độ GPS không hợp lệ."
                return@setOnClickListener
            }

            // 6. Kiểm tra vị trí có quá cũ không.
            val ageMs =
                (SystemClock.elapsedRealtimeNanos() -
                        location.elapsedRealtimeNanos) / 1_000_000L

            if (ageMs < 0L || ageMs > 20_000L) {
                selectedGpsLocation = null

                tvGpsInfo.text =
                    "Vị trí GPS đã cũ. Vui lòng lấy lại vị trí."

                return@setOnClickListener
            }

            // 7. Bán kính mặc định cho bản demo.
            val radiusMeters = 50

            // 8. Gửi dữ liệu đã kiểm tra vào ViewModel.
            viewModel.createSession(
                meetingId = id,
                durationMinutes = 3,
                latitude = location.latitude,
                longitude = location.longitude,
                radiusMeters = radiusMeters,
                accuracyMeters = location.accuracy.toDouble()
            )
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

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

            if (fineGranted) {
                fetchLecturerLocation()
            } else {
                selectedGpsLocation = null
                tvGpsInfo.text =
                    "Cần cấp quyền vị trí chính xác để lấy GPS."
            }
        }

    private fun checkPermissionAndGetLocation() {

        val fineGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted) {

            // Đã có quyền: lấy vị trí.
            fetchLecturerLocation()

        } else {

            // Chưa có quyền: yêu cầu Android hiển thị dialog.
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }


    @SuppressLint("MissingPermission")
    private fun fetchLecturerLocation() {

        // 1. Kiểm tra quyền một lần nữa.
        val fineGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted) {
            selectedGpsLocation = null
            tvGpsInfo.text = "Chưa có quyền vị trí chính xác."
            return
        }

        // 2. Khóa nút trong lúc lấy vị trí.
        btnGetLocation.isEnabled = false
        tvGpsInfo.text = "Đang lấy vị trí GPS..."

        selectedGpsLocation = null

        // 3. Hủy yêu cầu trước nếu còn.
        locationCancellation?.cancel()

        val cancellationSource = CancellationTokenSource()
        locationCancellation = cancellationSource

        // 4. Cấu hình lấy vị trí.
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(0L)
            .setDurationMillis(20_000L)
            .build()

        // 5. Gọi Google Play services Location.
        try {
            fusedLocationClient.getCurrentLocation(
                request,
                cancellationSource.token
            )
                .addOnSuccessListener(this) { location ->

                    btnGetLocation.isEnabled = true

                    if (location == null) {
                        tvGpsInfo.text =
                            "Không lấy được vị trí. Hãy bật vị trí và thử lại."
                        return@addOnSuccessListener
                    }

                    // 6. Kiểm tra độ mới của dữ liệu.
                    val ageMs =
                        (SystemClock.elapsedRealtimeNanos() -
                                location.elapsedRealtimeNanos) / 1_000_000L

                    if (ageMs < 0L || ageMs > 20_000L) {
                        tvGpsInfo.text =
                            "Dữ liệu vị trí đã cũ. Vui lòng thử lại."
                        return@addOnSuccessListener
                    }

                    if (!location.hasAccuracy()) {
                        tvGpsInfo.text =
                            "Không xác định được độ chính xác vị trí."
                        return@addOnSuccessListener
                    }

                    selectedGpsLocation = location

                    // 7. Hiển thị thông tin.
                    val accuracyWarning =
                        if (location.accuracy > 30f) {
                            "\nCảnh báo: Độ chính xác còn thấp."
                        } else {
                            ""
                        }

                    tvGpsInfo.text = String.format(
                        Locale.US,
                        "Vĩ độ: %.6f\nKinh độ: %.6f\nĐộ chính xác: ±%.1f m%s",
                        location.latitude,
                        location.longitude,
                        location.accuracy,
                        accuracyWarning
                    )
                }
                .addOnFailureListener(this) {

                    btnGetLocation.isEnabled = true
                    selectedGpsLocation = null

                    tvGpsInfo.text =
                        "Lấy vị trí thất bại. Vui lòng thử lại."
                }

        } catch (e: SecurityException) {

            btnGetLocation.isEnabled = true
            selectedGpsLocation = null

            tvGpsInfo.text =
                "Quyền truy cập vị trí không còn hợp lệ."
        }
    }


    override fun onDestroy() {
        locationCancellation?.cancel()
        locationCancellation = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_MEETING_ID = "extra_meeting_id"
    }
}

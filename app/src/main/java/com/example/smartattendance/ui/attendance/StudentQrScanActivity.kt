
package com.example.smartattendance.ui.attendance

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat

import com.example.smartattendance.databinding.ActivityStudentQrScanBinding

import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage

import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean


import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

import com.example.smartattendance.data.repository.VerifyQrFailureReason


import android.annotation.SuppressLint
import android.location.Location
import android.os.SystemClock

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

import java.util.Locale

import android.content.Intent
import android.provider.Settings

import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay


class StudentQrScanActivity : AppCompatActivity() {

    private lateinit var binding:
            ActivityStudentQrScanBinding

    // Thread riêng dành cho phân tích khung hình.
    private lateinit var cameraExecutor: ExecutorService

    // ImageAnalysis hiện tại.
    private var imageAnalysis: ImageAnalysis? = null

    // Ngăn xử lý một QR nhiều lần.
    private val scanCompleted = AtomicBoolean(false)


    private val scanViewModel:
            StudentQrScanViewModel by viewModels()

    private val gpsViewModel:
            StudentGpsViewModel by viewModels()

    private var pendingQrSessionId: String? = null
    private var verifiedQrSessionId: String? = null


    private var selectedMeetingId: String? = null

    private lateinit var fusedLocationClient:
            FusedLocationProviderClient

    private var studentGpsLocation: Location? = null

    private var studentLocationCallback: LocationCallback? = null
    private var studentLocationTimeout: Job? = null

    // Đánh dấu cần tiếp tục lấy GPS khi quay lại màn hình.
    private var resumeGpsAfterInterruption = false

    // Chỉ quét mã QR, không quét các loại barcode khác.
    private val barcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE
            )
            .build()
    )

    companion object {
        const val EXTRA_MEETING_ID =
            "extra_meeting_id"
    }


    // 1. Xử lý kết quả xin quyền Camera.
    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                startCamera()
            } else {
                binding.tvScanStatus.text =
                    "Cần cấp quyền Camera để quét QR"

                binding.tvCameraPlaceholder.text =
                    "Chưa được cấp quyền Camera"
            }
        }


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityStudentQrScanBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)


        // 2. Kiểm tra Meeting ID.
        selectedMeetingId =
            intent.getStringExtra(EXTRA_MEETING_ID)

        pendingQrSessionId =
            savedInstanceState?.getString("pending_qr_session_id")

        verifiedQrSessionId =
            savedInstanceState?.getString("verified_qr_session_id")

        resumeGpsAfterInterruption =
            savedInstanceState?.getBoolean(
                "resume_gps_after_interruption",
                false
            ) ?: false

        if (selectedMeetingId.isNullOrBlank()) {
            finish()
            return
        }

        // Đồng bộ khóa quét khi Activity được tạo lại,
        // ví dụ khi xoay màn hình.
        scanCompleted.set(
            scanViewModel.uiState.value.status !=
                    StudentQrScanStatus.SCANNING
        )


        // 3. Chuẩn bị thread phân tích Camera.
        cameraExecutor =
            Executors.newSingleThreadExecutor()

        binding.tvScanStatus.text =
            "Đang chuẩn bị Camera..."

        // 4. Nút quay lại.
        binding.btnCancelScan.setOnClickListener {
            finish()
        }



        binding.btnRetryScan.setOnClickListener {

            // 1. Kiểm tra có được phép retry không.
            val canRetry = scanViewModel.resetForRetry(
                allowAfterSuccess = gpsRequiresNewQr(gpsViewModel.uiState.value)
            )

            if (!canRetry) {
                return@setOnClickListener
            }

            // 2. Mở khóa nhận diện QR.
            scanCompleted.set(false)

            // 3. Đăng ký lại Analyzer.
            // Camera Preview không cần khởi động lại.
            if (
                ::cameraExecutor.isInitialized &&
                !cameraExecutor.isShutdown
            ) {
                imageAnalysis?.setAnalyzer(
                    cameraExecutor
                ) { imageProxy ->
                    analyzeImage(imageProxy)
                }
            }
        }

        binding.btnRetryStudentGps.setOnClickListener {
            if (
                studentLocationCallback != null ||
                gpsViewModel.uiState.value.status == StudentGpsStatus.VERIFYING
            ) {
                return@setOnClickListener
            }

            startGpsVerificationAfterQr()
        }

        observeScanState()
        observeGpsState()

        // 5. Kiểm tra và yêu cầu quyền Camera.
        ensureCameraPermission()
    }



    private fun extractSessionIdFromQr(
        qrToken: String
    ): String? {

        val parts = qrToken.split('.')

        if (
            parts.size != 4 ||
            parts[0] != "sa1"
        ) {
            return null
        }

        val uuidPattern = Regex(
            "^[0-9a-fA-F]{8}-" +
                    "[0-9a-fA-F]{4}-" +
                    "[0-9a-fA-F]{4}-" +
                    "[0-9a-fA-F]{4}-" +
                    "[0-9a-fA-F]{12}$"
        )

        return parts[1].takeIf {
            uuidPattern.matches(it)
        }
    }

    private val studentLocationSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (scanViewModel.uiState.value.status != StudentQrScanStatus.SUCCESS) {
                return@registerForActivityResult
            }

            if (isAttendanceLocationEnabled()) {
                startGpsVerificationAfterQr()
            } else {
                binding.tvStudentGpsStatus.text =
                    "Vị trí đang tắt. Bạn có thể bấm Thử lại vị trí."
            }
        }

    private val studentLocationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineGranted =
                permissions[
                    Manifest.permission.ACCESS_FINE_LOCATION
                ] == true

            if (
                fineGranted &&
                scanViewModel.uiState.value.status ==
                StudentQrScanStatus.SUCCESS
            ) {
                fetchStudentLocation()
            } else {
                studentGpsLocation = null

                binding.tvStudentGpsStatus.text =
                    "Chưa được cấp quyền vị trí chính xác."
            }
        }


    /**
     * Kiểm tra quyền Camera.
     */
    private fun ensureCameraPermission() {

        val permission = Manifest.permission.CAMERA

        val granted = ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(permission)
        }
    }


    /**
     * Khởi động CameraX.
     */
    private fun startCamera() {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            if (isFinishing || isDestroyed) {
                return@addListener
            }

            try {
                val cameraProvider =
                    cameraProviderFuture.get()

                // 1. Tạo Preview.
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider =
                            binding.previewView.surfaceProvider
                    }

                // 2. Tạo ImageAnalysis.
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(
                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                    )
                    .build()

                // 3. Đăng ký hàm phân tích frame.
                if (
                    scanViewModel.uiState.value.status ==
                    StudentQrScanStatus.SCANNING
                ) {
                    scanCompleted.set(false)

                    analysis.setAnalyzer(
                        cameraExecutor
                    ) { imageProxy ->
                        analyzeImage(imageProxy)
                    }

                } else {
                    // Không phân tích ảnh khi đang xác minh,
                    // đã thành công hoặc đang hiển thị lỗi.
                    scanCompleted.set(true)
                }


                // 4. Bỏ các Use Case cũ.
                cameraProvider.unbindAll()

                // 5. Gắn Camera vào Activity.
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )

                imageAnalysis = analysis

                // 6. Cập nhật giao diện.
                binding.tvCameraPlaceholder.visibility =
                    View.GONE


                if (
                    scanViewModel.uiState.value.status ==
                    StudentQrScanStatus.SCANNING
                ) {
                    binding.tvScanStatus.text =
                        "Đưa mã QR vào vùng Camera"
                }


            } catch (e: Exception) {

                binding.tvScanStatus.text =
                    "Không thể khởi động Camera"

                binding.tvCameraPlaceholder.visibility =
                    View.VISIBLE
            }

        }, ContextCompat.getMainExecutor(this))
    }


    /**
     * Phân tích mỗi frame từ CameraX.
     */
    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun analyzeImage(
        imageProxy: ImageProxy
    ) {

        // 1. Đã có QR rồi thì bỏ qua frame mới.
        if (scanCompleted.get()) {
            imageProxy.close()
            return
        }

        // 2. Lấy hình ảnh thực tế.
        val mediaImage = imageProxy.image

        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        try {

            // 3. Chuyển Camera frame sang ML Kit InputImage.
            val inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            // 4. Yêu cầu ML Kit phân tích.
            barcodeScanner.process(inputImage)

                .addOnSuccessListener(this) { barcodes ->

                    if (scanCompleted.get()) {
                        return@addOnSuccessListener
                    }

                    // 5. Duyệt QR được phát hiện.
                    for (barcode in barcodes) {

                        val qrToken =
                            barcode.rawValue ?: continue

                        // Lọc sơ bộ QR SmartAttendance.
                        // Đây chưa phải xác minh bảo mật.
                        if (
                            !qrToken.startsWith("sa1.") ||
                            qrToken.length > 256
                        ) {
                            continue
                        }

                        // 6. Chỉ cho phép xử lý đúng một lần.
                        if (
                            scanCompleted.compareAndSet(
                                false,
                                true
                            )
                        ) {
                            onQrDetected(qrToken)
                        }

                        break
                    }
                }

                .addOnFailureListener(this) {
                    // Lỗi phân tích một frame không có
                    // nghĩa phải đóng Camera.
                    // Camera sẽ thử frame tiếp theo.
                }

                .addOnCompleteListener {
                    // 7. Luôn giải phóng frame.
                    imageProxy.close()
                }

        } catch (e: Exception) {

            // Nếu lỗi trước khi ML Kit nhận frame,
            // vẫn phải giải phóng ImageProxy.
            imageProxy.close()
        }
    }



    private fun observeScanState() {

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {

                scanViewModel.uiState.collect { state ->

                    if (state.status != StudentQrScanStatus.SUCCESS) {
                        stopStudentLocationUpdates()
                        studentGpsLocation = null
                        binding.tvStudentGpsStatus.text =
                            "Chưa xác minh vị trí"
                    }

                    when (state.status) {


                        StudentQrScanStatus.SUCCESS -> {
                            val sessionId = state.result?.sessionId
                            if (!sessionId.isNullOrBlank() && verifiedQrSessionId != sessionId) {
                                verifiedQrSessionId = sessionId
                                startGpsVerificationAfterQr()
                            }
                        }

                        StudentQrScanStatus.SCANNING,
                        StudentQrScanStatus.ERROR -> {

                            pendingQrSessionId = null
                            verifiedQrSessionId = null

                            stopStudentLocationUpdates()
                            studentGpsLocation = null

                            gpsViewModel.resetGpsVerification()
                        }

                        StudentQrScanStatus.VERIFYING -> {

                            // Giữ pendingQrSessionId để chờ kết quả.
                            verifiedQrSessionId = null
                            gpsViewModel.resetGpsVerification()
                        }
                    }

                    val canVerifyGps =
                        state.status == StudentQrScanStatus.SUCCESS &&
                                !verifiedQrSessionId.isNullOrBlank()


                    binding.tvStudentGpsStatus.visibility =
                        if (canVerifyGps) View.VISIBLE else View.GONE

                    binding.btnRetryStudentGps.visibility =
                        if (canVerifyGps) View.VISIBLE else View.GONE
                    when (state.status) {

                        StudentQrScanStatus.SCANNING -> {

                            binding.tvScanStatus.text =
                                "Đưa Camera vào mã QR của giảng viên"

                            binding.btnRetryScan.visibility =
                                View.GONE
                        }

                        StudentQrScanStatus.VERIFYING -> {

                            binding.tvScanStatus.text =
                                "Đang xác minh QR với máy chủ..."

                            binding.btnRetryScan.visibility =
                                View.GONE
                        }

                        StudentQrScanStatus.SUCCESS -> {


                            binding.tvScanStatus.text =
                                "QR hợp lệ!\n" +
                                        "Đã xác minh với máy chủ.\n" +
                                        "Chưa ghi nhận điểm danh PRESENT."

                            binding.btnRetryScan.visibility =
                                View.GONE
                        }


                        StudentQrScanStatus.ERROR -> {

                            val reason = state.failureReason

                            binding.tvScanStatus.text =
                                when (reason) {

                                    VerifyQrFailureReason.QR_EXPIRED ->
                                        "Mã QR đã hết hạn. " +
                                                "Vui lòng quét mã mới."

                                    VerifyQrFailureReason.INVALID_QR ->
                                        "Mã QR không hợp lệ."

                                    VerifyQrFailureReason.WRONG_MEETING ->
                                        "Mã QR không thuộc buổi học này."

                                    VerifyQrFailureReason.NOT_ENROLLED ->
                                        "Bạn không có quyền " +
                                                "điểm danh trong lớp này."

                                    VerifyQrFailureReason.LOGIN_REQUIRED ->
                                        "Phiên đăng nhập không hợp lệ. " +
                                                "Vui lòng đăng nhập lại."

                                    VerifyQrFailureReason.SESSION_UNAVAILABLE ->
                                        "Phiên điểm danh đã đóng " +
                                                "hoặc không khả dụng."

                                    else ->
                                        "Không thể xác minh QR. " +
                                                "Vui lòng thử lại."
                                }

                            // Lỗi đăng nhập và phân quyền
                            // không thể sửa bằng cách quét lại.
                            val canRetry = reason != VerifyQrFailureReason.LOGIN_REQUIRED

                            binding.btnRetryScan.visibility =
                                if (canRetry) View.VISIBLE
                                else View.GONE
                        }
                    }
                }
            }
        }
    }



    private fun observeGpsState() {

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {

                gpsViewModel.uiState.collect { state ->

                    // Không xử lý khi chưa gửi GPS.
                    if (state.status == StudentGpsStatus.IDLE) {
                        return@collect
                    }

                    // Kết quả phải thuộc đúng Session QR.
                    val sameSession =
                        state.sessionId == verifiedQrSessionId &&
                                !verifiedQrSessionId.isNullOrBlank()

                    if (
                        !sameSession ||
                        scanViewModel.uiState.value.status !=
                        StudentQrScanStatus.SUCCESS
                    ) {
                        return@collect
                    }

                    when (state.status) {

                        StudentGpsStatus.IDLE -> Unit

                        StudentGpsStatus.VERIFYING -> {
                            binding.tvStudentGpsStatus.text =
                                "Đang xác minh GPS với Backend..."
                        }

                        StudentGpsStatus.PASS -> {
                            binding.tvStudentGpsStatus.text =
                                "GPS PASS\n" +
                                        "Bạn nằm trong vùng điểm danh.\n" +
                                        "Chưa ghi nhận PRESENT."
                        }

                        StudentGpsStatus.FAIL -> {

                            binding.tvStudentGpsStatus.text =
                                "GPS FAIL\n" +
                                        "Bạn nằm ngoài vùng điểm danh."
                        }

                        StudentGpsStatus.ERROR -> {
                            val needsNewQr = gpsRequiresNewQr(state)
                            binding.btnRetryScan.visibility =
                                if (needsNewQr) View.VISIBLE else View.GONE
                            binding.btnRetryStudentGps.visibility =
                                if (needsNewQr) View.GONE else View.VISIBLE

                            binding.tvStudentGpsStatus.text =
                                state.errorMessage
                                    ?: "Không thể xác minh GPS."
                        }

                    }
                }
            }
        }
    }


    private fun submitStudentGps(location: Location) {

        // 1. QR phải PASS.
        if (
            scanViewModel.uiState.value.status !=
            StudentQrScanStatus.SUCCESS
        ) {
            return
        }

        // 2. Lấy Meeting và Session đã xác minh.
        val result = scanViewModel.uiState.value.result
        val meetingId = result?.meetingId
        val sessionId = result?.sessionId

        if (
            meetingId.isNullOrBlank() ||
            sessionId.isNullOrBlank()
        ) {
            binding.tvStudentGpsStatus.text =
                "Thiếu thông tin Session. Hãy quét lại QR."
            return
        }

        // 3. Kiểm tra tọa độ.
        if (
            !location.latitude.isFinite() ||
            !location.longitude.isFinite() ||
            location.latitude !in -90.0..90.0 ||
            location.longitude !in -180.0..180.0
        ) {
            binding.tvStudentGpsStatus.text =
                "Tọa độ GPS không hợp lệ."
            return
        }

        // 4. Kiểm tra accuracy.
        if (
            !location.hasAccuracy() ||
            !location.accuracy.isFinite() ||
            location.accuracy !in 0f..30f
        ) {
            binding.tvStudentGpsStatus.text =
                "Độ chính xác GPS không hợp lệ."
            return
        }

        // 5. Kiểm tra vị trí còn mới.
        val ageMs =
            (SystemClock.elapsedRealtimeNanos() -
                    location.elapsedRealtimeNanos) / 1_000_000L

        if (ageMs < 0L || ageMs > 20_000L) {
            binding.tvStudentGpsStatus.text =
                "Vị trí GPS đã cũ."
            return
        }

        // 6. Gọi ViewModel để xác minh trên Backend.
        gpsViewModel.verifyGps(
            meetingId = meetingId,
            sessionId = sessionId,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy.toDouble()
        )
    }


    private fun gpsRequiresNewQr(state: StudentGpsUiState): Boolean =
        state.status == StudentGpsStatus.ERROR &&
                (state.errorCode == "SESSION_NOT_FOUND" ||
                        state.errorCode == "SESSION_UNAVAILABLE")

    private fun startGpsVerificationAfterQr() {

        // 1. Chỉ tiếp tục khi QR đã được Backend xác minh.
        if (
            scanViewModel.uiState.value.status !=
            StudentQrScanStatus.SUCCESS
        ) {
            return
        }

        // 2. Phải có Session ID tương ứng với QR hợp lệ.
        if (verifiedQrSessionId.isNullOrBlank()) {
            binding.tvStudentGpsStatus.text =
                "Không xác định được Session. Vui lòng quét lại QR."
            return
        }

        // 3. Kiểm tra quyền vị trí chính xác.
        val fineGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted) {

            // Đã có quyền: lấy GPS ngay.
            fetchStudentLocation()

        } else {

            // Chưa có quyền: để Android hỏi Allow / Deny.
            binding.tvStudentGpsStatus.text =
                "Đang yêu cầu quyền vị trí chính xác..."

            studentLocationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun stopStudentLocationUpdates() {
        val callback = studentLocationCallback
        studentLocationCallback = null

        studentLocationTimeout?.cancel()
        studentLocationTimeout = null

        if (callback != null) {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchStudentLocation() {

        // 1. Chỉ lấy GPS sau QR PASS.
        if (
            scanViewModel.uiState.value.status !=
            StudentQrScanStatus.SUCCESS
        ) {
            return
        }

        // 2. Kiểm tra lại quyền.
        val fineGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted) {
            binding.tvStudentGpsStatus.text =
                "Cần cấp quyền vị trí chính xác."
            return
        }

        if (!isAttendanceLocationEnabled()) {
            showEnableAttendanceLocationDialog(
                message = "Bật vị trí để kiểm tra bạn có ở trong khu vực điểm danh.",
                onEnable = {
                    studentLocationSettingsLauncher.launch(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    )
                },
                onLater = {
                    binding.tvStudentGpsStatus.text = "Chưa xác minh vị trí."
                }
            )
            return
        }

        if (studentLocationCallback != null) return

        binding.tvStudentGpsStatus.text = "Đang lấy vị trí của bạn..."

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1_000L
        )
            .setMaxUpdateAgeMillis(10_000L)
            .setDurationMillis(20_000L)
            .build()

        var bestAccuracy: Float? = null

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (studentLocationCallback !== this) return

                for (location in result.locations) {
                    val ageMs = (SystemClock.elapsedRealtimeNanos() -
                            location.elapsedRealtimeNanos) / 1_000_000L

                    if (
                        ageMs !in 0L..20_000L ||
                        !location.hasAccuracy() ||
                        !location.accuracy.isFinite() ||
                        location.accuracy < 0f ||
                        location.latitude !in -90.0..90.0 ||
                        location.longitude !in -180.0..180.0
                    ) {
                        continue
                    }

                    val accuracy = location.accuracy
                    bestAccuracy = minOf(bestAccuracy ?: accuracy, accuracy)

                    if (accuracy <= 30f) {
                        stopStudentLocationUpdates()
                        submitStudentGps(location)
                        return
                    }
                }
            }
        }

        studentLocationCallback = callback
        studentLocationTimeout = lifecycleScope.launch {
            delay(20_000L)
            if (studentLocationCallback !== callback) return@launch

            stopStudentLocationUpdates()

            binding.tvStudentGpsStatus.text = bestAccuracy?.let { accuracy ->
                val meters = String.format(Locale.US, "%.1f", accuracy)
                "GPS tốt nhất ±$meters m, yêu cầu ≤ 30 m. Hãy thử lại."
            } ?: "Chưa lấy được vị trí sau 20 giây. Hãy thử lại."
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                callback,
                Looper.getMainLooper()
            ).addOnFailureListener { error ->
                if (studentLocationCallback === callback) {
                    stopStudentLocationUpdates()
                    binding.tvStudentGpsStatus.text =
                        if (error is SecurityException) {
                            "Không còn quyền truy cập vị trí."
                        } else {
                            "Lấy vị trí thất bại. Hãy thử lại."
                        }
                }
            }
        } catch (e: SecurityException) {
            stopStudentLocationUpdates()
            binding.tvStudentGpsStatus.text = "Không còn quyền truy cập vị trí."
        }
    }


    /**
     * Xử lý QR vừa đọc được.
     * Bước 7.6.3 sẽ gọi Repository tại đây.
     */

    private fun onQrDetected(
        qrToken: String
    ) {

        // 1. Dừng phân tích frame.
        imageAnalysis?.clearAnalyzer()

        // 2. Meeting sinh viên đang chọn.
        val meetingId = selectedMeetingId

        if (meetingId.isNullOrBlank()) {
            binding.tvScanStatus.text =
                "Không tìm thấy buổi học"
            return
        }

        // ID mới đọc từ QR, chưa được tin cậy.
        pendingQrSessionId =
            extractSessionIdFromQr(qrToken)

        // Không tái sử dụng kết quả của lượt trước.
        verifiedQrSessionId = null

        stopStudentLocationUpdates()
        studentGpsLocation = null

        gpsViewModel.resetGpsVerification()


        // 3. Chuyển QR Token sang ViewModel.
        scanViewModel.verifyQr(
            qrToken = qrToken,
            meetingId = meetingId
        )
    }


    override fun onSaveInstanceState(outState: Bundle) {

        outState.putString(
            "pending_qr_session_id",
            pendingQrSessionId
        )

        outState.putString(
            "verified_qr_session_id",
            verifiedQrSessionId
        )

        val gpsWasInterruptedOrRunning =
            resumeGpsAfterInterruption ||
                    studentLocationCallback != null

        val shouldResumeGps =
            scanViewModel.uiState.value.status ==
                    StudentQrScanStatus.SUCCESS &&
                    !verifiedQrSessionId.isNullOrBlank() &&
                    gpsViewModel.uiState.value.status ==
                    StudentGpsStatus.IDLE &&
                    gpsWasInterruptedOrRunning

        outState.putBoolean(
            "resume_gps_after_interruption",
            shouldResumeGps
        )

        super.onSaveInstanceState(outState)
    }


    override fun onResume() {
        super.onResume()

        // Không có GPS nào bị gián đoạn.
        if (!resumeGpsAfterInterruption) {
            return
        }

        // Xóa cờ trước khi tiếp tục, tránh lặp yêu cầu.
        resumeGpsAfterInterruption = false

        // QR vẫn phải ở trạng thái SUCCESS.
        if (
            scanViewModel.uiState.value.status !=
            StudentQrScanStatus.SUCCESS ||
            verifiedQrSessionId.isNullOrBlank()
        ) {
            return
        }

        // Nếu Backend đã nhận request GPS,
        // không tạo thêm request vị trí.
        if (
            gpsViewModel.uiState.value.status !=
            StudentGpsStatus.IDLE
        ) {
            return
        }

        // Chỉ tự tiếp tục khi đã có quyền vị trí chính xác.
        val fineGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted) {

            binding.tvStudentGpsStatus.text =
                "Đang tiếp tục xác minh vị trí GPS..."

            fetchStudentLocation()

        } else {

            // Không tự hiện lại hộp thoại quyền liên tục.
            binding.tvStudentGpsStatus.text =
                "Cần cấp quyền vị trí chính xác để tiếp tục điểm danh."
        }
    }



    override fun onStop() {

        // Chỉ đánh dấu tiếp tục nếu GPS thực sự đang được lấy,
        // QR vẫn hợp lệ và chưa có kết quả Backend.
        if (
            studentLocationCallback != null &&
            scanViewModel.uiState.value.status ==
            StudentQrScanStatus.SUCCESS &&
            !verifiedQrSessionId.isNullOrBlank() &&
            gpsViewModel.uiState.value.status ==
            StudentGpsStatus.IDLE
        ) {
            resumeGpsAfterInterruption = true
        }

        // Hủy yêu cầu GPS của Activity đang dừng.
        stopStudentLocationUpdates()

        super.onStop()
    }


    override fun onDestroy() {

        imageAnalysis?.clearAnalyzer()

        // Giải phóng ML Kit.
        barcodeScanner.close()

        // Kết thúc thread Camera.
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }

        super.onDestroy()
    }
}

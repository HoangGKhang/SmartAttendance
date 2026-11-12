
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

    private var locationCancellation:
            CancellationTokenSource? = null


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
            val canRetry = scanViewModel.resetForRetry()

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


        binding.btnVerifyStudentGps.setOnClickListener {

            // 1. QR phải PASS trước.
            if (
                scanViewModel.uiState.value.status !=
                StudentQrScanStatus.SUCCESS
            ) {
                return@setOnClickListener
            }

            if (verifiedQrSessionId.isNullOrBlank()) {
                binding.tvStudentGpsStatus.text =
                    "Không xác định được Session. Vui lòng quét lại QR."
                return@setOnClickListener
            }

            if (
                gpsViewModel.uiState.value.status ==
                StudentGpsStatus.VERIFYING
            ) {
                return@setOnClickListener
            }

            // 2. Kiểm tra quyền vị trí chính xác.
            val fineGranted =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

            if (fineGranted) {

                // Đã có quyền: lấy GPS.
                fetchStudentLocation()

            } else {

                // Chưa có quyền: hỏi người dùng.
                studentLocationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
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
                        locationCancellation?.cancel()
                        locationCancellation = null
                        studentGpsLocation = null
                        binding.btnVerifyStudentGps.isEnabled = true
                        binding.tvStudentGpsStatus.text =
                            "Chưa xác minh vị trí"
                    }

                    when (state.status) {

                        StudentQrScanStatus.SUCCESS -> {

                            if (pendingQrSessionId != null) {
                                verifiedQrSessionId = pendingQrSessionId
                                pendingQrSessionId = null
                            }
                        }

                        StudentQrScanStatus.SCANNING,
                        StudentQrScanStatus.ERROR -> {

                            pendingQrSessionId = null
                            verifiedQrSessionId = null

                            locationCancellation?.cancel()
                            locationCancellation = null
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

                    binding.btnVerifyStudentGps.visibility =
                        if (canVerifyGps) View.VISIBLE else View.GONE

                    binding.tvStudentGpsStatus.visibility =
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
                            binding.btnVerifyStudentGps.isEnabled = false

                            binding.tvStudentGpsStatus.text =
                                "Đang xác minh GPS với Backend..."
                        }

                        StudentGpsStatus.PASS -> {
                            binding.btnVerifyStudentGps.isEnabled = true

                            binding.tvStudentGpsStatus.text =
                                "GPS PASS\n" +
                                        "Bạn nằm trong vùng điểm danh.\n" +
                                        "Chưa ghi nhận PRESENT."
                        }

                        StudentGpsStatus.FAIL -> {
                            binding.btnVerifyStudentGps.isEnabled = true

                            binding.tvStudentGpsStatus.text =
                                "GPS FAIL\n" +
                                        "Bạn nằm ngoài vùng điểm danh."
                        }

                        StudentGpsStatus.ERROR -> {
                            binding.btnVerifyStudentGps.isEnabled = true

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
        val meetingId = selectedMeetingId
        val sessionId = verifiedQrSessionId

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

        // 3. Khóa nút khi đang lấy GPS.
        binding.btnVerifyStudentGps.isEnabled = false
        binding.tvStudentGpsStatus.text =
            "Đang lấy vị trí của bạn..."

        studentGpsLocation = null

        // 4. Hủy request trước nếu tồn tại.
        locationCancellation?.cancel()

        val source = CancellationTokenSource()
        locationCancellation = source

        // 5. Yêu cầu vị trí mới.
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(0L)
            .setDurationMillis(20_000L)
            .build()

        try {
            fusedLocationClient.getCurrentLocation(
                request,
                source.token
            )
                .addOnSuccessListener(this) { location ->

                    // Bỏ qua kết quả từ request đã hủy.
                    if (locationCancellation !== source) {
                        return@addOnSuccessListener
                    }

                    locationCancellation = null
                    binding.btnVerifyStudentGps.isEnabled = true

                    // QR phải vẫn ở trạng thái SUCCESS.
                    if (
                        scanViewModel.uiState.value.status !=
                        StudentQrScanStatus.SUCCESS
                    ) {
                        studentGpsLocation = null
                        return@addOnSuccessListener
                    }

                    // 6. Không nhận được GPS.
                    if (location == null) {
                        binding.tvStudentGpsStatus.text =
                            "Không lấy được vị trí. Vui lòng thử lại."
                        return@addOnSuccessListener
                    }

                    // 7. Kiểm tra tuổi dữ liệu vị trí.
                    val ageMs =
                        (SystemClock.elapsedRealtimeNanos() -
                                location.elapsedRealtimeNanos) / 1_000_000L

                    if (ageMs < 0L || ageMs > 20_000L) {
                        binding.tvStudentGpsStatus.text =
                            "Vị trí GPS đã cũ. Hãy lấy lại."
                        return@addOnSuccessListener
                    }

                    // 8. Kiểm tra độ chính xác.
                    if (
                        !location.hasAccuracy() ||
                        !location.accuracy.isFinite() ||
                        location.accuracy < 0f ||
                        location.accuracy > 30f
                    ) {
                        binding.tvStudentGpsStatus.text =
                            "GPS chưa đủ chính xác (yêu cầu ≤ 30 m)."
                        return@addOnSuccessListener
                    }

                    // 9. Kiểm tra tọa độ.
                    if (
                        !location.latitude.isFinite() ||
                        !location.longitude.isFinite() ||
                        location.latitude !in -90.0..90.0 ||
                        location.longitude !in -180.0..180.0
                    ) {
                        binding.tvStudentGpsStatus.text =
                            "Tọa độ GPS không hợp lệ."
                        return@addOnSuccessListener
                    }

                    // 10. Lưu vị trí tạm thời.
                    studentGpsLocation = location

                    // 11. Hiển thị thông tin.
                    binding.tvStudentGpsStatus.text =
                        String.format(
                            Locale.US,
                            "Đã lấy GPS (chưa xác minh).\n" +
                                    "Latitude: %.6f\n" +
                                    "Longitude: %.6f\n" +
                                    "Accuracy: ±%.1f m",
                            location.latitude,
                            location.longitude,
                            location.accuracy
                        )

                    // GPS hợp lệ, gửi lên Backend.
                    submitStudentGps(location)
                }
                .addOnFailureListener(this) {

                    if (locationCancellation !== source) {
                        return@addOnFailureListener
                    }

                    locationCancellation = null
                    studentGpsLocation = null
                    binding.btnVerifyStudentGps.isEnabled = true

                    binding.tvStudentGpsStatus.text =
                        "Lấy vị trí thất bại. Vui lòng thử lại."
                }

        } catch (e: SecurityException) {

            if (locationCancellation === source) {
                locationCancellation = null
            }

            studentGpsLocation = null
            binding.btnVerifyStudentGps.isEnabled = true

            binding.tvStudentGpsStatus.text =
                "Không còn quyền truy cập vị trí."
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

        locationCancellation?.cancel()
        locationCancellation = null
        studentGpsLocation = null

        gpsViewModel.resetGpsVerification()


        // 3. Chuyển QR Token sang ViewModel.
        scanViewModel.verifyQr(
            qrToken = qrToken,
            meetingId = meetingId
        )
    }


    override fun onStop() {
        locationCancellation?.cancel()
        locationCancellation = null

        if (::binding.isInitialized) {
            binding.btnVerifyStudentGps.isEnabled = true
        }

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


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

    private var selectedMeetingId: String? = null



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

        observeScanState()

        // 5. Kiểm tra và yêu cầu quyền Camera.
        ensureCameraPermission()
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
    @OptIn(ExperimentalGetImage::class)
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

        // 3. Chuyển QR Token sang ViewModel.
        scanViewModel.verifyQr(
            qrToken = qrToken,
            meetingId = meetingId
        )
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

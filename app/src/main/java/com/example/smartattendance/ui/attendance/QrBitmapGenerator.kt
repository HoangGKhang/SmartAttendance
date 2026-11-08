
package com.example.smartattendance.ui.attendance

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrBitmapGenerator {

    fun generate(
        token: String,
        sizePx: Int = 768
    ): Bitmap {

        require(token.isNotBlank()) {
            "QR token không được rỗng"
        }

        require(sizePx > 0) {
            "Kích thước QR phải lớn hơn 0"
        }

        // 1. Cấu hình QR Code.
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION
                    to ErrorCorrectionLevel.M,

            EncodeHintType.MARGIN to 4
        )

        // 2. Chuyển token thành ma trận QR.
        val matrix = QRCodeWriter().encode(
            token,
            BarcodeFormat.QR_CODE,
            sizePx,
            sizePx,
            hints
        )

        // 3. Đọc kích thước ma trận.
        val width = matrix.width
        val height = matrix.height

        // 4. Tạo mảng điểm ảnh.
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {

                pixels[y * width + x] =
                    if (matrix[x, y]) {
                        Color.BLACK
                    } else {
                        Color.WHITE
                    }
            }
        }

        // 5. Chuyển mảng điểm ảnh thành Bitmap.
        return Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        ).apply {
            setPixels(
                pixels,
                0,
                width,
                0,
                0,
                width,
                height
            )
        }
    }
}

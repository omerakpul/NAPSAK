package com.napsak.app.ui

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/** Renders [content] as a QR code bitmap entirely on device. */
fun qrBitmap(content: String, size: Int = 512): Bitmap {
    val matrix = QRCodeWriter().encode(
        content, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1)
    )
    val pixels = IntArray(size * size) { i ->
        if (matrix[i % size, i / size]) Color.BLACK else Color.WHITE
    }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}

/** Public web page that also opens the Android app via the deep link. */
fun roomLink(roomId: String) = "https://omerakpul.github.io/NAPSAK/?room=$roomId"

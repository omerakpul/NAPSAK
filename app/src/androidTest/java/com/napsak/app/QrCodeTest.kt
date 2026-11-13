package com.napsak.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.napsak.app.ui.qrBitmap
import com.napsak.app.ui.roomLink
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QrCodeTest {

    @Test
    fun qr_code_decodes_back_to_the_room_link() {
        val link = roomLink("123456")
        val bitmap = qrBitmap(link)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val decoded = QRCodeReader().decode(
            BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width, bitmap.height, pixels)))
        )
        assertEquals("https://omerakpul.github.io/NAPSAK/?room=123456", decoded.text)
    }
}

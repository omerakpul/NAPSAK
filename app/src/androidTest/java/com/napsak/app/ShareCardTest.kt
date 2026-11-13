package com.napsak.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.napsak.app.domain.model.Choice
import com.napsak.app.ui.ShareCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ShareCardTest {

    @Test
    fun winner_card_renders_with_content() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val winner = Choice(id = "a", name = "Napoli Pizza & Pasta Evi", details = "Taş fırında İtalyan pizzası, bol malzeme", category = "Yemek")
        val bitmap = ShareCard.render(winner, "4 Oy", 5)
        assertEquals(1080, bitmap.width)
        assertEquals(1350, bitmap.height)

        // not a flat colour: the white card, text and gradient all contribute distinct pixels
        val distinct = HashSet<Int>()
        for (x in 0 until bitmap.width step 37) for (y in 0 until bitmap.height step 37) distinct.add(bitmap.getPixel(x, y))
        assertTrue("card looks blank (${distinct.size} colours)", distinct.size > 20)

        // keep a copy for visual inspection (adb pull)
        val out = File(ctx.getExternalFilesDir(null), "share_card.png")
        out.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
}

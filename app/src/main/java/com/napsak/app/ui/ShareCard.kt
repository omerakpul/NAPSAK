package com.napsak.app.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.napsak.app.domain.model.Choice
import java.io.File
import java.io.FileOutputStream

/** Draws the winner as a shareable image card and hands it to the system share sheet. */
object ShareCard {

    private const val W = 1080
    private const val H = 1350

    private fun paint(size: Float, color: Int, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
    }

    private fun layout(text: String, paint: TextPaint, width: Int, maxLines: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()

    private fun Canvas.drawCentered(layout: StaticLayout, top: Float) {
        save()
        translate((W - layout.width) / 2f, top)
        layout.draw(this)
        restore()
    }

    fun render(winner: Choice, votesText: String, people: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawRect(
            0f, 0f, W.toFloat(), H.toFloat(),
            Paint().apply { shader = LinearGradient(0f, 0f, W.toFloat(), H.toFloat(), 0xFF7C3AED.toInt(), 0xFF4C1D95.toInt(), Shader.TileMode.CLAMP) }
        )

        val header = paint(76f, 0xFFFFFFFF.toInt(), bold = true)
        canvas.drawText("N\u2019APSAK?", (W - header.measureText("N\u2019APSAK?")) / 2f, 140f, header)

        canvas.drawRoundRect(RectF(70f, 200f, W - 70f, 1150f), 64f, 64f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })

        val emoji = CategoryStyle.emoji(winner.name, winner.category)
        val emojiPaint = paint(240f, 0xFF000000.toInt())
        canvas.drawText(emoji, (W - emojiPaint.measureText(emoji)) / 2f, 480f, emojiPaint)

        val nameLayout = layout(winner.name, paint(88f, 0xFF1B1B1F.toInt(), bold = true), 860, 3)
        canvas.drawCentered(nameLayout, 540f)

        var y = 540f + nameLayout.height + 20f
        if (winner.details.isNotBlank()) {
            val detailsLayout = layout(winner.details, paint(44f, 0xFF6B6B76.toInt()), 840, 3)
            canvas.drawCentered(detailsLayout, y)
            y += detailsLayout.height + 20f
        }

        val pill = paint(52f, 0xFF7C3AED.toInt(), bold = true)
        val pillText = votesText
        val pillW = pill.measureText(pillText) + 96f
        val pillTop = minOf(y + 10f, 1020f)
        canvas.drawRoundRect(
            RectF((W - pillW) / 2f, pillTop, (W + pillW) / 2f, pillTop + 96f), 48f, 48f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x1F7C3AED }
        )
        canvas.drawText(pillText, (W - pill.measureText(pillText)) / 2f, pillTop + 66f, pill)

        val footer = paint(46f, 0xFFFFFFFF.toInt(), bold = true)
        val line1 = "Grup kararını verdi! ($people kişi)"
        canvas.drawText(line1, (W - footer.measureText(line1)) / 2f, 1230f, footer)
        val small = paint(38f, 0xCCFFFFFF.toInt())
        val line2 = "omerakpul.github.io/NAPSAK"
        canvas.drawText(line2, (W - small.measureText(line2)) / 2f, 1290f, small)
        return bitmap
    }

    fun share(context: Context, winner: Choice, votesText: String, people: Int) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "napsak-kazanan.png")
        FileOutputStream(file).use { render(winner, votesText, people).compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "N\u2019APSAK? ile karar verdik: ${winner.name} kazandı! ${roomLinkBase}")
            // ClipData lets the chooser grant (and preview) the image
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, null))
    }

    private const val roomLinkBase = "https://omerakpul.github.io/NAPSAK/"
}

package com.napsak.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Default visuals for options without a photo: an emoji from the name/category and a stable accent colour. */
object CategoryStyle {

    private val keywords = listOf(
        "pizza" to "\uD83C\uDF55", "burger" to "\uD83C\uDF54", "hamburger" to "\uD83C\uDF54",
        "kebap" to "\uD83E\uDD59", "döner" to "\uD83E\uDD59", "sushi" to "\uD83C\uDF63",
        "makarna" to "\uD83C\uDF5D", "pasta" to "\uD83C\uDF5D", "kahve" to "\u2615", "coffee" to "\u2615",
        "starbucks" to "\u2615", "çay" to "\uD83C\uDF75", "tatlı" to "\uD83C\uDF70", "pasta" to "\uD83C\uDF70",
        "dondurma" to "\uD83C\uDF66", "balık" to "\uD83D\uDC1F", "salata" to "\uD83E\uDD57",
        "sinema" to "\uD83C\uDFAC", "film" to "\uD83C\uDF7F", "bowling" to "\uD83C\uDFB3",
        "konser" to "\uD83C\uDFB5", "müzik" to "\uD83C\uDFB5", "tiyatro" to "\uD83C\uDFAD",
        "karaoke" to "\uD83C\uDFA4", "bilardo" to "\uD83C\uDFB1", "dart" to "\uD83C\uDFAF",
        "playstation" to "\uD83C\uDFAE", "oyun" to "\uD83C\uDFAE", "kutu oyun" to "\uD83C\uDFB2",
        "park" to "\uD83C\uDF33", "piknik" to "\uD83E\uDDFA", "plaj" to "\uD83C\uDFD6\uFE0F",
        "deniz" to "\uD83C\uDF0A", "dağ" to "\u26F0\uFE0F", "kamp" to "\u26FA", "alışveriş" to "\uD83D\uDECD\uFE0F",
        "futbol" to "\u26BD", "basketbol" to "\uD83C\uDFC0", "yüzme" to "\uD83C\uDFCA", "koşu" to "\uD83C\uDFC3",
        "kitap" to "\uD83D\uDCDA", "bar" to "\uD83C\uDF7B", "bira" to "\uD83C\uDF7B", "restoran" to "\uD83C\uDF7D\uFE0F",
        "ev" to "\uD83C\uDFE0", "tatil" to "\u2708\uFE0F", "bilim kurgu" to "\uD83D\uDE80", "komedi" to "\uD83D\uDE02",
        "korku" to "\uD83D\uDC7B", "aksiyon" to "\uD83D\uDCA5", "romantik" to "\uD83D\uDC95"
    )

    private val categoryEmoji = mapOf(
        "yemek" to "\uD83C\uDF54", "aktivite" to "\uD83C\uDFAC", "film" to "\uD83C\uDF7F",
        "eğlence" to "\uD83C\uDFAE", "kahve" to "\u2615"
    )

    private val fallbackPool = listOf(
        "\uD83C\uDFAF", "\u2B50", "\uD83C\uDFB2", "\uD83C\uDF40", "\uD83C\uDF81", "\uD83D\uDD25", "\uD83C\uDF08", "\uD83C\uDFC8"
    )

    private val accents = listOf(
        Color(0xFF7C3AED), Color(0xFFEC4899), Color(0xFFF59E0B), Color(0xFF10B981),
        Color(0xFF3B82F6), Color(0xFFEF4444), Color(0xFF14B8A6), Color(0xFF8B5CF6)
    )

    private fun hash(key: String): Int = (key.lowercase().hashCode() and Int.MAX_VALUE)

    /** Best emoji for an option: a keyword in its name, then its category, then a stable pick. */
    fun emoji(name: String, category: String = ""): String {
        val n = name.lowercase()
        val tokens = n.split(' ', '-', '/', ',', '.')
        keywords.firstOrNull { (word, _) ->
            if (' ' in word) n.contains(word) else tokens.any { it.startsWith(word) }
        }?.let { return it.second }
        categoryEmoji[category.lowercase()]?.let { return it }
        return fallbackPool[hash(name) % fallbackPool.size]
    }

    /** Stable accent colour for an option (same name -> same colour on every device). */
    fun accent(name: String, category: String = ""): Color =
        accents[hash(category.ifBlank { name }) % accents.size]
}

/** Round tinted avatar with the option's emoji; the default visual when there is no photo. */
@Composable
fun OptionEmoji(name: String, category: String = "", size: Dp = 56.dp, modifier: Modifier = Modifier) {
    val accent = CategoryStyle.accent(name, category)
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(accent.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = CategoryStyle.emoji(name, category), fontSize = (size.value * 0.5f).sp)
    }
}

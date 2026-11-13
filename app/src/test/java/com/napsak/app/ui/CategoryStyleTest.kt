package com.napsak.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryStyleTest {

    @Test
    fun `name keywords win over the category`() {
        assertEquals("\uD83C\uDF55", CategoryStyle.emoji("Napoli Pizza", "Aktivite"))
        assertEquals("\u2615", CategoryStyle.emoji("Starbucks"))
    }

    @Test
    fun `short keywords only match whole words`() {
        // "ev" must not match inside unrelated words
        assertNotEquals("\uD83C\uDFE0", CategoryStyle.emoji("Seven Eleven"))
        assertEquals("\uD83C\uDFE0", CategoryStyle.emoji("Evde takılalım"))
    }

    @Test
    fun `category is the fallback and the last resort is stable`() {
        assertEquals("\uD83C\uDFAE", CategoryStyle.emoji("Bir şey", "Eğlence"))
        val first = CategoryStyle.emoji("Gizemli seçenek")
        assertEquals(first, CategoryStyle.emoji("Gizemli seçenek"))
        assertTrue(first.isNotEmpty())
    }

    @Test
    fun `accent colour is stable per name or category`() {
        assertEquals(CategoryStyle.accent("Pizza"), CategoryStyle.accent("Pizza"))
        assertEquals(CategoryStyle.accent("A", "Yemek"), CategoryStyle.accent("B", "Yemek"))
    }
}

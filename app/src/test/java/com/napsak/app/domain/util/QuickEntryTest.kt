package com.napsak.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickEntryTest {

    @Test
    fun `one option per line, blanks skipped`() {
        val result = parseQuickChoices("Pizza\n\n  Burger  \n")
        assertEquals(listOf("Pizza", "Burger"), result.map { it.name })
        assertTrue(result.all { it.id.isNotBlank() })
    }

    @Test
    fun `bullets and numbers are stripped`() {
        val result = parseQuickChoices("- Pizza\n* Burger\n1. Sushi\n2) Kebap\n\u2022 Salata")
        assertEquals(listOf("Pizza", "Burger", "Sushi", "Kebap", "Salata"), result.map { it.name })
    }

    @Test
    fun `details are split from the name`() {
        val result = parseQuickChoices("Pizza - taş fırın\nBurger | gurme\nSushi: taze")
        assertEquals(listOf("taş fırın", "gurme", "taze"), result.map { it.details })
        assertEquals(listOf("Pizza", "Burger", "Sushi"), result.map { it.name })
    }

    @Test
    fun `hyphens inside a name are kept`() {
        val result = parseQuickChoices("Spider-Man\nCoca-Cola")
        assertEquals(listOf("Spider-Man", "Coca-Cola"), result.map { it.name })
    }

    @Test
    fun `duplicates are ignored case-insensitively and the limit is respected`() {
        assertEquals(1, parseQuickChoices("Pizza\npizza\nPIZZA").size)
        val many = (1..80).joinToString("\n") { "Seçenek $it" }
        assertEquals(50, parseQuickChoices(many).size)
        assertEquals(3, parseQuickChoices(many, limit = 3).size)
    }

    @Test
    fun `category is applied to every option`() {
        assertEquals(listOf("Yemek", "Yemek"), parseQuickChoices("a\nb", category = " Yemek ").map { it.category })
    }
}

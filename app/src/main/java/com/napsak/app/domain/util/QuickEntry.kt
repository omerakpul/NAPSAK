package com.napsak.app.domain.util

import com.napsak.app.domain.model.Choice
import java.util.UUID

private val LEADING_MARK = Regex("^\\s*(?:[-*\u2022\u2013]+|\\d{1,2}[.)])\\s*")
private val DETAIL_SPLIT = Regex("\\s+[-\u2013|]\\s+|\\s*[|:]\\s+")

/**
 * Turns pasted text into options: one option per line. A leading bullet or number is ignored and
 * "Name - details" (or "Name | details") fills the description. Blank lines and duplicate names are skipped.
 */
fun parseQuickChoices(text: String, category: String = "", limit: Int = 50): List<Choice> {
    val seen = HashSet<String>()
    val result = ArrayList<Choice>()
    for (raw in text.lineSequence()) {
        val line = raw.replace(LEADING_MARK, "").trim()
        if (line.isEmpty()) continue
        val parts = line.split(DETAIL_SPLIT, limit = 2)
        val name = parts[0].trim().take(100)
        if (name.isEmpty() || !seen.add(name.lowercase())) continue
        val details = parts.getOrNull(1)?.trim().orEmpty().take(500)
        result += Choice(id = UUID.randomUUID().toString(), name = name, details = details, category = category.trim())
        if (result.size >= limit) break
    }
    return result
}

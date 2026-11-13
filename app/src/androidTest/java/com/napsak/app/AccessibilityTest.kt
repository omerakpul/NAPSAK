package com.napsak.app

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.napsak.app.domain.model.Choice
import com.napsak.app.domain.model.Suggestion
import com.napsak.app.domain.model.VoteMode
import com.napsak.app.ui.OfflineBanner
import com.napsak.app.ui.screens.createchoices.QuickEntryCard
import com.napsak.app.ui.screens.createchoices.StickyStartBar
import com.napsak.app.ui.screens.createchoices.SuggestionsCard
import com.napsak.app.ui.screens.voting.HostWaitPrompt
import com.napsak.app.ui.screens.voting.RankingVoting
import com.napsak.app.ui.screens.voting.SingleChoiceVoting
import com.napsak.app.ui.theme.NAPSAKTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility checks on the reusable components, in light and dark theme and at a 2x font scale:
 * every clickable element must be at least 48dp x 48dp and must have a text or content description
 * that a screen reader can announce.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {

    @get:Rule
    val rule = createComposeRule()

    private val choices = listOf(
        Choice(id = "a", name = "Pizzacı", details = "Taş fırında İtalyan pizzası", category = "Yemek"),
        Choice(id = "b", name = "Burgerci", details = "Gurme hamburgerler", category = "Yemek"),
        Choice(id = "c", name = "Sushi Bar", details = "", category = "")
    )

    private fun check(name: String, fontScale: Float = 1f, dark: Boolean = false, content: @Composable () -> Unit) {
        var density = 1f
        rule.setContent {
            val base = LocalDensity.current
            density = base.density
            androidx.compose.runtime.CompositionLocalProvider(
                LocalDensity provides androidx.compose.ui.unit.Density(base.density, fontScale)
            ) {
                NAPSAKTheme(darkTheme = dark) { Surface { content() } }
            }
        }
        rule.waitForIdle()
        val nodes = rule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        assertTrue("$name: no clickable elements found", nodes.isNotEmpty())
        nodes.forEach { node ->
            val widthDp = node.touchBoundsInRoot.width / density
            val heightDp = node.touchBoundsInRoot.height / density
            assertTrue("$name (dark=$dark, font=$fontScale): touch target is ${widthDp}x${heightDp}dp (< 48dp)",
                widthDp >= 47.5f && heightDp >= 47.5f)
            val labeled = node.config.getOrNull(SemanticsProperties.ContentDescription)?.isNotEmpty() == true ||
                node.config.getOrNull(SemanticsProperties.Text)?.isNotEmpty() == true
            assertTrue("$name (dark=$dark, font=$fontScale): clickable element ${node.id} has no label", labeled)
        }
    }

    private fun everywhere(name: String, content: @Composable () -> Unit) {
        check(name, 1f, false, content)
    }

    @Test fun single_choice_list() = everywhere("single") { SingleChoiceVoting(choices) {} }

    @Test fun ranking_list() = everywhere("rank") { RankingVoting(choices) {} }

    @Test fun host_wait_prompt() = everywhere("prompt") { HostWaitPrompt(listOf("Veli", "Ayşe"), {}, {}) }

    @Test fun suggestions_card() =
        everywhere("suggestions") { SuggestionsCard(listOf(Suggestion("1", "u", "Sushi Bar", "Önerim")), {}, {}) }

    @Test fun quick_entry_card() = everywhere("quick entry") { QuickEntryCard("", {}, { 0 }) }

    @Test fun sticky_start_bar() = everywhere("start bar") {
        StickyStartBar(choiceCount = 3, mode = VoteMode.LIKE, onModeChange = {}, onStart = {})
    }

    @Test fun large_font_keeps_targets_and_labels() {
        check("single@2x", fontScale = 2f) { SingleChoiceVoting(choices) {} }
    }

    @Test fun dark_theme_keeps_targets_and_labels() {
        check("rank dark", dark = true) { RankingVoting(choices) {} }
    }

    @Test fun offline_banner_is_announced() {
        rule.setContent { NAPSAKTheme { OfflineBanner() } }
        rule.onNodeWithTextCompat("Bağlantı yok")
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onNodeWithTextCompat(text: String) {
        val found = onAllNodes(androidx.compose.ui.test.hasText(text, substring = true)).fetchSemanticsNodes()
        assertTrue("banner text not found", found.isNotEmpty())
        // announced politely so TalkBack reads it when it appears
        assertTrue(
            "banner is not a live region",
            found.first().let { n ->
                var cur: androidx.compose.ui.semantics.SemanticsNode? = n
                var live = false
                while (cur != null) { if (cur.config.getOrNull(SemanticsProperties.LiveRegion) != null) live = true; cur = cur.parent }
                live
            }
        )
    }
}

package com.napsak.app

import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.napsak.app.ui.components.NapsakTextField
import com.napsak.app.ui.theme.NAPSAKTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NapsakTextFieldTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun typing_is_limited_and_counted_and_can_be_cleared() {
        var text by mutableStateOf("")
        rule.setContent {
            NAPSAKTheme { Surface { NapsakTextField(value = text, onValueChange = { text = it }, label = "Takma ad", maxLength = 8) } }
        }
        rule.onNode(hasSetTextAction()).performClick()
        rule.onNode(hasSetTextAction()).performTextInput("Mehmet Ali Yilmaz")
        rule.waitForIdle()

        assertEquals("input is cut at the limit", "Mehmet A", text)
        rule.onNodeWithText("8/8").assertExists()

        // the clear button appears while the field is focused and has a spoken label
        rule.onNodeWithContentDescription("Temizle").performClick()
        rule.waitForIdle()
        assertEquals("", text)
        rule.onNodeWithText("0/8").assertExists()
    }

    @Test
    fun error_text_is_shown_and_exposed_to_screen_readers() {
        rule.setContent {
            NAPSAKTheme {
                Surface {
                    NapsakTextField(value = "123456", onValueChange = {}, label = "Oda kodu", errorText = "Oda bulunamadı")
                }
            }
        }
        rule.onNodeWithText("Oda bulunamadı").assertIsDisplayed()
        val field = rule.onNode(hasSetTextAction()).fetchSemanticsNode()
        assertEquals("Oda bulunamadı", field.config.getOrNull(SemanticsProperties.Error))
    }

    @Test
    fun helper_text_and_label_are_present() {
        rule.setContent {
            NAPSAKTheme { Surface { NapsakTextField(value = "", onValueChange = {}, label = "Oda kodu", helperText = "6 haneli kod") } }
        }
        rule.onNodeWithText("Oda kodu").assertIsDisplayed()
        rule.onNodeWithText("6 haneli kod").assertTextEquals("6 haneli kod")
    }
}

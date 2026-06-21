package com.napsak.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testCreateRoomButtonAppearsOnlyWhenNicknameIsEntered() {
        val inputField = composeTestRule.onNodeWithText("Takma Adınız")
        
        // 1. Clear any pre-filled nickname from past sessions to ensure fresh state
        inputField.performTextClearance()

        // 2. Since nickname is now blank, verify "Yeni Oda Oluştur" button does not exist/is not visible
        composeTestRule.onNodeWithText("Yeni Oda Oluştur").assertDoesNotExist()

        // 3. Type nickname "Ömer"
        inputField.performTextInput("Ömer")

        // 4. Verify that the button is now visible in the Compose UI tree
        composeTestRule.onNodeWithText("Yeni Oda Oluştur").assertIsDisplayed()
    }
}

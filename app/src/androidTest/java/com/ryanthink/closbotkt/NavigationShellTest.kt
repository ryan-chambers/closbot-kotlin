package com.ryanthink.closbotkt

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import org.junit.Rule
import org.junit.Test

class NavigationShellTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun text(id: Int, vararg args: Any) = composeRule.activity.getString(id, *args)

    @Test
    fun startsOnChatWithAllTabsVisible() {
        composeRule.onNodeWithText(text(R.string.chat_greeting)).assertIsDisplayed()
        listOf(R.string.nav_chat, R.string.nav_add_note, R.string.nav_gallery, R.string.nav_vintage)
            .forEach { composeRule.onNodeWithText(text(it)).assertIsDisplayed() }
    }

    @Test
    fun tappingATabShowsItsScreen() {
        composeRule.onNodeWithText(text(R.string.nav_vintage)).performClick()
        composeRule.onNodeWithText(text(R.string.screen_vintage)).assertIsDisplayed()
    }

    @Test
    fun openingANoteShowsTheEditScreenWithItsIdAndHidesTheBottomBar() {
        composeRule.onNodeWithText(text(R.string.nav_gallery)).performClick()
        composeRule.onNodeWithText(text(R.string.gallery_open_sample_note)).performClick()

        composeRule.onNodeWithText(text(R.string.screen_edit_note, 1L)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.nav_gallery)).assertDoesNotExist()
    }

    @Test
    fun backFromEditReturnsToTheGallery() {
        composeRule.onNodeWithText(text(R.string.nav_gallery)).performClick()
        composeRule.onNodeWithText(text(R.string.gallery_open_sample_note)).performClick()

        Espresso.pressBack()

        composeRule.onNodeWithText(text(R.string.screen_gallery)).assertIsDisplayed()
    }

    @Test
    fun switchingTabsKeepsTheChatConversation() {
        composeRule.onNodeWithText(text(R.string.chat_input_hint)).performTextInput("What pairs with duck?")
        composeRule.onNodeWithText(text(R.string.chat_send)).performClick()
        composeRule.onNodeWithText("What pairs with duck?").assertIsDisplayed()

        composeRule.onNodeWithText(text(R.string.nav_vintage)).performClick()
        composeRule.onNodeWithText(text(R.string.nav_chat)).performClick()

        composeRule.onNodeWithText("What pairs with duck?").assertIsDisplayed()
    }
}

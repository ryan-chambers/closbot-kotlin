package com.ryanthink.closbotkt.feature.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.ryanthink.closbotkt.R
import com.ryanthink.closbotkt.ui.theme.ClosBotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChatContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sent = mutableListOf<String>()

    private fun text(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setContent(state: ChatUiState = ChatUiState()) {
        composeRule.setContent {
            ClosBotTheme { ChatContent(state = state, onSendMessage = { sent += it }) }
        }
    }

    @Test
    fun showsTheGreetingBeforeAnyMessages() {
        setContent()

        composeRule.onNodeWithText(text(R.string.chat_greeting)).assertIsDisplayed()
    }

    @Test
    fun showsBothSidesOfTheConversation() {
        setContent(
            ChatUiState(
                messages = listOf(
                    ChatMessage("What pairs with duck?", ChatSender.USER),
                    ChatMessage("Try a Pinot Noir.", ChatSender.ASSISTANT),
                ),
            ),
        )

        composeRule.onNodeWithText("What pairs with duck?").assertIsDisplayed()
        composeRule.onNodeWithText("Try a Pinot Noir.").assertIsDisplayed()
    }

    @Test
    fun sendIsDisabledUntilTheUserTypesSomething() {
        setContent()

        composeRule.onNodeWithText(text(R.string.chat_send)).assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.chat_input_hint)).performTextInput("Hi")
        composeRule.onNodeWithText(text(R.string.chat_send)).assertIsEnabled()
    }

    @Test
    fun sendingReportsTheTextAndClearsTheField() {
        setContent()

        composeRule.onNodeWithText(text(R.string.chat_input_hint)).performTextInput("Hi")
        composeRule.onNodeWithText(text(R.string.chat_send)).performClick()

        assertEquals(listOf("Hi"), sent)
        composeRule.onNodeWithText("Hi").assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.chat_send)).assertIsNotEnabled()
    }

    @Test
    fun sendIsDisabledWhileWaitingForAReply() {
        setContent(ChatUiState(isWaiting = true))

        composeRule.onNodeWithText(text(R.string.chat_input_hint)).performTextInput("Hi")

        composeRule.onNodeWithText(text(R.string.chat_send)).assertIsNotEnabled()
    }

    @Test
    fun showsAnErrorMessageWhenTheLastSendFailed() {
        setContent(ChatUiState(hasError = true))

        composeRule.onNodeWithText(text(R.string.chat_error)).assertIsDisplayed()
    }
}

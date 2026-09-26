package com.ryanthink.closbotkt.feature.chat

import app.cash.turbine.test
import com.ryanthink.closbotkt.MainDispatcherRule
import com.ryanthink.closbotkt.data.assistant.WineAssistantRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class ControlledRepository : WineAssistantRepository {
        var replies = Channel<String>(Channel.UNLIMITED)
        val requests = mutableListOf<String>()

        override fun invokeChat(userMessage: String): Flow<String> {
            requests += userMessage
            return replies.receiveAsFlow()
        }
    }

    private val repository = ControlledRepository()
    private val viewModel = ChatViewModel(repository)

    @Test
    fun `starts with an empty idle state`() {
        assertEquals(ChatUiState(), viewModel.uiState.value)
    }

    @Test
    fun `sending a message shows it immediately and waits for the reply`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.onSendMessage("  What pairs with duck?  ")

            assertEquals(
                ChatUiState(
                    messages = listOf(ChatMessage("What pairs with duck?", ChatSender.USER)),
                    isWaiting = true,
                ),
                awaitItem(),
            )
            assertEquals(listOf("What pairs with duck?"), repository.requests)
        }
    }

    @Test
    fun `reply is appended and waiting ends when the flow completes`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onSendMessage("Hi")
            awaitItem()

            repository.replies.send("Hello!")
            assertEquals(
                listOf(
                    ChatMessage("Hi", ChatSender.USER),
                    ChatMessage("Hello!", ChatSender.ASSISTANT),
                ),
                awaitItem().messages,
            )

            repository.replies.close()
            assertEquals(false, awaitItem().isWaiting)
        }
    }

    @Test
    fun `later emissions replace the reply in progress`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onSendMessage("Hi")
            awaitItem()

            repository.replies.send("Hel")
            awaitItem()
            repository.replies.send("Hello!")

            assertEquals(
                listOf(
                    ChatMessage("Hi", ChatSender.USER),
                    ChatMessage("Hello!", ChatSender.ASSISTANT),
                ),
                awaitItem().messages,
            )
        }
    }

    @Test
    fun `a failing repository ends waiting and flags an error`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onSendMessage("Hi")
            awaitItem()

            repository.replies.close(IllegalStateException("network down"))

            val state = awaitItem()
            assertTrue(state.hasError)
            assertEquals(false, state.isWaiting)
        }
    }

    @Test
    fun `blank messages are ignored`() {
        viewModel.onSendMessage("   ")

        assertEquals(ChatUiState(), viewModel.uiState.value)
        assertTrue(repository.requests.isEmpty())
    }

    @Test
    fun `messages sent while waiting are ignored`() {
        viewModel.onSendMessage("First")
        viewModel.onSendMessage("Second")

        assertEquals(listOf("First"), repository.requests)
        assertEquals(1, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `sending again after an error clears the error`() {
        viewModel.onSendMessage("Hi")
        repository.replies.close(IllegalStateException("network down"))
        assertTrue(viewModel.uiState.value.hasError)
        repository.replies = Channel(Channel.UNLIMITED)

        viewModel.onSendMessage("Retry")

        val state = viewModel.uiState.value
        assertEquals(false, state.hasError)
        assertTrue(state.isWaiting)
    }
}

package com.ryanthink.closbotkt.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryanthink.closbotkt.data.assistant.WineAssistantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: WineAssistantRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onSendMessage(text: String) {
        val message = text.trim()
        if (message.isEmpty() || _uiState.value.isWaiting) return

        _uiState.update {
            it.copy(
                messages = it.messages + ChatMessage(message, ChatSender.USER),
                isWaiting = true,
                hasError = false,
            )
        }

        viewModelScope.launch {
            var replyStarted = false
            repository.invokeChat(message)
                .catch { _uiState.update { state -> state.copy(isWaiting = false, hasError = true) } }
                .collect { reply ->
                    _uiState.update { it.withAssistantReply(reply, replace = replyStarted) }
                    replyStarted = true
                }
            _uiState.update { it.copy(isWaiting = false) }
        }
    }

    private fun ChatUiState.withAssistantReply(reply: String, replace: Boolean): ChatUiState {
        val assistantMessage = ChatMessage(reply, ChatSender.ASSISTANT)
        val history = if (replace) messages.dropLast(1) else messages
        return copy(messages = history + assistantMessage)
    }
}

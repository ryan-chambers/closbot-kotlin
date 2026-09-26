package com.ryanthink.closbotkt.feature.chat

enum class ChatSender { USER, ASSISTANT }

data class ChatMessage(val text: String, val sender: ChatSender)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isWaiting: Boolean = false,
    val hasError: Boolean = false,
)

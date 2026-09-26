package com.ryanthink.closbotkt.data.assistant

import kotlinx.coroutines.flow.Flow

interface WineAssistantRepository {
    /**
     * Sends a chat message to the wine assistant. The reply is a [Flow] rather than a plain
     * suspend function so a future streaming implementation can emit partial text without
     * changing this contract. Each emission is the complete reply so far, replacing the previous one.
     */
    fun invokeChat(userMessage: String): Flow<String>
}

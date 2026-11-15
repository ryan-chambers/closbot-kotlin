package com.ryanthink.closbotkt.data.assistant.openai

import com.ryanthink.closbotkt.data.assistant.WineAssistantRepository
import com.ryanthink.closbotkt.data.knowledge.KnowledgeBase
import com.ryanthink.closbotkt.data.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Real [WineAssistantRepository] backed by OpenAI's Responses API. Conversation state lives on OpenAI's servers: each
 * reply's id is chained onto the next request via [ResponsesApiRequest.previousResponseId], so this class only ever
 * sends the newest message, not the whole transcript.
 *
 * [Singleton] because [previousResponseId] is this app's one conversation's state; a fresh instance per injection would
 * silently drop it and start a new conversation with OpenAI on every screen recreation.
 *
 * TODO(rag-in-chat): before each request, when [SettingsRepository.burgundyFocus] is on and the trimmed message is
 *   longer than 10 characters, ask [knowledge] for context about the message and build the instructions with
 *   [sommelierInstructions]. If the lookup fails, send the message without context instead of failing the chat, but
 *   never swallow a CancellationException. See OpenAiWineAssistantRepositoryTest for the exact behaviour.
 */
@Singleton
class OpenAiWineAssistantRepository
@Inject
constructor(
    private val api: OpenAiApi,
    private val knowledge: KnowledgeBase,
    private val settings: SettingsRepository,
) : WineAssistantRepository {

    private var previousResponseId: String? = null

    override fun invokeChat(userMessage: String): Flow<String> = flow {
        val response =
            api.createResponse(
                ResponsesApiRequest(
                    model = MODEL,
                    instructions = SOMMELIER_INSTRUCTIONS,
                    input = userMessage,
                    previousResponseId = previousResponseId,
                )
            )
        previousResponseId = response.id
        emit(response.outputText() ?: error("OpenAI response had no reply text"))
    }

    private companion object {
        const val MODEL = "gpt-4o-mini"
        const val SOMMELIER_INSTRUCTIONS =
            "You are a knowledgeable, friendly sommelier helping the user with wine pairing, " +
                "tasting notes, and recommendations. Keep replies concise."
    }
}

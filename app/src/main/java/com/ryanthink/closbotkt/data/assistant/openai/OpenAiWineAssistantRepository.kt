package com.ryanthink.closbotkt.data.assistant.openai

import com.ryanthink.closbotkt.data.assistant.WineAssistantRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Real [WineAssistantRepository] backed by OpenAI's Responses API. Conversation state lives on
 * OpenAI's servers: each reply's id is chained onto the next request via
 * [ResponsesApiRequest.previousResponseId], so this class only ever sends the newest message, not
 * the whole transcript.
 *
 * Not yet bound in Hilt — see feature/openai-di.
 */
class OpenAiWineAssistantRepository @Inject constructor(
    private val api: OpenAiApi,
) : WineAssistantRepository {

    private var previousResponseId: String? = null

    override fun invokeChat(userMessage: String): Flow<String> = flow {
        val response = api.createResponse(
            ResponsesApiRequest(
                model = MODEL,
                instructions = SOMMELIER_INSTRUCTIONS,
                input = userMessage,
                previousResponseId = previousResponseId,
            ),
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

package com.ryanthink.closbotkt.data.assistant.openai

import com.ryanthink.closbotkt.data.assistant.WineAssistantRepository
import com.ryanthink.closbotkt.data.knowledge.KnowledgeBase
import com.ryanthink.closbotkt.data.knowledge.WineContext
import com.ryanthink.closbotkt.data.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/**
 * Real [WineAssistantRepository] backed by OpenAI's Responses API. Conversation state lives on OpenAI's servers: each
 * reply's id is chained onto the next request via [ResponsesApiRequest.previousResponseId], so this class only ever
 * sends the newest message, not the whole transcript.
 *
 * [Singleton] because [previousResponseId] is this app's one conversation's state; a fresh instance per injection would
 * silently drop it and start a new conversation with OpenAI on every screen recreation.
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
        val trimmedMessage = userMessage.trim()
        val burgundyFocus = settings.burgundyFocus.first()
        var context: WineContext? = null
        if (burgundyFocus && trimmedMessage.length > 10) {
            try {
                context = knowledge.contextFor(trimmedMessage)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // TODO log this exception
            }
        }

        val instructions = sommelierInstructions(burgundyFocus, context)

        val response =
            api.createResponse(
                ResponsesApiRequest(
                    model = MODEL,
                    instructions = instructions,
                    input = trimmedMessage,
                    previousResponseId = previousResponseId,
                )
            )
        previousResponseId = response.id
        emit(response.outputText() ?: error("OpenAI response had no reply text"))
    }

    private companion object {
        const val MODEL = "gpt-4o-mini"
    }
}

package com.ryanthink.closbotkt.data.assistant.openai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for OpenAI's Responses API (`POST /v1/responses`). [previousResponseId] chains
 * this turn onto an earlier reply so OpenAI keeps the conversation history server-side; the app
 * never resends the full transcript.
 */
@Serializable
data class ResponsesApiRequest(
    val model: String,
    val instructions: String,
    val input: String,
    @SerialName("previous_response_id") val previousResponseId: String? = null,
)

/**
 * The real response has many more fields (usage, status, timestamps, …). Only the ones this app
 * reads are modelled here; [ignoreUnknownKeys] on the shared [kotlinx.serialization.json.Json]
 * instance lets the rest pass through unparsed.
 */
@Serializable
data class ResponsesApiResponse(
    val id: String,
    val output: List<OutputItem> = emptyList(),
) {
    /** The assistant's reply text, or null if the response contained no message content. */
    fun outputText(): String? = output
        .firstOrNull { it.type == "message" }
        ?.content
        ?.firstOrNull { it.type == "output_text" }
        ?.text
}

@Serializable
data class OutputItem(
    val type: String,
    val content: List<OutputContent> = emptyList(),
)

@Serializable
data class OutputContent(
    val type: String,
    val text: String? = null,
)

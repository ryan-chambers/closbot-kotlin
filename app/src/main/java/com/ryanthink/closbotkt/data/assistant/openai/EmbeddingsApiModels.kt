package com.ryanthink.closbotkt.data.assistant.openai

import kotlinx.serialization.Serializable

@Serializable
data class EmbeddingsApiRequest (
    val model: String,
    val input: List<String> = emptyList(),
)

@Serializable
data class Embedding(
    val embedding: List<Float> = emptyList(),
    val index: Int,
)
@Serializable
data class EmbeddingsApiResponse (
    val data: List<Embedding> = emptyList(),
)

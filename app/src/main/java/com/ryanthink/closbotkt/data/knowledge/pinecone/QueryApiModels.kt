package com.ryanthink.closbotkt.data.knowledge.pinecone

import kotlinx.serialization.Serializable

@Serializable
data class QueryApiRequest (
    val embeddings: List<Float> = emptyList(),
)
{}

@Serializable
data class QueryApiResponse (
    val score: Float,
    val source: String,
    val info: String,
)

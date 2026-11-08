package com.ryanthink.closbotkt.data.knowledge.pinecone

import kotlinx.serialization.Serializable

@Serializable
data class QueryApiRequest (
    val embeddings: List<Float> = emptyList(),
)
{}

@Serializable
data class Metadata(
    val chunk: String,
    val source: String,
)

@Serializable
data class Match(
    val id: String,
    val score: Float,
    val metadata: Metadata
)

@Serializable
data class QueryApiResponse (
    val matches: List<Match> = emptyList(),
)

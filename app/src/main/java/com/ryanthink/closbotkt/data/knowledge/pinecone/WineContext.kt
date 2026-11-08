package com.ryanthink.closbotkt.data.knowledge.pinecone

data class RagQueryResult(
    val score: Float,
    val content: String,
)

data class WineContext(
    val personalNotes: List<RagQueryResult>,
    val otherContext: List<RagQueryResult>,
)

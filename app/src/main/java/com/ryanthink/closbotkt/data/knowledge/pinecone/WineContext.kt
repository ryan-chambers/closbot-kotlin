package com.ryanthink.closbotkt.data.knowledge.pinecone

data class RagQueryResult(
    val score: Float,
    val content: String,
)

data class WineContext(
    val personalNotes: List<RagQueryResult>,
    val otherContext: List<RagQueryResult>,
)

// TODO move somewhere else, as this is not a general purpose model
data class PineconeWineContext(
    val score: Float,
    val source: String,
    val info: String,
) {}

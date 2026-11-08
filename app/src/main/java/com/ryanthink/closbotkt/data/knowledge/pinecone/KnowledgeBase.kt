package com.ryanthink.closbotkt.data.knowledge.pinecone

interface KnowledgeBase {
    suspend fun contextFor(query: String): List<WineContext>
}

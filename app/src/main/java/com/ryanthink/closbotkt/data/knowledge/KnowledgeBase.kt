package com.ryanthink.closbotkt.data.knowledge

import com.ryanthink.closbotkt.data.knowledge.pinecone.WineContext

interface KnowledgeBase {
    suspend fun contextFor(query: String): WineContext
}

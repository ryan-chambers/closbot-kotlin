package com.ryanthink.closbotkt.data.knowledge

interface KnowledgeBase {
    suspend fun contextFor(query: String): WineContext
}

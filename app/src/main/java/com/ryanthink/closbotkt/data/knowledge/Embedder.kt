package com.ryanthink.closbotkt.data.knowledge

interface Embedder {
    suspend fun embedAll(texts: List<String>): List<List<Float>>

    suspend fun embed(text: String): List<Float> {
        return embedAll(listOf(text)).single()
    }
}

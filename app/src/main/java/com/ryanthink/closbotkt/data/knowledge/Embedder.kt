package com.ryanthink.closbotkt.data.knowledge

interface Embedder {
    fun embedAll(texts: List<String>): List<List<Float>>
    fun embed(text: String): List<Float> {
        return embedAll(listOf(text)).single()
    }
}

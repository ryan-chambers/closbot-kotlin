package com.ryanthink.closbotkt.data.knowledge

import com.ryanthink.closbotkt.data.assistant.openai.EmbeddingsApiRequest
import com.ryanthink.closbotkt.data.assistant.openai.OpenAiApi

class OpenAiEmbedder(val api: OpenAiApi) : Embedder {
    override suspend fun embedAll(texts: List<String>): List<List<Float>> {
        val request = EmbeddingsApiRequest("text-embedding-3-large", texts)
        val response = api.createEmbeddings(request)
        return response.data.map { it.embedding }
    }
}

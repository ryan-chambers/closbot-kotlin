package com.ryanthink.closbotkt.data.knowledge

import com.ryanthink.closbotkt.data.knowledge.pinecone.KnowledgeBase
import com.ryanthink.closbotkt.data.knowledge.pinecone.PineconeApi
import com.ryanthink.closbotkt.data.knowledge.pinecone.QueryApiRequest
import com.ryanthink.closbotkt.data.knowledge.pinecone.WineContext

class PineconeKnowledgeBase(private val embedder: Embedder, private val api: PineconeApi) : KnowledgeBase {
    override suspend fun contextFor(query: String): List<WineContext> {
        val embeddings = embedder.embed(query)
        val response =
            api.query(
                QueryApiRequest(
                    vector = embeddings,
                    topK = 3,
                    includeMetadata = true,
                )
            )
        return response.matches.map { it ->
            WineContext(score = it.score, source = it.metadata.source, info = it.metadata.chunk)
        }
    }
}

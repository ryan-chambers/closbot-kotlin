package com.ryanthink.closbotkt.data.knowledge

import com.ryanthink.closbotkt.data.knowledge.pinecone.KnowledgeBase
import com.ryanthink.closbotkt.data.knowledge.pinecone.PineconeApi
import com.ryanthink.closbotkt.data.knowledge.pinecone.QueryApiRequest
import com.ryanthink.closbotkt.data.knowledge.pinecone.RagQueryResult
import com.ryanthink.closbotkt.data.knowledge.pinecone.WineContext

class PineconeKnowledgeBase(private val embedder: Embedder, private val api: PineconeApi) : KnowledgeBase {

    override suspend fun contextFor(query: String): WineContext {
        val embeddings = embedder.embed(query)
        val response =
            api.query(
                QueryApiRequest(
                    vector = embeddings,
                    topK = 3,
                    includeMetadata = true,
                )
            )
        val (personalNotes, otherContext) =
            response.matches.partition { it.metadata.source.equals(PERSONAL_SOURCE, ignoreCase = true) }
        return WineContext(
            personalNotes = personalNotes.map { RagQueryResult(score = it.score, content = it.metadata.chunk) },
            // Other context says where it came from, when it knows; personal notes are the user's own, so they do not.
            otherContext =
                otherContext.map {
                    val source = it.metadata.source
                    val content = if (source == null) "(no source)" else "${it.metadata.chunk} (Source: $source)"
                    RagQueryResult(score = it.score, content = content)
                },
        )
    }

    private companion object {
        /** The `source` metadata that marks a vector as one of the user's own notes. */
        const val PERSONAL_SOURCE = "My Notes"
    }
}

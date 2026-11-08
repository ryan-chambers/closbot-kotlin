package com.ryanthink.closbotkt.data.knowledge

import com.ryanthink.closbotkt.data.knowledge.pinecone.KnowledgeBase
import com.ryanthink.closbotkt.data.knowledge.pinecone.PineconeApi
import com.ryanthink.closbotkt.data.knowledge.pinecone.PineconeWineContext
import com.ryanthink.closbotkt.data.knowledge.pinecone.QueryApiRequest
import com.ryanthink.closbotkt.data.knowledge.pinecone.RagQueryResult
import com.ryanthink.closbotkt.data.knowledge.pinecone.WineContext

class PineconeKnowledgeBase(private val embedder: Embedder, private val api: PineconeApi) : KnowledgeBase {

    private val myNotes: String = "My Notes"
    private val myNotesUppercase = myNotes.uppercase()

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
        val pcContext =
            response.matches.map {
                PineconeWineContext(score = it.score, source = it.metadata.source, info = it.metadata.chunk)
            }

        val (personalNotes, otherContext) = pcContext.partition { it.source.uppercase() == myNotesUppercase }
        return WineContext(
            personalNotes = personalNotes.map { it -> toTagResult(it) },
            otherContext = otherContext.map { it -> toTagResult(it) },
        )
    }

    private fun toRagResult(p: PineconeWineContext): RagQueryResult {
        return RagQueryResult(score = p.score, content = p.info)
    }
}

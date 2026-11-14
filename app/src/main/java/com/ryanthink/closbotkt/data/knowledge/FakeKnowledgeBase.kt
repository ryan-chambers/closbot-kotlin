package com.ryanthink.closbotkt.data.knowledge

import com.ryanthink.closbotkt.data.knowledge.pinecone.RagQueryResult
import com.ryanthink.closbotkt.data.knowledge.pinecone.WineContext
import javax.inject.Inject

class FakeKnowledgeBase @Inject constructor() : KnowledgeBase {
    override suspend fun contextFor(query: String): WineContext {
        return WineContext(
            personalNotes =
                listOf(
                    RagQueryResult(
                        score = 88.0f,
                        content = "Some of my canned notes.",
                    )
                ),
            otherContext =
                listOf(
                    RagQueryResult(
                        score = 95.1f,
                        content = "My fake opinion as a sommelier is doi",
                    )
                ),
        )
    }
}

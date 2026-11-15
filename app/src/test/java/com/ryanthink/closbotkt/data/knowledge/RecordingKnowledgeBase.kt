package com.ryanthink.closbotkt.data.knowledge

/**
 * For tests: a [KnowledgeBase] that remembers what it was asked and answers with [context], or throws [failure] when
 * that is set.
 */
class RecordingKnowledgeBase(
    var context: WineContext = WineContext(personalNotes = emptyList(), otherContext = emptyList()),
    var failure: Throwable? = null,
) : KnowledgeBase {

    val queries = mutableListOf<String>()

    override suspend fun contextFor(query: String): WineContext {
        queries += query
        failure?.let { throw it }
        return context
    }
}

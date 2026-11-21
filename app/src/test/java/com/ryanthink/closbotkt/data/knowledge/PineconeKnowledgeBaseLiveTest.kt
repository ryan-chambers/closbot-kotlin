package com.ryanthink.closbotkt.data.knowledge

import com.ryanthink.closbotkt.data.LiveServices
import com.ryanthink.closbotkt.data.knowledge.pinecone.PineconeKnowledgeBase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Checks [PineconeKnowledgeBase] against the real OpenAI and Pinecone services.
 *
 * The other tests use a response shape copied from the old app. This one shows the real index agrees with it: the
 * embedding model matches the index, and real matches parse into notes and other context.
 *
 * It skips itself when the environment variables listed on [LiveServices] are missing, so `check` stays green offline.
 *
 * Gradle does not treat environment variables as test inputs, so a repeat run reports the task as up to date. Run it
 * with `./gradlew testDemoDebugUnitTest --tests '*PineconeKnowledgeBaseLiveTest' --rerun`. The results are printed to
 * the test's standard output, which is in the HTML report under `app/build/reports/tests`.
 */
class PineconeKnowledgeBaseLiveTest {

    private lateinit var knowledge: PineconeKnowledgeBase

    @Before
    fun setUp() {
        knowledge = LiveServices.fromEnvironmentOrSkip().knowledge
    }

    @Test
    fun `finds context in the real index`() = runBlocking {
        val context = knowledge.contextFor("Which Chablis do I like?")

        println("Personal notes: ${context.personalNotes}")
        println("Other context: ${context.otherContext}")

        val results = context.personalNotes + context.otherContext
        assertTrue("The index returned nothing; is it empty or the wrong one?", results.isNotEmpty())
        assertTrue("At most topK (3) results", results.size <= 3)
        assertTrue("Every result has text", results.all { it.content.isNotBlank() })
    }
}

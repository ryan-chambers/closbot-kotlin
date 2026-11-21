package com.ryanthink.closbotkt.data.assistant.openai

import com.ryanthink.closbotkt.data.LiveServices
import com.ryanthink.closbotkt.data.knowledge.KnowledgeBase
import com.ryanthink.closbotkt.data.knowledge.WineContext
import com.ryanthink.closbotkt.data.settings.FakeSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Checks [OpenAiWineAssistantRepository] against the real OpenAI and Pinecone services: the whole chat path, from
 * looking up notes to getting a reply.
 *
 * The unit tests use a fake server, so they cannot tell whether OpenAI accepts the request this app builds (model name,
 * instructions, chaining) or what the reply looks like. These tests show that it does, and print the replies so you can
 * judge the answers.
 *
 * It skips itself when the environment variables listed on [LiveServices] are missing, so `check` stays green offline.
 * Each test makes real, billable calls (a few cents at most).
 *
 * Gradle does not treat environment variables as test inputs, so a repeat run reports the task as up to date. Run it
 * with `./gradlew testDemoDebugUnitTest --tests '*OpenAiWineAssistantRepositoryLiveTest' --rerun`. The results are
 * printed to the test's standard output, which is in the HTML report under `app/build/reports/tests`.
 */
class OpenAiWineAssistantRepositoryLiveTest {

    /** Wraps the real knowledge base so a test can see what was looked up and what came back. */
    private class WatchingKnowledgeBase(private val real: KnowledgeBase) : KnowledgeBase {
        val queries = mutableListOf<String>()

        override suspend fun contextFor(query: String): WineContext {
            queries += query
            return real.contextFor(query).also {
                println("Looked up \"$query\"")
                println("  Personal notes: ${it.personalNotes}")
                println("  Other context: ${it.otherContext}")
            }
        }
    }

    private lateinit var knowledge: WatchingKnowledgeBase
    private val settings = FakeSettingsRepository(burgundyFocus = true)
    private lateinit var repository: OpenAiWineAssistantRepository

    @Before
    fun setUp() {
        val services = LiveServices.fromEnvironmentOrSkip()
        knowledge = WatchingKnowledgeBase(services.knowledge)
        repository = OpenAiWineAssistantRepository(services.openAi, knowledge, settings)
    }

    @Test
    fun `answers a Burgundy question using notes from the index`() = runBlocking {
        val reply = repository.invokeChat("Which Chablis do I like?").first()

        println("Reply: $reply")
        assertEquals("The question should have been looked up once", 1, knowledge.queries.size)
        assertTrue("The reply should not be blank", reply.isNotBlank())
    }

    @Test
    fun `answers with no lookup when the Burgundy focus is off`() = runBlocking {
        settings.setBurgundyFocus(false)

        val reply = repository.invokeChat("What pairs well with roast duck?").first()

        println("Reply: $reply")
        assertTrue("Nothing should be looked up", knowledge.queries.isEmpty())
        assertTrue("The reply should not be blank", reply.isNotBlank())
    }

    @Test
    fun `a follow-up continues the same conversation`() = runBlocking {
        settings.setBurgundyFocus(false)

        repository.invokeChat("My favourite grape is Pinot Noir. Please remember that.").first()
        val reply = repository.invokeChat("What did I say my favourite grape was?").first()

        println("Reply: $reply")
        assertTrue("The reply should remember Pinot Noir", reply.contains("Pinot Noir", ignoreCase = true))
    }
}

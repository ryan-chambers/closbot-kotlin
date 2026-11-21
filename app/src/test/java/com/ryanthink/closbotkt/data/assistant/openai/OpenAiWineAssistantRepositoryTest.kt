package com.ryanthink.closbotkt.data.assistant.openai

import app.cash.turbine.test
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.ryanthink.closbotkt.data.knowledge.RagQueryResult
import com.ryanthink.closbotkt.data.knowledge.RecordingKnowledgeBase
import com.ryanthink.closbotkt.data.knowledge.WineContext
import com.ryanthink.closbotkt.data.settings.FakeSettingsRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class OpenAiWineAssistantRepositoryTest {

    private val server = MockWebServer()
    private val knowledge = RecordingKnowledgeBase()
    private val settings = FakeSettingsRepository(burgundyFocus = true)
    private lateinit var repository: OpenAiWineAssistantRepository

    @Before
    fun setUp() {
        server.start()

        // Real Responses API replies carry many fields this app doesn't read (usage, status,
        // timestamps, …); ignoreUnknownKeys lets the DTOs stay minimal instead of modelling all of it.
        val json = Json { ignoreUnknownKeys = true }
        val client = OkHttpClient.Builder().addInterceptor(OpenAiAuthInterceptor(apiKey = "test-key")).build()
        val retrofit =
            Retrofit.Builder()
                .baseUrl(server.url("/"))
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
        repository = OpenAiWineAssistantRepository(retrofit.create(OpenAiApi::class.java), knowledge, settings)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun enqueueReply(id: String, text: String) {
        server.enqueue(
            MockResponse()
                .setBody(
                    """{"id":"$id","output":[{"type":"message","content":[{"type":"output_text","text":"$text"}]}]}"""
                )
        )
    }

    @Test
    fun `sends the message and emits the reply text`() = runTest {
        enqueueReply(id = "resp_1", text = "Try a Pinot Noir.")

        repository.invokeChat("What pairs with duck?").test {
            assertEquals("Try a Pinot Noir.", awaitItem())
            awaitComplete()
        }

        val request = server.takeRequest()
        assertEquals("Bearer test-key", request.getHeader("Authorization"))
        assertTrue(request.body.readUtf8().contains("\"input\":\"What pairs with duck?\""))
    }

    @Test
    fun `omits the previous response id on the first message`() = runTest {
        enqueueReply(id = "resp_1", text = "Hello!")

        repository.invokeChat("Hi").test {
            awaitItem()
            awaitComplete()
        }

        assertFalse(server.takeRequest().body.readUtf8().contains("previous_response_id"))
    }

    @Test
    fun `chains the previous response id onto the next message`() = runTest {
        enqueueReply(id = "resp_1", text = "Hello!")
        enqueueReply(id = "resp_2", text = "A Chablis would work well.")

        repository.invokeChat("Hi").test {
            awaitItem()
            awaitComplete()
        }
        repository.invokeChat("What pairs with oysters?").test {
            awaitItem()
            awaitComplete()
        }

        server.takeRequest() // the first request, already inspected above
        val secondBody = server.takeRequest().body.readUtf8()
        assertTrue(secondBody.contains("\"previous_response_id\":\"resp_1\""))
    }

    @Test
    fun `a server error fails the flow`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        repository.invokeChat("Hi").test { awaitError() }
    }

    @Test
    fun `a reply with no text fails the flow`() = runTest {
        server.enqueue(MockResponse().setBody("""{"id":"resp_1","output":[]}"""))

        repository.invokeChat("Hi").test { awaitError() }
    }

    // --- Retrieval (RAG) ---

    private val chablisNotes =
        WineContext(
            personalNotes = listOf(RagQueryResult(score = 92f, content = "I loved the 2019 Chablis")),
            otherContext = listOf(RagQueryResult(score = 88f, content = "Chablis is steely and mineral")),
        )

    /** Sends [message], expecting one reply, and returns the body of the request that went to OpenAI. */
    private suspend fun requestBodyFor(message: String): String {
        enqueueReply(id = "resp_1", text = "ok")
        repository.invokeChat(message).test {
            awaitItem()
            awaitComplete()
        }
        return server.takeRequest().body.readUtf8()
    }

    @Test
    fun `asks the knowledge base about a long enough message when Burgundy focus is on`() = runTest {
        requestBodyFor("Which Chablis do I like?")

        assertEquals(listOf("Which Chablis do I like?"), knowledge.queries)
    }

    @Test
    fun `puts the retrieved notes in the instructions but not in the input`() = runTest {
        knowledge.context = chablisNotes

        val body = requestBodyFor("Which Chablis do I like?")

        val instructions = body.substringAfter("\"instructions\":").substringBefore("\"input\":")
        assertTrue(instructions.contains("I loved the 2019 Chablis"))
        assertTrue(instructions.contains("Chablis is steely and mineral"))
        assertTrue(body.contains("\"input\":\"Which Chablis do I like?\""))
    }

    @Test
    fun `does not ask the knowledge base when Burgundy focus is off`() = runTest {
        settings.setBurgundyFocus(false)
        knowledge.context = chablisNotes

        val body = requestBodyFor("Which Chablis do I like?")

        assertTrue(knowledge.queries.isEmpty())
        assertFalse(body.contains("I loved the 2019 Chablis"))
        assertFalse(body.contains("Burgundy"))
    }

    @Test
    fun `says Burgundy in the instructions when focus is on, even for a short message`() = runTest {
        assertTrue(requestBodyFor("Hi").contains("Burgundy"))
    }

    @Test
    fun `does not ask the knowledge base about a short message`() = runTest {
        requestBodyFor("Hi there")

        assertTrue(knowledge.queries.isEmpty())
    }

    @Test
    fun `a message of exactly ten characters is too short, eleven is enough`() = runTest {
        requestBodyFor("0123456789")
        assertTrue(knowledge.queries.isEmpty())

        requestBodyFor("01234567890")
        assertEquals(listOf("01234567890"), knowledge.queries)
    }

    @Test
    fun `spaces around a message do not count towards its length`() = runTest {
        requestBodyFor("   Hi there   ")

        assertTrue(knowledge.queries.isEmpty())
    }

    @Test
    fun `if the lookup fails the message is still sent, without context`() = runTest {
        knowledge.failure = IOException("Pinecone is down")

        val body = requestBodyFor("Which Chablis do I like?")

        assertEquals(1, knowledge.queries.size)
        assertTrue(body.contains("\"input\":\"Which Chablis do I like?\""))
        assertFalse(body.contains("personal notes", ignoreCase = true))
    }

    @Test
    fun `a cancelled lookup cancels the chat instead of being swallowed`() = runTest {
        knowledge.failure = CancellationException("cancelled")

        try {
            repository.invokeChat("Which Chablis do I like?").collect()
            fail("Expected the CancellationException to propagate")
        } catch (expected: CancellationException) {
            // Swallowing this would send the message after the caller gave up.
        }

        assertEquals(0, server.requestCount)
    }
}

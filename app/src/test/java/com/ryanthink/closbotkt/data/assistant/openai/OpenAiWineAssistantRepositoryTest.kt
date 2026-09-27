package com.ryanthink.closbotkt.data.assistant.openai

import app.cash.turbine.test
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
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
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class OpenAiWineAssistantRepositoryTest {

    private val server = MockWebServer()
    private lateinit var repository: OpenAiWineAssistantRepository

    @Before
    fun setUp() {
        server.start()

        // Real Responses API replies carry many fields this app doesn't read (usage, status,
        // timestamps, …); ignoreUnknownKeys lets the DTOs stay minimal instead of modelling all of it.
        val json = Json { ignoreUnknownKeys = true }
        val client = OkHttpClient.Builder()
            .addInterceptor(OpenAiAuthInterceptor(apiKey = "test-key"))
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        repository = OpenAiWineAssistantRepository(retrofit.create(OpenAiApi::class.java))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun enqueueReply(id: String, text: String) {
        server.enqueue(
            MockResponse().setBody(
                """{"id":"$id","output":[{"type":"message","content":[{"type":"output_text","text":"$text"}]}]}""",
            ),
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
}

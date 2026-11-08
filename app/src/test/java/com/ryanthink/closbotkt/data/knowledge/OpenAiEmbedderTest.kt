package com.ryanthink.closbotkt.data.knowledge

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.ryanthink.closbotkt.data.assistant.openai.OpenAiApi
import com.ryanthink.closbotkt.data.assistant.openai.OpenAiAuthInterceptor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit

/**
 * Specifies [OpenAiEmbedder]: turn text into the vectors that Pinecone is searched with.
 *
 * The model must be the one the Pinecone index was filled with. Vectors from a different model are not comparable, so a
 * mismatch would not fail loudly; it would just return poor matches.
 */
class OpenAiEmbedderTest {

    private val server = MockWebServer()
    private lateinit var embedder: OpenAiEmbedder

    @Before
    fun setUp() {
        server.start()
        val json = Json { ignoreUnknownKeys = true }
        val client = OkHttpClient.Builder().addInterceptor(OpenAiAuthInterceptor(apiKey = "test-key")).build()
        val retrofit =
            Retrofit.Builder()
                .baseUrl(server.url("/"))
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
        embedder = OpenAiEmbedder(retrofit.create(OpenAiApi::class.java))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // The real response also has "object", "model" and "usage"; only "data" matters here.
    private fun enqueueVectors(vararg indexed: Pair<Int, List<Double>>) {
        val data =
            indexed.joinToString(",") { (index, vector) ->
                """{"index":$index,"embedding":[${vector.joinToString(",")}]}"""
            }
        server.enqueue(MockResponse().setBody("""{"data":[$data]}"""))
    }

    @Test
    fun `posts the texts to the embeddings endpoint with the key`() = runTest {
        enqueueVectors(0 to listOf(0.5), 1 to listOf(0.25))

        embedder.embedAll(listOf("Chablis", "Meursault"))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/embeddings", request.path)
        assertEquals("Bearer test-key", request.getHeader("Authorization"))
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals("text-embedding-3-large", body.getValue("model").jsonPrimitive.content)
        assertEquals(
            JsonArray(listOf(JsonPrimitive("Chablis"), JsonPrimitive("Meursault"))),
            body.getValue("input").jsonArray,
        )
    }

    @Test
    fun `returns one vector per text`() = runTest {
        enqueueVectors(0 to listOf(0.5, 0.25), 1 to listOf(0.125, 1.0))

        val vectors = embedder.embedAll(listOf("Chablis", "Meursault"))

        assertEquals(listOf(listOf(0.5f, 0.25f), listOf(0.125f, 1.0f)), vectors)
    }

    @Test
    fun `puts vectors back in the order of the texts even if the reply is shuffled`() = runTest {
        enqueueVectors(1 to listOf(2.0), 0 to listOf(1.0))

        val vectors = embedder.embedAll(listOf("first", "second"))

        assertEquals(listOf(listOf(1.0f), listOf(2.0f)), vectors)
    }

    @Test
    fun `embeds a single text`() = runTest {
        enqueueVectors(0 to listOf(0.5, 0.25))

        assertEquals(listOf(0.5f, 0.25f), embedder.embed("Chablis"))
    }

    @Test
    fun `makes no request for no texts`() = runTest {
        // OpenAI rejects an empty input, so asking would turn a harmless case into an error.
        assertEquals(emptyList<List<Float>>(), embedder.embedAll(emptyList()))

        assertEquals(0, server.requestCount)
    }

    @Test
    fun `lets an HTTP error through`() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"message":"bad key"}}"""))

        val error = assertThrows(HttpException::class.java) { runBlocking { embedder.embed("x") } }

        assertEquals(401, error.code())
    }
}

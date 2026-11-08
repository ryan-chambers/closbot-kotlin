package com.ryanthink.closbotkt.data.knowledge

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.ryanthink.closbotkt.data.knowledge.pinecone.PineconeApi
import com.ryanthink.closbotkt.data.knowledge.pinecone.PineconeAuthInterceptor
import com.ryanthink.closbotkt.data.knowledge.pinecone.RagQueryResult
import com.ryanthink.closbotkt.data.knowledge.pinecone.WineContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
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
 * Specifies [PineconeKnowledgeBase]: find what the cellar already knows about a question.
 *
 * Pinecone holds two kinds of text in one index: the user's own tasting notes (source "My Notes") and reference
 * material from elsewhere. The assistant treats them differently, so the lookup splits them apart.
 *
 * The contract the tests assume:
 * - `KnowledgeBase.contextFor(query: String): WineContext`
 * - `WineContext(personalNotes: List<RagQueryResult>, otherContext: List<RagQueryResult>)`
 * - `RagQueryResult(content: String, score: Float)`
 * - `PineconeKnowledgeBase(embedder: Embedder, api: PineconeApi)`
 * - `PineconeAuthInterceptor(apiKey: String)`, `PineconeApi` with a `POST query` call (the base URL is the index host)
 */
class PineconeKnowledgeBaseTest {

    /** Hands back a fixed vector and remembers what it was asked to embed. */
    private class FakeEmbedder(private val vector: List<Float>) : Embedder {
        val embedded = mutableListOf<String>()

        override suspend fun embedAll(texts: List<String>): List<List<Float>> {
            embedded += texts
            return texts.map { vector }
        }
    }

    private val server = MockWebServer()
    private val embedder = FakeEmbedder(vector = listOf(0.5f, 0.25f))
    private lateinit var knowledge: PineconeKnowledgeBase

    @Before
    fun setUp() {
        server.start()
        val json = Json { ignoreUnknownKeys = true }
        val client = OkHttpClient.Builder().addInterceptor(PineconeAuthInterceptor(apiKey = "pc-key")).build()
        val retrofit =
            Retrofit.Builder()
                .baseUrl(server.url("/"))
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
        knowledge = PineconeKnowledgeBase(embedder, retrofit.create(PineconeApi::class.java))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun match(id: String, score: Double, chunk: String, source: String) =
        """{"id":"$id","score":$score,"metadata":{"chunk":"$chunk","source":"$source"}}"""

    // The real response also has "namespace" and "usage"; only "matches" matters here.
    private fun enqueueMatches(vararg matches: String) {
        server.enqueue(MockResponse().setBody("""{"matches":[${matches.joinToString(",")}]}"""))
    }

    @Test
    fun `searches with the embedded question`() = runTest {
        enqueueMatches()

        knowledge.contextFor("Which Chablis do I like?")

        assertEquals(listOf("Which Chablis do I like?"), embedder.embedded)
    }

    @Test
    fun `posts the vector to the query endpoint with the key and API version`() = runTest {
        enqueueMatches()

        knowledge.contextFor("Which Chablis do I like?")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/query", request.path)
        assertEquals("pc-key", request.getHeader("Api-Key"))
        assertEquals("2025-01", request.getHeader("X-Pinecone-API-Version"))
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals(JsonArray(listOf(JsonPrimitive(0.5), JsonPrimitive(0.25))), body.getValue("vector").jsonArray)
        // Both are sent explicitly. The Json instance does not encode default values, so a property that
        // relies on its default would silently be left out and Pinecone would return no metadata.
        assertEquals(3, body.getValue("topK").jsonPrimitive.int)
        assertEquals(true, body.getValue("includeMetadata").jsonPrimitive.boolean)
    }

    @Test
    fun `separates personal notes from other context`() = runTest {
        enqueueMatches(
            match("1", 0.75, "Loved the minerality", "My Notes"),
            match("2", 0.5, "Chablis is steely", "Wine Folly"),
        )

        val context = knowledge.contextFor("Chablis")

        assertEquals(listOf(RagQueryResult(content = "Loved the minerality", score = 0.75f)), context.personalNotes)
        // Other context says where it came from; personal notes are the user's own, so they do not.
        assertEquals(
            listOf(RagQueryResult(content = "Chablis is steely (Source: Wine Folly)", score = 0.5f)),
            context.otherContext,
        )
    }

    @Test
    fun `recognises personal notes whatever the case of the source`() = runTest {
        enqueueMatches(match("1", 0.75, "Loved it", "MY NOTES"), match("2", 0.5, "Tart", "my notes"))

        val context = knowledge.contextFor("Chablis")

        assertEquals(listOf("Loved it", "Tart"), context.personalNotes.map { it.content })
        assertEquals(emptyList<RagQueryResult>(), context.otherContext)
    }

    @Test
    fun `keeps the order Pinecone returned within each group`() = runTest {
        enqueueMatches(
            match("1", 0.75, "first note", "My Notes"),
            match("2", 0.5, "first other", "Guide"),
            match("3", 0.25, "second note", "My Notes"),
            match("4", 0.125, "second other", "Guide"),
        )

        val context = knowledge.contextFor("Chablis")

        assertEquals(listOf("first note", "second note"), context.personalNotes.map { it.content })
        assertEquals(
            listOf("first other (Source: Guide)", "second other (Source: Guide)"),
            context.otherContext.map { it.content },
        )
    }

    @Test
    fun `returns an empty context when nothing matches`() = runTest {
        enqueueMatches()

        val context = knowledge.contextFor("Chablis")

        assertEquals(WineContext(personalNotes = emptyList(), otherContext = emptyList()), context)
    }

    @Test
    fun `lets an HTTP error through`() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"bad key"}"""))

        val error = assertThrows(HttpException::class.java) { runBlocking { knowledge.contextFor("x") } }

        assertEquals(401, error.code())
    }
}

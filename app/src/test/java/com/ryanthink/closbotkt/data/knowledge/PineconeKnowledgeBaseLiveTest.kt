package com.ryanthink.closbotkt.data.knowledge

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.ryanthink.closbotkt.data.assistant.openai.OpenAiApi
import com.ryanthink.closbotkt.data.assistant.openai.OpenAiAuthInterceptor
import com.ryanthink.closbotkt.data.knowledge.pinecone.PineconeApi
import com.ryanthink.closbotkt.data.knowledge.pinecone.PineconeAuthInterceptor
import com.ryanthink.closbotkt.data.knowledge.pinecone.RagQueryResult
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

/**
 * Checks [PineconeKnowledgeBase] against the real OpenAI and Pinecone services.
 *
 * The other tests use a response shape copied from the old app. This one shows the real index agrees with it: the
 * embedding model matches the index, and real matches parse into notes and other context.
 *
 * It needs three environment variables and skips itself when any is missing, so `check` stays green offline:
 * - `OPENAI_API_KEY`
 * - `PINECONE_API_KEY`
 * - `PINECONE_HOST` (the index host, e.g. `https://my-index-abc123.svc.pinecone.io`)
 *
 * Gradle does not treat environment variables as test inputs, so a repeat run reports the task as up to date. Run it
 * with `./gradlew testDemoDebugUnitTest --tests '*PineconeKnowledgeBaseLiveTest' --rerun`. The results are printed to
 * the test's standard output, which is in the HTML report under `app/build/reports/tests`.
 */
class PineconeKnowledgeBaseLiveTest {

    private lateinit var knowledge: PineconeKnowledgeBase

    @Before
    fun setUp() {
        val openAiKey = System.getenv("OPENAI_API_KEY")
        val pineconeKey = System.getenv("PINECONE_API_KEY")
        val pineconeHost = System.getenv("PINECONE_HOST")
        assumeTrue(
            "Set OPENAI_API_KEY, PINECONE_API_KEY and PINECONE_HOST to run",
            hasValue(openAiKey, pineconeKey, pineconeHost),
        )

        val converter = Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType())
        val openAi =
            Retrofit.Builder()
                .baseUrl("https://api.openai.com/v1/")
                .client(OkHttpClient.Builder().addInterceptor(OpenAiAuthInterceptor(openAiKey)).build())
                .addConverterFactory(converter)
                .build()
                .create(OpenAiApi::class.java)
        val pinecone =
            Retrofit.Builder()
                // Retrofit insists that the base URL ends in a slash; the host is usually copied without one.
                .baseUrl(pineconeHost.trimEnd('/') + "/")
                .client(OkHttpClient.Builder().addInterceptor(PineconeAuthInterceptor(pineconeKey)).build())
                .addConverterFactory(converter)
                .build()
                .create(PineconeApi::class.java)
        knowledge = PineconeKnowledgeBase(OpenAiEmbedder(openAi), pinecone)
    }

    private fun hasValue(vararg values: String?) = values.all { !it.isNullOrBlank() }

    @Test
    fun `finds context in the real index`() = runBlocking {
        val context = knowledge.contextFor("Which Chablis do I like?")

        println("Personal notes: ${context.personalNotes}")
        println("Other context: ${context.otherContext}")

        val results = context.personalNotes + context.otherContext
        assertTrue("The index returned nothing; is it empty or the wrong one?", results.isNotEmpty())
        assertTrue("At most topK (3) results", results.size <= 3)
        assertTrue("Every result has text", results.all { it.content.isNotBlank() })
        assertTrue("Other context names its source", context.otherContext.all(::namesItsSource))
        assertTrue("Personal notes do not", context.personalNotes.none(::namesItsSource))
    }

    private fun namesItsSource(result: RagQueryResult) = Regex("""\(Source: .+\)$""").containsMatchIn(result.content)
}

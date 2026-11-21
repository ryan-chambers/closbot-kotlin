package com.ryanthink.closbotkt.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.ryanthink.closbotkt.data.assistant.openai.OpenAiApi
import com.ryanthink.closbotkt.data.assistant.openai.OpenAiAuthInterceptor
import com.ryanthink.closbotkt.data.knowledge.OpenAiEmbedder
import com.ryanthink.closbotkt.data.knowledge.pinecone.PineconeApi
import com.ryanthink.closbotkt.data.knowledge.pinecone.PineconeAuthInterceptor
import com.ryanthink.closbotkt.data.knowledge.pinecone.PineconeKnowledgeBase
import com.ryanthink.closbotkt.data.knowledge.pinecone.pineconeBaseUrl
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assume.assumeTrue
import retrofit2.Retrofit

/**
 * The real OpenAI and Pinecone clients, for the live tests that talk to the actual services.
 *
 * [fromEnvironmentOrSkip] reads three environment variables and skips the calling test (a JUnit assumption, not a
 * failure) when any is missing, so `check` stays green offline:
 * - `OPENAI_API_KEY`
 * - `PINECONE_API_KEY`
 * - `PINECONE_HOST` (the index host, e.g. `https://my-index-abc123.svc.pinecone.io`)
 */
class LiveServices private constructor(val openAi: OpenAiApi, val knowledge: PineconeKnowledgeBase) {

    companion object {
        fun fromEnvironmentOrSkip(): LiveServices {
            val openAiKey = System.getenv("OPENAI_API_KEY")
            val pineconeKey = System.getenv("PINECONE_API_KEY")
            val pineconeHost = System.getenv("PINECONE_HOST")
            assumeTrue(
                "Set OPENAI_API_KEY, PINECONE_API_KEY and PINECONE_HOST to run",
                listOf(openAiKey, pineconeKey, pineconeHost).all { !it.isNullOrBlank() },
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
                    .baseUrl(pineconeBaseUrl(pineconeHost))
                    .client(OkHttpClient.Builder().addInterceptor(PineconeAuthInterceptor(pineconeKey)).build())
                    .addConverterFactory(converter)
                    .build()
                    .create(PineconeApi::class.java)
            return LiveServices(openAi, PineconeKnowledgeBase(OpenAiEmbedder(openAi), pinecone))
        }
    }
}

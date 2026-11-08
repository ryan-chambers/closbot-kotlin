package com.ryanthink.closbotkt.data.knowledge.pinecone

import okhttp3.Interceptor
import okhttp3.Response

/** Attaches API key and version headers */
class PineconeAuthInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val authorized =
            chain.request().newBuilder().header("Api-Key", apiKey).header("X-Pinecone-API-Version", "2025-01").build()
        return chain.proceed(authorized)
    }
}

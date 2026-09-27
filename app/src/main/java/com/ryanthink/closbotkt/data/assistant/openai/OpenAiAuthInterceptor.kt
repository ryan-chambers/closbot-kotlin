package com.ryanthink.closbotkt.data.assistant.openai

import okhttp3.Interceptor
import okhttp3.Response

/** Attaches the OpenAI API key to every request as a bearer token. */
class OpenAiAuthInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val authorized = chain.request().newBuilder()
            .header("Authorization", "Bearer $apiKey")
            .build()
        return chain.proceed(authorized)
    }
}

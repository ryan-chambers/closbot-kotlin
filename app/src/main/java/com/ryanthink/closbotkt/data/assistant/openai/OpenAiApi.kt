package com.ryanthink.closbotkt.data.assistant.openai

import retrofit2.http.Body
import retrofit2.http.POST

/** Retrofit contract for the endpoints this app calls. The base URL supplies the `/v1/` prefix. */
interface OpenAiApi {
    @POST("responses")
    suspend fun createResponse(@Body request: ResponsesApiRequest): ResponsesApiResponse
}

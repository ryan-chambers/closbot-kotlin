package com.ryanthink.closbotkt.data.knowledge.pinecone

import retrofit2.http.Body
import retrofit2.http.POST

interface PineconeApi {
    @POST("query") suspend fun query(@Body request: QueryApiRequest): QueryApiResponse
}

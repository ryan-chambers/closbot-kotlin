package com.ryanthink.closbotkt.data.knowledge.pinecone

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.ryanthink.closbotkt.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

/** prod flavor: wires the real OkHttp/Retrofit client the [PineconeApi] binding needs. */
@Module
@InstallIn(SingletonComponent::class)
object PineconeNetworkModule {

    @Provides
    @Singleton
    fun providePineconeApi(): PineconeApi {
        val client =
            OkHttpClient.Builder().addInterceptor(PineconeAuthInterceptor(apiKey = BuildConfig.PINECONE_API_KEY)).build()
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(BuildConfig.PINECONE_HOST)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(PineconeApi::class.java)
    }
}

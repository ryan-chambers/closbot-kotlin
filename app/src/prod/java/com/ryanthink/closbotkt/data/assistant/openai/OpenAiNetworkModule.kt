package com.ryanthink.closbotkt.data.assistant.openai

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

/** prod flavor: wires the real OkHttp/Retrofit client the [OpenAiApi] binding needs. */
@Module
@InstallIn(SingletonComponent::class)
object OpenAiNetworkModule {

    private const val BASE_URL = "https://api.openai.com/v1/"

    @Provides
    @Singleton
    fun provideOpenAiApi(): OpenAiApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(OpenAiAuthInterceptor(apiKey = BuildConfig.OPENAI_API_KEY))
            .build()
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenAiApi::class.java)
    }
}

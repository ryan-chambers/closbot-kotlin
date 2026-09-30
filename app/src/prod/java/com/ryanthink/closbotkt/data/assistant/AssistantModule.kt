package com.ryanthink.closbotkt.data.assistant

import com.ryanthink.closbotkt.data.assistant.openai.OpenAiWineAssistantRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** prod flavor: binds the real, OpenAI-backed repository. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AssistantModule {

    @Binds
    abstract fun bindWineAssistantRepository(
        impl: OpenAiWineAssistantRepository,
    ): WineAssistantRepository
}

package com.ryanthink.closbotkt.data.assistant

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** demo flavor: binds the fake so the app runs fully offline, with no OpenAI key required. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AssistantModule {

    @Binds
    abstract fun bindWineAssistantRepository(
        impl: FakeWineAssistantRepository,
    ): WineAssistantRepository
}

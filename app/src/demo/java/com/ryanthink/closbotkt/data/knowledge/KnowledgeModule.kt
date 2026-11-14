package com.ryanthink.closbotkt.data.knowledge

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class KnowledgeModule {

    @Binds
    abstract fun bindKnowledgeBase(impl: FakeKnowledgeBase): KnowledgeBase
}

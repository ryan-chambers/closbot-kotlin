package com.ryanthink.closbotkt.data.knowledge

import com.ryanthink.closbotkt.data.knowledge.pinecone.PineconeKnowledgeBase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class KnowledgeModule {

    @Binds abstract fun bindKnowledgeBase(impl: PineconeKnowledgeBase): KnowledgeBase
}

package com.ryanthink.closbotkt.data.assistant

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeWineAssistantRepository @Inject constructor() : WineAssistantRepository {

    override fun invokeChat(userMessage: String): Flow<String> = flow {
        emit("This is a canned response from the fake wine assistant.")
    }
}

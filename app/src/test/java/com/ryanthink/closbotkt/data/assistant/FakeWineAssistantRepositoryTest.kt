package com.ryanthink.closbotkt.data.assistant

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeWineAssistantRepositoryTest {

    private val repository = FakeWineAssistantRepository()

    @Test
    fun `invokeChat emits a single canned reply`() = runTest {
        repository.invokeChat("What should I drink tonight?").test {
            assertEquals("This is a canned response from the fake wine assistant.", awaitItem())
            awaitComplete()
        }
    }
}

package com.ryanthink.closbotkt.data.notes

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeWineNoteRepositoryTest {

    private val repository = FakeWineNoteRepository()

    @Test
    fun `a saved note shows up in observeNotes`() = runTest {
        repository.observeNotes().test {
            assertEquals(emptyList<WineNote>(), awaitItem())

            val id = repository.addNote(NewWineNote("a.jpg", "Chablis"))

            assertEquals(listOf(id), awaitItem().map { it.id })
        }
    }
}

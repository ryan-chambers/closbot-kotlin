package com.ryanthink.closbotkt.data.notes

import app.cash.turbine.test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomWineNoteRepositoryTest {

    private val now = Instant.parse("2026-03-01T12:00:00Z")
    private val repository = RoomWineNoteRepository(
        dao = FakeWineNoteDao(),
        clock = Clock.fixed(now, ZoneOffset.UTC),
    )

    @Test
    fun `addNote stamps the creation time and returns the assigned id`() = runTest {
        val id = repository.addNote(NewWineNote(photoFileName = "a.jpg", details = "Chablis"))

        val saved = repository.getNote(id)

        assertEquals(WineNote(id, "a.jpg", "Chablis", emptyList(), now), saved)
    }

    @Test
    fun `updateNote keeps the id and creation time`() = runTest {
        val id = repository.addNote(NewWineNote("a.jpg", "Chablis"))
        val saved = repository.getNote(id)!!

        repository.updateNote(saved.copy(details = "Chablis Premier Cru", labels = listOf("white")))

        assertEquals(
            saved.copy(details = "Chablis Premier Cru", labels = listOf("white")),
            repository.getNote(id),
        )
    }

    @Test
    fun `deleteNote removes the note`() = runTest {
        val id = repository.addNote(NewWineNote("a.jpg", "Chablis"))

        repository.deleteNote(repository.getNote(id)!!)

        assertNull(repository.getNote(id))
    }

    @Test
    fun `observeNotes emits again after each change`() = runTest {
        repository.observeNotes().test {
            assertEquals(emptyList<WineNote>(), awaitItem())

            repository.addNote(NewWineNote("a.jpg", "Chablis"))

            assertEquals(listOf("Chablis"), awaitItem().map { it.details })
        }
    }
}

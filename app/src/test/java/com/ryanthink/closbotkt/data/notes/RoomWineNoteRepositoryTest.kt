package com.ryanthink.closbotkt.data.notes

import app.cash.turbine.test
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RoomWineNoteRepositoryTest {

    @get:Rule val temporaryFolder = TemporaryFolder()

    private val now = Instant.parse("2026-03-01T12:00:00Z")

    // Lazy because the temporary folder only exists once the rule has run.
    private val photoStorage by lazy { PhotoStorage(File(temporaryFolder.root, "photos")) }
    private val repository by lazy {
        RoomWineNoteRepository(
            dao = FakeWineNoteDao(),
            clock = Clock.fixed(now, ZoneOffset.UTC),
            photoStorage = photoStorage,
        )
    }

    /** Stores a photo file and a note that points at it. */
    private suspend fun savedNoteWithPhoto(details: String = "Chablis"): WineNote {
        val photoFileName = photoStorage.copyIn(byteArrayOf(1, 2, 3).inputStream())
        val id = repository.addNote(NewWineNote(photoFileName, details))
        return repository.getNote(id)!!
    }

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
    fun `deleteNote removes the note's photo file`() = runTest {
        val note = savedNoteWithPhoto()
        assertTrue(photoStorage.fileFor(note.photoFileName).exists())

        repository.deleteNote(note)

        assertFalse(photoStorage.fileFor(note.photoFileName).exists())
    }

    @Test
    fun `deleteNote leaves other notes and their photos alone`() = runTest {
        val deleted = savedNoteWithPhoto("Chablis")
        val kept = savedNoteWithPhoto("Merlot")

        repository.deleteNote(deleted)

        assertNotNull(repository.getNote(kept.id))
        assertTrue(photoStorage.fileFor(kept.photoFileName).exists())
    }

    @Test
    fun `deleteNote still removes a note whose photo file is already gone`() = runTest {
        val id = repository.addNote(NewWineNote("never-stored", "Chablis"))

        repository.deleteNote(repository.getNote(id)!!)

        assertNull(repository.getNote(id))
    }

    @Test
    fun `deleteNote can be repeated, so a failed delete can be retried`() = runTest {
        val note = savedNoteWithPhoto()

        repository.deleteNote(note)
        repository.deleteNote(note)

        assertNull(repository.getNote(note.id))
    }

    @Test
    fun `deleteNote removes the note first, then reports a photo it cannot remove`() = runTest {
        // A directory with something inside can't be deleted, which stands in for a file the system won't let go.
        File(photoStorage.fileFor("stuck"), "inner").apply {
            parentFile.mkdirs()
            writeBytes(byteArrayOf(1))
        }
        val id = repository.addNote(NewWineNote("stuck", "Chablis"))

        val failure = runCatching { repository.deleteNote(repository.getNote(id)!!) }.exceptionOrNull()

        assertTrue(failure is IOException)
        // The note is gone even so: an orphaned file is invisible and harmless, a note without its photo is not.
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

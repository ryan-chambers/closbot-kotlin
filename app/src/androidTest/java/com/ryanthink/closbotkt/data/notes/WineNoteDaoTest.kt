package com.ryanthink.closbotkt.data.notes

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs against a real, in-memory SQLite database (via [Room.inMemoryDatabaseBuilder]) rather than
 * a fake DAO, since the thing worth verifying here is that Room's generated SQL and the
 * [Converters] round-trip correctly — a fake couldn't catch a mistake in either.
 */
@RunWith(AndroidJUnit4::class)
class WineNoteDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: WineNoteDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        dao = database.wineNoteDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertThenGetById_returnsTheSameNote() = runBlocking {
        val id = dao.insert(sampleNote())

        val loaded = dao.getById(id)

        assertEquals("Domaine Leflaive, Puligny-Montrachet", loaded?.details)
        assertEquals(listOf("white", "burgundy"), loaded?.labels)
    }

    @Test
    fun observeAll_ordersMostRecentFirst() = runBlocking {
        val older = sampleNote(createdAt = Instant.parse("2026-01-01T00:00:00Z"))
        val newer = sampleNote(createdAt = Instant.parse("2026-06-01T00:00:00Z"))
        dao.insert(older)
        dao.insert(newer)

        val notes = dao.observeAll().first()

        assertEquals(newer.createdAt, notes.first().createdAt)
    }

    @Test
    fun delete_removesTheNote() = runBlocking {
        val id = dao.insert(sampleNote())
        val note = dao.getById(id)!!

        dao.delete(note)

        assertNull(dao.getById(id))
    }

    private fun sampleNote(
        details: String = "Domaine Leflaive, Puligny-Montrachet",
        createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    ) = WineNoteEntity(
        photoFileName = "bottle.jpg",
        details = details,
        labels = listOf("white", "burgundy"),
        createdAt = createdAt,
    )
}

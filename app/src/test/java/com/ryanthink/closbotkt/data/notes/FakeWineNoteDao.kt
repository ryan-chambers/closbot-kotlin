package com.ryanthink.closbotkt.data.notes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [WineNoteDao] so the repository can be tested on the JVM, without Room or a device. */
class FakeWineNoteDao : WineNoteDao {

    private val rows = MutableStateFlow<List<WineNoteEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<WineNoteEntity>> =
        rows.map { list -> list.sortedByDescending { it.createdAt } }

    override suspend fun getById(id: Long): WineNoteEntity? = rows.value.find { it.id == id }

    override suspend fun insert(note: WineNoteEntity): Long {
        val id = nextId++
        rows.value += note.copy(id = id)
        return id
    }

    override suspend fun update(note: WineNoteEntity) {
        rows.value = rows.value.map { if (it.id == note.id) note else it }
    }

    override suspend fun delete(note: WineNoteEntity) {
        rows.value = rows.value.filterNot { it.id == note.id }
    }
}

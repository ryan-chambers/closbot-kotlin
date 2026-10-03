package com.ryanthink.closbotkt.data.notes

import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomWineNoteRepository @Inject constructor(
    private val dao: WineNoteDao,
    private val clock: Clock,
) : WineNoteRepository {

    override fun observeNotes(): Flow<List<WineNote>> =
        dao.observeAll().map { entities -> entities.map(WineNoteEntity::toDomain) }

    override suspend fun getNote(id: Long): WineNote? = dao.getById(id)?.toDomain()

    override suspend fun addNote(note: NewWineNote): Long =
        dao.insert(note.toEntity(createdAt = clock.instant()))

    override suspend fun updateNote(note: WineNote) = dao.update(note.toEntity())

    override suspend fun deleteNote(note: WineNote) = dao.delete(note.toEntity())
}

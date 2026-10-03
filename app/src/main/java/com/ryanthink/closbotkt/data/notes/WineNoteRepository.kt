package com.ryanthink.closbotkt.data.notes

import kotlinx.coroutines.flow.Flow

interface WineNoteRepository {

    /** All notes, newest first. Re-emits whenever a note is added, changed or removed. */
    fun observeNotes(): Flow<List<WineNote>>

    /** Returns the note with [id], or null if there is none (for instance, it was just deleted). */
    suspend fun getNote(id: Long): WineNote?

    /** Saves [note] and returns the id it was given. */
    suspend fun addNote(note: NewWineNote): Long

    suspend fun updateNote(note: WineNote)

    suspend fun deleteNote(note: WineNote)
}

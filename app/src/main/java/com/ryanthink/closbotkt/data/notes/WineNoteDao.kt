package com.ryanthink.closbotkt.data.notes

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WineNoteDao {

    @Query("SELECT * FROM wine_notes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<WineNoteEntity>>

    @Query("SELECT * FROM wine_notes WHERE id = :id")
    suspend fun getById(id: Long): WineNoteEntity?

    @Insert
    suspend fun insert(note: WineNoteEntity): Long

    @Update
    suspend fun update(note: WineNoteEntity)

    @Delete
    suspend fun delete(note: WineNoteEntity)
}

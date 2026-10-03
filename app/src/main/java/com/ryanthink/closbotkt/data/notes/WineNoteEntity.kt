package com.ryanthink.closbotkt.data.notes

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A gallery entry: a wine bottle photo plus the note text about it. Mirrors the old app's
 * `WinePhoto` model, minus the Capacitor-specific `webviewPath` (a rendering detail, not data).
 */
@Entity(tableName = "wine_notes")
data class WineNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val photoFileName: String,
    val details: String,
    val labels: List<String>,
    val createdAt: Instant,
)

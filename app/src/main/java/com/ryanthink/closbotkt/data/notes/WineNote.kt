package com.ryanthink.closbotkt.data.notes

import java.time.Instant

/**
 * A saved wine note as the rest of the app sees it. Deliberately separate from [WineNoteEntity]:
 * the entity's shape is dictated by Room and the schema, this one by what the UI needs, so a
 * schema change doesn't have to ripple into every screen.
 */
data class WineNote(
    val id: Long,
    val photoFileName: String,
    val details: String,
    val labels: List<String>,
    val createdAt: Instant,
)

/**
 * A note that hasn't been saved yet. It has no id (the database assigns one) and no timestamp (the
 * repository stamps it), so neither can be faked by a caller.
 */
data class NewWineNote(
    val photoFileName: String,
    val details: String,
    val labels: List<String> = emptyList(),
)

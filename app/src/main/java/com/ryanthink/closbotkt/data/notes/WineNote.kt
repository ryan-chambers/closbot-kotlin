package com.ryanthink.closbotkt.data.notes

import java.time.Instant

/**
 * A saved wine note as the rest of the app sees it.
 *
 * Design decision: this deliberately duplicates [WineNoteEntity], even though the two are
 * field-for-field identical today.
 * - The entity's shape is dictated by Room and the schema (storable types, annotations); this
 *   class's shape is dictated by what the UI needs. Keeping them apart means a schema change
 *   (renaming a column, splitting `details`, storing something other than a file name) is absorbed
 *   by the mappers in `WineNoteMappers.kt` instead of rippling into every screen.
 * - Only the repository touches entities; ViewModels and composables see [WineNote] and
 *   [NewWineNote]. Retrofitting that boundary once many screens use the entity is painful, while
 *   the cost of having it now is a few lines of mapping code.
 * - If the two ever stop diverging, collapsing them into one class is a cheap, mechanical change;
 *   going the other way is not.
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
 *
 * Design decision: this is a separate type from [WineNote], rather than one class with a nullable
 * `id`, so the compiler enforces the difference. `addNote` can only be given an unsaved note, and
 * `updateNote`/`deleteNote` can only be given one that has a real id. The entity's `id = 0`
 * placeholder is a Room detail that the rest of the app shouldn't have to know about.
 */
data class NewWineNote(
    val photoFileName: String,
    val details: String,
    val labels: List<String> = emptyList(),
)

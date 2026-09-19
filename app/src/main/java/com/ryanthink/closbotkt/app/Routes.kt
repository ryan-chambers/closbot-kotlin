package com.ryanthink.closbotkt.app

import kotlinx.serialization.Serializable

// Each destination is a type. Navigation Compose serializes it to a route string,
// so arguments are checked by the compiler instead of parsed out of "edit-note/{id}".

@Serializable
data object ChatRoute

@Serializable
data object AddNoteRoute

@Serializable
data object GalleryRoute

@Serializable
data object VintageRoute

@Serializable
data class EditNoteRoute(val noteId: Long)

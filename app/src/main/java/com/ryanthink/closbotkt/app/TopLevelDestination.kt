package com.ryanthink.closbotkt.app

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.ui.graphics.vector.ImageVector
import com.ryanthink.closbotkt.R

/** The destinations shown in the bottom bar. Edit note is reached from the gallery, so it is not one. */
enum class TopLevelDestination(
    val route: Any,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Chat(ChatRoute, R.string.nav_chat, Icons.Default.Email),
    AddNote(AddNoteRoute, R.string.nav_add_note, Icons.Default.Add),
    Gallery(GalleryRoute, R.string.nav_gallery, Icons.AutoMirrored.Filled.List),
    Vintage(VintageRoute, R.string.nav_vintage, Icons.Default.DateRange),
}

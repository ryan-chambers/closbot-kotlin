package com.ryanthink.closbotkt.feature.notes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ryanthink.closbotkt.R
import com.ryanthink.closbotkt.core.ui.PlaceholderScreen

@Composable
fun EditNoteScreen(noteId: Long, modifier: Modifier = Modifier) {
    PlaceholderScreen(title = stringResource(R.string.screen_edit_note, noteId), modifier = modifier)
}

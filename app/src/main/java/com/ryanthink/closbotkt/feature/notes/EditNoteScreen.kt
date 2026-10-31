package com.ryanthink.closbotkt.feature.notes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ryanthink.closbotkt.R
import com.ryanthink.closbotkt.core.ui.PlaceholderScreen

@Composable
fun EditNoteScreen(
    noteId: Long,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditNoteViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PlaceholderScreen(title = stringResource(R.string.screen_edit_note, noteId), modifier = modifier)
}

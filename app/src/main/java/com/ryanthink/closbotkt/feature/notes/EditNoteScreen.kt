package com.ryanthink.closbotkt.feature.notes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage

@Composable
fun EditNoteScreen(
    noteId: Long,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditNoteViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    EditNoteContent(
        state = state
    )
}

@Composable
fun EditNoteContent(
    state: EditNoteUiState,
    modifier: Modifier = Modifier,

) {
    Column(
        modifier = modifier
    ) {
        if (state is EditNoteUiState.Editing) {

            AsyncImage(
                model = state.photoFileName,
                contentDescription ="TODO",
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop,
            )
            Text("Can you see the photo?")
        } else {
            Text("Could not load photo")
        }
    }
}

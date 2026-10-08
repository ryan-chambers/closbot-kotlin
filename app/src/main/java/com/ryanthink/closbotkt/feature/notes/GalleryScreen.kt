package com.ryanthink.closbotkt.feature.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ryanthink.closbotkt.R
import com.ryanthink.closbotkt.data.notes.WineNote
import java.io.File

/**
 * Stateful entry point: connects [GalleryContent] to its [GalleryViewModel]. It takes a callback
 * rather than a NavController, so the content can be previewed and tested without navigation.
 */
@Composable
fun GalleryScreen(
    onNoteClick: (noteId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GalleryContent(
        state = state,
        photoFile = viewModel::photoFile,
        onSearchTermChange = viewModel::onSearchTermChange,
        onTagSelected = viewModel::onTagSelected,
        onNoteClick = onNoteClick,
        modifier = modifier,
    )
}

/** Stateless gallery UI: renders [state] and reports the user's intent through the callbacks. */
@Composable
fun GalleryContent(
    state: GalleryUiState,
    photoFile: (WineNote) -> File,
    onSearchTermChange: (String) -> Unit,
    onTagSelected: (String) -> Unit,
    onNoteClick: (noteId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.searchTerm,
            onValueChange = onSearchTermChange,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text(stringResource(R.string.gallery_search_hint)) },
            singleLine = true,
        )

        // Either the chosen tag (tap to clear) or, while typing, labels to choose from. The
        // ViewModel never reports both at once.
        state.selectedTag?.let { tag ->
            InputChip(
                selected = true,
                onClick = { onTagSelected(tag) },
                label = { Text(tag) },
                modifier = Modifier.padding(horizontal = 16.dp),
                trailingIcon = {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.gallery_clear_tag, tag),
                    )
                },
            )
        }
        if (state.matchingLabels.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.matchingLabels) { label ->
                    SuggestionChip(onClick = { onTagSelected(label) }, label = { Text(label) })
                }
            }
        }

        when {
            state.isLoading -> CenteredBox { CircularProgressIndicator() }
            !state.hasAnyNotes -> CenteredBox { Text(stringResource(R.string.gallery_empty)) }
            state.notes.isEmpty() -> CenteredBox { Text(stringResource(R.string.gallery_no_matches)) }
            else -> PhotoGrid(state.notes, photoFile, onNoteClick)
        }
    }
}

@Composable
private fun PhotoGrid(
    notes: List<WineNote>,
    photoFile: (WineNote) -> File,
    onNoteClick: (noteId: Long) -> Unit,
) {
    // Shown while a photo loads and if its file is missing, so the cell keeps its place in the grid.
    val missingPhoto = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)

    LazyVerticalGrid(
        // As many columns as fit at least 120dp wide, so phones, tablets and landscape all work.
        columns = GridCells.Adaptive(minSize = 120.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // The id as key lets Compose keep each cell's state when notes are added, removed or
        // reordered, instead of matching cells by position.
        items(notes, key = WineNote::id) { note ->
            val description = if (note.labels.isEmpty()) {
                stringResource(R.string.gallery_photo_unlabelled)
            } else {
                stringResource(R.string.gallery_photo_labelled, note.labels.joinToString())
            }
            // Coil decodes the photo at the size of this cell rather than at camera resolution,
            // and caches the result.
            AsyncImage(
                model = photoFile(note),
                contentDescription = description,
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onNoteClick(note.id) },
                placeholder = missingPhoto,
                error = missingPhoto,
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

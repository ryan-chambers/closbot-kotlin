package com.ryanthink.closbotkt.feature.notes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ryanthink.closbotkt.R
import java.io.File

@Composable
fun EditNoteScreen(
    noteId: Long,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditNoteViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    EditNoteContent(
        state = state,
        photoFile = viewModel.fileForPhoto(),
        onDetailsChange = viewModel::onDetailsChange,
        onLabelsChange = viewModel::onLabelsChange,
        modifier = modifier,
    )
}

@Composable
fun EditNoteContent(
    state: EditNoteUiState,
    photoFile: File?,
    onDetailsChange: (String) -> Unit,
    onLabelsChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        when (state) {
            is EditNoteUiState.Editing if photoFile != null -> {
                val description =
                    if (state.details.isEmpty()) {
                        stringResource(R.string.edit_note_no_details)
                    } else {
                        stringResource(R.string.gallery_photo_labelled, state.details)
                    }

                AsyncImage(
                    model = photoFile,
                    contentDescription = description,
                    modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop,
                )

                OutlinedTextField(
                    value = state.details,
                    onValueChange = onDetailsChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.edit_note_edit_details)) },
                    minLines = 3,
                )

                OutlinedTextField(
                    value = state.labelsText,
                    onValueChange = onLabelsChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.edit_note_labels)) },
                    supportingText = { Text(stringResource(R.string.edit_note_labels_hint)) },
                    singleLine = true,
                )
            }

            is EditNoteUiState.Loading -> {
                // TODO
                Text("Pretend I'm a spinner")
            }

            else -> {
                Text("Could not load photo")
            }
        }
    }
}

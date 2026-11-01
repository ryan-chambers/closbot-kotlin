package com.ryanthink.closbotkt.feature.notes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

    val finished = (state as? EditNoteUiState.Editing)?.isFinished == true
    LaunchedEffect(finished) {
        if (finished) {
            onDone()
            viewModel.onFinishedHandled()
        }
    }

    EditNoteContent(
        state = state,
        photoFile = viewModel.fileForPhoto(),
        onDetailsChange = viewModel::onDetailsChange,
        onLabelsChange = viewModel::onLabelsChange,
        onSave = viewModel::onSave,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        modifier = modifier,
    )
}

@Composable
fun EditNoteContent(
    state: EditNoteUiState,
    photoFile: File?,
    onDetailsChange: (String) -> Unit,
    onLabelsChange: (String) -> Unit,
    onSave: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }

    when (state) {
        is EditNoteUiState.Editing if photoFile != null ->
            Column(modifier = modifier.verticalScroll(rememberScrollState())) {
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

                Row() {
                    OutlinedButton(
                        onClick = onSave,
                        enabled = !state.isBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.edit_note_save))
                    }

                    OutlinedButton(
                        onClick = { confirmingDelete = true },
                        enabled = !state.isBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.edit_note_delete))
                    }
                }
            }

        is EditNoteUiState.Loading -> {
            val loading = stringResource(R.string.edit_note_loading)
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loading })
            }
        }

        else ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.edit_note_not_found))
            }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false }, // back button or tap outside
            title = { Text(stringResource(R.string.edit_note_delete_title)) },
            text = { Text(stringResource(R.string.edit_note_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDelete = false
                        onDeleteConfirmed()
                    }
                ) {
                    Text(stringResource(R.string.edit_note_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) {
                    Text(stringResource(R.string.edit_note_cancel))
                }
            },
        )
    }
}

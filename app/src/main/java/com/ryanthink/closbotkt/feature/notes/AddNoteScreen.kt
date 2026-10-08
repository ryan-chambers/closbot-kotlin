package com.ryanthink.closbotkt.feature.notes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ryanthink.closbotkt.R

/**
 * Stateful entry point: connects [AddNoteContent] to its [AddNoteViewModel] and to the Photo
 * Picker. [onSaved] is called once after a note is saved, so the caller decides where to go next.
 */
@Composable
fun AddNoteScreen(
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddNoteViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // The system Photo Picker: no permission needed, because the user hands the app just the photo
    // they choose. The launcher returns null if they back out without choosing.
    val photoPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onPhotoPicked(uri.toString())
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onSaved()
            viewModel.onSavedHandled()
        }
    }

    AddNoteContent(
        state = state,
        onPickPhoto = { photoPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
        onDetailsChange = viewModel::onDetailsChange,
        onLabelsChange = viewModel::onLabelsChange,
        onSave = viewModel::onSave,
        modifier = modifier,
    )
}

/** Stateless add-note form: renders [state] and reports the user's intent through the callbacks. */
@Composable
fun AddNoteContent(
    state: AddNoteUiState,
    onPickPhoto: () -> Unit,
    onDetailsChange: (String) -> Unit,
    onLabelsChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Scrolls so the form stays reachable in landscape and with the keyboard open; imePadding lifts
    // it above the keyboard, which edge-to-edge no longer does for us.
    Column(
        modifier = modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        state.photoSource?.let { photo ->
            AsyncImage(
                model = photo,
                contentDescription = stringResource(R.string.add_note_photo_preview),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop,
            )
        }
        OutlinedButton(onClick = onPickPhoto, modifier = Modifier.fillMaxWidth()) {
            val label = if (state.photoSource == null) {
                R.string.add_note_choose_photo
            } else {
                R.string.add_note_change_photo
            }
            Text(stringResource(label))
        }

        OutlinedTextField(
            value = state.details,
            onValueChange = onDetailsChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.add_note_details)) },
            minLines = 3,
        )
        OutlinedTextField(
            value = state.labelsText,
            onValueChange = onLabelsChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.add_note_labels)) },
            supportingText = { Text(stringResource(R.string.add_note_labels_hint)) },
            singleLine = true,
        )

        if (state.hasError) {
            Text(
                text = stringResource(R.string.add_note_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Button(onClick = onSave, enabled = state.canSave, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.add_note_save))
        }
    }
}

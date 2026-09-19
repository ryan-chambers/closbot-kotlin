package com.ryanthink.closbotkt.feature.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ryanthink.closbotkt.R

/**
 * Placeholder gallery. The button stands in for tapping a note and shows how an argument
 * travels to the edit screen. The screen takes a callback rather than a NavController, so it
 * can be previewed and tested without navigation.
 */
@Composable
fun GalleryScreen(
    onNoteClick: (noteId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = stringResource(R.string.screen_gallery))
        Button(onClick = { onNoteClick(SAMPLE_NOTE_ID) }) {
            Text(text = stringResource(R.string.gallery_open_sample_note))
        }
    }
}

private const val SAMPLE_NOTE_ID = 1L

package com.ryanthink.closbotkt.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryanthink.closbotkt.data.notes.WineNote
import com.ryanthink.closbotkt.data.notes.WineNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Turns the saved notes plus the user's filter choices into a [GalleryUiState].
 *
 * The notes come from [WineNoteRepository.observeNotes] and the filter inputs are two small
 * [MutableStateFlow]s; [combine] re-runs the filtering whenever any of the three changes, so the
 * grid updates whether a note is added or the user types in the search box.
 */
@HiltViewModel
class GalleryViewModel @Inject constructor(
    repository: WineNoteRepository,
) : ViewModel() {

    private val searchTerm = MutableStateFlow("")
    private val requestedTag = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GalleryUiState> =
        combine(repository.observeNotes(), searchTerm, requestedTag, ::buildState)
            .stateIn(
                scope = viewModelScope,
                // Keep the database query alive for a few seconds after the last collector leaves,
                // so a rotation doesn't tear it down and restart it.
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = GalleryUiState(),
            )

    fun onSearchTermChange(term: String) {
        searchTerm.value = term
    }

    /** Selects [tag] as the filter, or clears it if it is already selected. */
    fun onTagSelected(tag: String) {
        requestedTag.update { current -> if (current == tag) null else tag }
        searchTerm.value = ""
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

private fun buildState(notes: List<WineNote>, term: String, requestedTag: String?): GalleryUiState {
    val allLabels = notes.flatMap { it.labels }.distinct().sorted()
    // The requested tag may have disappeared (its last note was edited or deleted); ignore it then
    // rather than showing an empty grid under a filter the user can no longer see a reason for.
    val tag = requestedTag?.takeIf { it in allLabels }
    val needle = term.trim()

    val visible = notes
        .filter { tag == null || tag in it.labels }
        .filter { note -> needle.isEmpty() || note.labels.any { it.contains(needle, ignoreCase = true) } }

    return GalleryUiState(
        isLoading = false,
        notes = visible,
        hasAnyNotes = notes.isNotEmpty(),
        searchTerm = term,
        selectedTag = tag,
        matchingLabels = if (needle.isEmpty() || tag != null) {
            emptyList()
        } else {
            allLabels.filter { it.contains(needle, ignoreCase = true) }
        },
    )
}

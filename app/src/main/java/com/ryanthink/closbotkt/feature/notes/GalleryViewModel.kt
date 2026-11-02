package com.ryanthink.closbotkt.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryanthink.closbotkt.data.notes.PhotoStorage
import com.ryanthink.closbotkt.data.notes.WineNote
import com.ryanthink.closbotkt.data.notes.WineNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
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
 * The notes come from [WineNoteRepository.observeNotes] and the filter inputs are held together in one
 * [MutableStateFlow]; [combine] re-runs the filtering whenever either changes, so the grid updates whether a note is
 * added or the user types in the search box. The search term and the selected tag live in one value so that changing
 * both (choosing a tag clears the search) is a single update; as two flows, [combine] could emit in between.
 */
@HiltViewModel
class GalleryViewModel
@Inject
constructor(
    repository: WineNoteRepository,
    private val photoStorage: PhotoStorage,
) : ViewModel() {

    private val filter = MutableStateFlow(Filter())

    val uiState: StateFlow<GalleryUiState> =
        combine(repository.observeNotes(), filter, ::buildState)
            .stateIn(
                scope = viewModelScope,
                // Keep the database query alive for a few seconds after the last collector leaves,
                // so a rotation doesn't tear it down and restart it.
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = GalleryUiState(),
            )

    /** The image file for [note]'s photo, for the grid to load. */
    fun photoFile(note: WineNote): File = photoStorage.fileFor(note.photoFileName)

    fun onSearchTermChange(term: String) {
        filter.update { it.copy(searchTerm = term) }
    }

    /** Selects [tag] as the filter, or clears it if it is already selected. */
    fun onTagSelected(tag: String) {
        filter.update { Filter(requestedTag = if (it.requestedTag == tag) null else tag) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

private data class Filter(val searchTerm: String = "", val requestedTag: String? = null)

private fun buildState(notes: List<WineNote>, filter: Filter): GalleryUiState {
    val (term, requestedTag) = filter
    val allLabels = notes.flatMap { it.labels }.toSet()
    // The requested tag may have disappeared (its last note was edited or deleted); ignore it then
    // rather than showing an empty grid under a filter the user can no longer see a reason for.
    val tag = requestedTag?.takeIf { it in allLabels }
    val needle = term.trim()

    val visible =
        notes
            .filter { tag == null || tag in it.labels }
            .filter { note -> needle.isEmpty() || note.labels.any { it.contains(needle, ignoreCase = true) } }

    return GalleryUiState(
        isLoading = false,
        notes = visible,
        hasAnyNotes = notes.isNotEmpty(),
        searchTerm = term,
        selectedTag = tag,
        matchingLabels =
            if (needle.isEmpty() || tag != null) {
                emptyList()
            } else {
                // Only the suggestions are shown in order, so that's the only place that sorts.
                allLabels.filter { it.contains(needle, ignoreCase = true) }.sorted()
            },
    )
}

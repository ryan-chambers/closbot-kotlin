package com.ryanthink.closbotkt.feature.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryanthink.closbotkt.data.notes.PhotoStorage
import com.ryanthink.closbotkt.data.notes.WineNote
import com.ryanthink.closbotkt.data.notes.WineNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EditNoteViewModel
@Inject
constructor(
    private val repository: WineNoteRepository,
    private val photoStorage: PhotoStorage,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    // TODO extract constant
    private val noteId: Long = checkNotNull(savedStateHandle["noteId"])
    private var note: WineNote? = null

    private val _uiState = MutableStateFlow<EditNoteUiState>(EditNoteUiState.Loading)
    val uiState: StateFlow<EditNoteUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val repoNote = repository.getNote(noteId)
            if (repoNote == null) {
                _uiState.update { EditNoteUiState.NotFound }
            } else {
                note = repoNote
                _uiState.update {
                    EditNoteUiState.Editing(
                        photoFileName = repoNote.photoFileName,
                        details = repoNote.details,
                        labelsText = formatLabels(repoNote.labels),
                    )
                }
            }
        }
    }

    fun onDetailsChange(newDetails: String) = editing { it.copy(details = newDetails) }

    fun onLabelsChange(newLabels: String) = editing { it.copy(labelsText = newLabels) }

    fun onSave() {
        val current = note ?: return
        val form = _uiState.value
        if (form is EditNoteUiState.Editing) {
            if (!form.isBusy) {
                editing { it.copy(isBusy = true) }

                viewModelScope.launch {
                    repository.updateNote(
                        current.copy(
                            details = form.details.trim(),
                            labels = parseLabels(form.labelsText),
                        )
                    )

                    editing { it.copy(hasError = false, isBusy = false, isFinished = true) }
                }
            }
        }
    }

    fun onDeleteConfirmed() {
        val current = note ?: return
        val form = _uiState.value
        if (form is EditNoteUiState.Editing) {
            if (!form.isBusy) {
                viewModelScope.launch {
                    editing { it.copy(isBusy = true, hasError = false) }

                    try {
                        repository.deleteNote(current)
                        editing { it.copy(isBusy = false, isFinished = true) }
                    } catch (_: IOException) {
                        editing { it.copy(hasError = true, isBusy = false) }
                        // TODO log exception
                    }
                }
            }
        }
    }

    fun onFinishedHandled() = editing { it.copy(isFinished = false) }

    private fun editing(change: (EditNoteUiState.Editing) -> EditNoteUiState.Editing) =
        _uiState.update { state ->
            if (state is EditNoteUiState.Editing) change(state) else state
        }
}

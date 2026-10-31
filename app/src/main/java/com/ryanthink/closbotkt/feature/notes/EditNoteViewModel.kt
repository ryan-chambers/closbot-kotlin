package com.ryanthink.closbotkt.feature.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryanthink.closbotkt.data.notes.PhotoStorage
import com.ryanthink.closbotkt.data.notes.WineNote
import com.ryanthink.closbotkt.data.notes.WineNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val savedHandleState: SavedStateHandle,
) : ViewModel() {
    // TODO extract constant
    private val noteId: Long = checkNotNull(savedHandleState["noteId"])
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

    fun onDetailsChange(newDetails: String) = editing { it.copy(details = newDetails.trim()) }

    fun onLabelsChange(newLabels: String) = editing { it.copy(labelsText = newLabels) }

    fun onSave() {
        val form = _uiState.value
        if (form is EditNoteUiState.Editing) {
            if (!form.isBusy) {
                editing { it.copy(isBusy = true) }

                viewModelScope.launch {
                    repository.updateNote(
                        WineNote(
                            id = noteId,
                            photoFileName = form.photoFileName,
                            details = form.details.trim(),
                            labels = parseLabels(form.labelsText),
                            createdAt = note?.createdAt!!,
                        )
                    )

                    editing { it.copy(hasError = false, isBusy = false, isFinished = true) }
                }
            }
        }
    }

    fun onDeleteConfirmed() {}

    fun onFinishedHandled() = editing { it.copy(isFinished = false) }

    private fun editing(change: (EditNoteUiState.Editing) -> EditNoteUiState.Editing) =
        _uiState.update { state ->
            if (state is EditNoteUiState.Editing) change(state) else state
        }
}

package com.ryanthink.closbotkt.feature.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryanthink.closbotkt.data.notes.PhotoStorage
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

    private val _uiState = MutableStateFlow<EditNoteUiState>(EditNoteUiState.Loading)
    val uiState: StateFlow<EditNoteUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val photo = repository.getNote(noteId)
            if (photo == null) {
                _uiState.update { EditNoteUiState.NotFound }
            } else {
                _uiState.update {
                    EditNoteUiState.Editing(
                        photoFileName = photo.photoFileName,
                        details = photo.details,
                        labelsText = formatLabels(photo.labels),
                    )
                }
            }
        }
    }

    fun onDetailsChange(newDetails: String) = _uiState.update { state ->
        if (state is EditNoteUiState.Editing) state.copy(details = newDetails) else state
    }

    fun onLabelsChange(newLabels: String) = _uiState.update { state ->
        if (state is EditNoteUiState.Editing) state.copy(labelsText = newLabels) else state
    }

    fun onSave() {}

    fun onDeleteConfirmed() {}

    fun onFinishedHandled() {}
}

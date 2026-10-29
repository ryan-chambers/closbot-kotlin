package com.ryanthink.closbotkt.feature.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.ryanthink.closbotkt.data.notes.PhotoStorage
import com.ryanthink.closbotkt.data.notes.WineNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class EditNoteViewModel
@Inject
constructor(
    private val repository: WineNoteRepository,
    private val photoStorage: PhotoStorage,
    private val savedHandleState: SavedStateHandle,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditNoteUiState.Loading)
    val uiState: StateFlow<EditNoteUiState> = _uiState.asStateFlow()

    fun onDetailsChange(newDetails: String) {
        // TODO
    }

    fun onLabelsChange(newLabels: String) {
        // TODO
    }

    fun onSave() {}

    fun onDeleteConfirmed() {}

    fun onFinishedHandled() {}
}

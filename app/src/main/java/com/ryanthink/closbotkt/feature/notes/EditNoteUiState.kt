package com.ryanthink.closbotkt.feature.notes

sealed interface EditNoteUiState {
    data object Loading : EditNoteUiState

    data object NotFound : EditNoteUiState

    data class Editing(
        val photoFileName: String,
        val details: String,
        val labelsText: String,
        val isBusy: Boolean = false,
        val hasError: Boolean = false,
        val isFinished: Boolean = false,
    ) : EditNoteUiState
}

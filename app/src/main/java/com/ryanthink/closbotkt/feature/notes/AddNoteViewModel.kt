package com.ryanthink.closbotkt.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryanthink.closbotkt.data.notes.NewWineNote
import com.ryanthink.closbotkt.data.notes.PhotoImporter
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
class AddNoteViewModel @Inject constructor(
    private val repository: WineNoteRepository,
    private val photoImporter: PhotoImporter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddNoteUiState())
    val uiState: StateFlow<AddNoteUiState> = _uiState.asStateFlow()

    /** [source] is the picked photo's content URI as text. */
    fun onPhotoPicked(source: String) = _uiState.update { it.copy(photoSource = source, hasError = false) }

    fun onDetailsChange(details: String) = _uiState.update { it.copy(details = details) }

    fun onLabelsChange(labelsText: String) = _uiState.update { it.copy(labelsText = labelsText) }

    fun onSave() {
        val form = _uiState.value
        val source = form.photoSource
        if (source == null || form.isSaving) return

        _uiState.update { it.copy(isSaving = true, hasError = false) }
        viewModelScope.launch {
            try {
                val photoFileName = photoImporter.importPhoto(source)
                repository.addNote(
                    NewWineNote(
                        photoFileName = photoFileName,
                        details = form.details.trim(),
                        labels = parseLabels(form.labelsText),
                    ),
                )
                // A fresh form, so coming back to this tab starts a new note.
                _uiState.value = AddNoteUiState(isSaved = true)
            } catch (e: IOException) {
                // The form is kept as it was so the user can simply try again.
                _uiState.update { it.copy(isSaving = false, hasError = true) }
            }
        }
    }

    fun onSavedHandled() = _uiState.update { it.copy(isSaved = false) }

    private fun parseLabels(text: String): List<String> =
        text.split(',').map(String::trim).filter(String::isNotEmpty).distinct()
}

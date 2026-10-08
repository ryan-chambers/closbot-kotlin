package com.ryanthink.closbotkt.feature.notes

/**
 * The add-note form.
 *
 * [photoSource] is the chosen photo's content URI as text. The photo is only copied into the app
 * when the note is saved, so abandoning the form leaves nothing behind on disk.
 *
 * [isSaved] is a one-shot signal for the screen to navigate away. The screen reports it handled with
 * `onSavedHandled`, so the signal survives a configuration change but doesn't fire twice.
 */
data class AddNoteUiState(
    val photoSource: String? = null,
    val details: String = "",
    /** Labels as typed, separated by commas. Split into a list when the note is saved. */
    val labelsText: String = "",
    val isSaving: Boolean = false,
    val hasError: Boolean = false,
    val isSaved: Boolean = false,
) {
    val canSave: Boolean get() = photoSource != null && !isSaving
}

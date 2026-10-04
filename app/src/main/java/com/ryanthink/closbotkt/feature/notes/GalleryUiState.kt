package com.ryanthink.closbotkt.feature.notes

import com.ryanthink.closbotkt.data.notes.WineNote

/**
 * Everything the gallery screen draws. [notes] is already filtered by [searchTerm] and
 * [selectedTag], so the screen only renders it.
 *
 * [isLoading] and [hasAnyNotes] exist so the screen can tell three situations apart: still loading,
 * no notes saved yet, and notes saved but none matching the filter.
 */
data class GalleryUiState(
    val isLoading: Boolean = true,
    val notes: List<WineNote> = emptyList(),
    val hasAnyNotes: Boolean = false,
    val searchTerm: String = "",
    val selectedTag: String? = null,
    /** Labels containing [searchTerm], offered as tag suggestions. Empty once a tag is selected. */
    val matchingLabels: List<String> = emptyList(),
)

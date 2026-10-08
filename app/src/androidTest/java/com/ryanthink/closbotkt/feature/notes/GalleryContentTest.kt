package com.ryanthink.closbotkt.feature.notes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.ryanthink.closbotkt.R
import com.ryanthink.closbotkt.data.notes.WineNote
import com.ryanthink.closbotkt.ui.theme.ClosBotTheme
import java.io.File
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GalleryContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val opened = mutableListOf<Long>()
    private val searched = mutableListOf<String>()
    private val tagged = mutableListOf<String>()

    private fun text(id: Int, vararg args: Any) =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun note(id: Long, vararg labels: String) =
        WineNote(id, "$id.jpg", details = "", labels = labels.toList(), createdAt = Instant.EPOCH)

    private fun setContent(state: GalleryUiState) {
        composeRule.setContent {
            ClosBotTheme {
                GalleryContent(
                    state = state,
                    photoFile = { File("missing", it.photoFileName) },
                    onSearchTermChange = { searched += it },
                    onTagSelected = { tagged += it },
                    onNoteClick = { opened += it },
                )
            }
        }
    }

    @Test
    fun explainsHowToAddNotesWhenThereAreNone() {
        setContent(GalleryUiState(isLoading = false))

        composeRule.onNodeWithText(text(R.string.gallery_empty)).assertIsDisplayed()
    }

    @Test
    fun saysSoWhenTheFilterMatchesNothing() {
        setContent(GalleryUiState(isLoading = false, hasAnyNotes = true, searchTerm = "zin"))

        composeRule.onNodeWithText(text(R.string.gallery_no_matches)).assertIsDisplayed()
    }

    @Test
    fun showsAPhotoPerNoteAndOpensTheOneTapped() {
        setContent(
            GalleryUiState(
                isLoading = false,
                hasAnyNotes = true,
                notes = listOf(note(1, "Chablis"), note(2, "Barolo", "Nebbiolo")),
            ),
        )

        composeRule.onNodeWithContentDescription(text(R.string.gallery_photo_labelled, "Chablis"))
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            text(R.string.gallery_photo_labelled, "Barolo, Nebbiolo"),
        ).performClick()

        assertEquals(listOf(2L), opened)
    }

    @Test
    fun anUnlabelledNoteStillHasADescription() {
        setContent(GalleryUiState(isLoading = false, hasAnyNotes = true, notes = listOf(note(1))))

        composeRule.onNodeWithContentDescription(text(R.string.gallery_photo_unlabelled))
            .assertIsDisplayed()
    }

    @Test
    fun typingReportsTheSearchTerm() {
        setContent(GalleryUiState(isLoading = false))

        composeRule.onNodeWithText(text(R.string.gallery_search_hint)).performTextInput("cha")

        assertEquals(listOf("cha"), searched)
    }

    @Test
    fun tappingASuggestionSelectsThatTag() {
        setContent(
            GalleryUiState(
                isLoading = false,
                hasAnyNotes = true,
                searchTerm = "cha",
                matchingLabels = listOf("Chablis", "Chardonnay"),
            ),
        )

        composeRule.onNodeWithText("Chardonnay").performClick()

        assertEquals(listOf("Chardonnay"), tagged)
    }

    @Test
    fun tappingTheSelectedTagClearsIt() {
        setContent(GalleryUiState(isLoading = false, hasAnyNotes = true, selectedTag = "Chablis"))

        composeRule.onNodeWithText("Chablis").performClick()

        assertEquals(listOf("Chablis"), tagged)
    }
}

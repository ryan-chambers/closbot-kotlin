package com.ryanthink.closbotkt.feature.notes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.ryanthink.closbotkt.R
import com.ryanthink.closbotkt.ui.theme.ClosBotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AddNoteContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var pickRequests = 0
    private var saveRequests = 0
    private val details = mutableListOf<String>()
    private val labels = mutableListOf<String>()

    private fun text(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    // Holds the state the way a ViewModel would, so a text field shows what was typed into it.
    private fun setContent(initial: AddNoteUiState) {
        composeRule.setContent {
            ClosBotTheme {
                var state by remember { mutableStateOf(initial) }
                AddNoteContent(
                    state = state,
                    onPickPhoto = { pickRequests++ },
                    onDetailsChange = {
                        details += it
                        state = state.copy(details = it)
                    },
                    onLabelsChange = {
                        labels += it
                        state = state.copy(labelsText = it)
                    },
                    onSave = { saveRequests++ },
                )
            }
        }
    }

    @Test
    fun cannotSaveUntilAPhotoIsChosen() {
        setContent(AddNoteUiState())

        composeRule.onNodeWithText(text(R.string.add_note_save)).assertIsNotEnabled()
    }

    @Test
    fun choosingAPhotoIsOfferedAndReported() {
        setContent(AddNoteUiState())

        composeRule.onNodeWithText(text(R.string.add_note_choose_photo)).performClick()

        assertEquals(1, pickRequests)
    }

    @Test
    fun aChosenPhotoIsPreviewedAndCanBeChanged() {
        setContent(AddNoteUiState(photoSource = "content://missing/1"))

        composeRule.onNodeWithContentDescription(text(R.string.add_note_photo_preview)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.add_note_change_photo)).performClick()

        assertEquals(1, pickRequests)
    }

    @Test
    fun savingIsReportedOnceAPhotoIsChosen() {
        setContent(AddNoteUiState(photoSource = "content://missing/1"))

        composeRule.onNodeWithText(text(R.string.add_note_save)).performScrollTo().assertIsEnabled().performClick()

        assertEquals(1, saveRequests)
    }

    @Test
    fun typingIsReported() {
        setContent(AddNoteUiState())

        composeRule.onNodeWithText(text(R.string.add_note_details)).performTextInput("Lovely")
        composeRule.onNodeWithText(text(R.string.add_note_labels)).performTextInput("Chablis")

        assertEquals(listOf("Lovely"), details)
        assertEquals(listOf("Chablis"), labels)
    }

    @Test
    fun aFailedSaveIsExplained() {
        setContent(AddNoteUiState(photoSource = "content://missing/1", hasError = true))

        composeRule.onNodeWithText(text(R.string.add_note_error)).performScrollTo().assertIsDisplayed()
    }
}

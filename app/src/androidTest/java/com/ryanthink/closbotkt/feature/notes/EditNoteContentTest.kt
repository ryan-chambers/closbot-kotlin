package com.ryanthink.closbotkt.feature.notes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.ryanthink.closbotkt.R
import com.ryanthink.closbotkt.ui.theme.ClosBotTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EditNoteContentTest {

    @get:Rule val composeRule = createComposeRule()

    private var saveRequests = 0
    private var deleteRequests = 0
    private val details = mutableListOf<String>()
    private val labels = mutableListOf<String>()

    private fun text(id: Int, vararg args: Any) =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun editing(details: String = "", labelsText: String = "", isBusy: Boolean = false) =
        EditNoteUiState.Editing(photoFileName = "1.jpg", details = details, labelsText = labelsText, isBusy = isBusy)

    // Holds the state the way a ViewModel would, so a text field shows what was typed into it.
    private fun setContent(initial: EditNoteUiState) {
        composeRule.setContent {
            ClosBotTheme {
                var state by remember { mutableStateOf(initial) }
                EditNoteContent(
                    state = state,
                    photoFile = File("missing", "1.jpg"),
                    onDetailsChange = {
                        details += it
                        state = (state as EditNoteUiState.Editing).copy(details = it)
                    },
                    onLabelsChange = {
                        labels += it
                        state = (state as EditNoteUiState.Editing).copy(labelsText = it)
                    },
                    onSave = { saveRequests++ },
                    onDeleteConfirmed = { deleteRequests++ },
                )
            }
        }
    }

    private fun dialogButton(id: Int) = composeRule.onNode(hasText(text(id)) and hasAnyAncestor(isDialog()))

    @Test
    fun showsTheNoteBeingEdited() {
        setContent(editing(details = "Lovely with duck", labelsText = "Chablis, 2019"))

        composeRule.onNodeWithText("Lovely with duck").assertIsDisplayed()
        composeRule.onNodeWithText("Chablis, 2019").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription(text(R.string.gallery_photo_labelled, "Lovely with duck"))
            .assertIsDisplayed()
    }

    @Test
    fun showsAProgressIndicatorWhileTheNoteLoads() {
        setContent(EditNoteUiState.Loading)

        composeRule.onNodeWithContentDescription(text(R.string.edit_note_loading)).assertIsDisplayed()
    }

    @Test
    fun explainsWhenTheNoteCannotBeFound() {
        setContent(EditNoteUiState.NotFound)

        composeRule.onNodeWithText(text(R.string.edit_note_not_found)).assertIsDisplayed()
    }

    @Test
    fun aNoteWithoutDetailsHasAGenericPhotoDescription() {
        setContent(editing())

        composeRule.onNodeWithContentDescription(text(R.string.edit_note_no_details)).assertIsDisplayed()
    }

    @Test
    fun typingIsReported() {
        setContent(editing())

        composeRule.onNodeWithText(text(R.string.edit_note_edit_details)).performTextInput("Lovely")
        composeRule.onNodeWithText(text(R.string.edit_note_labels)).performTextInput("Chablis")

        assertEquals(listOf("Lovely"), details)
        assertEquals(listOf("Chablis"), labels)
    }

    @Test
    fun savingIsReported() {
        setContent(editing())

        composeRule.onNodeWithText(text(R.string.edit_note_save)).assertIsEnabled().performClick()

        assertEquals(1, saveRequests)
    }

    @Test
    fun cannotSaveOrDeleteWhileBusy() {
        setContent(editing(isBusy = true))

        composeRule.onNodeWithText(text(R.string.edit_note_save)).assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.edit_note_delete)).assertIsNotEnabled()
    }

    @Test
    fun deletingAsksForConfirmationFirst() {
        setContent(editing())

        composeRule.onNodeWithText(text(R.string.edit_note_delete)).performClick()

        composeRule.onNode(isDialog()).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.edit_note_delete_message)).assertIsDisplayed()
        assertEquals(0, deleteRequests)
    }

    @Test
    fun confirmingTheDialogReportsTheDeleteOnce() {
        setContent(editing())
        composeRule.onNodeWithText(text(R.string.edit_note_delete)).performClick()

        dialogButton(R.string.edit_note_delete).performClick()

        assertEquals(1, deleteRequests)
        composeRule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun cancellingTheDialogKeepsTheNote() {
        setContent(editing())
        composeRule.onNodeWithText(text(R.string.edit_note_delete)).performClick()

        composeRule.onNodeWithText(text(R.string.edit_note_cancel)).performClick()

        assertEquals(0, deleteRequests)
        composeRule.onNode(isDialog()).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.edit_note_save)).assertIsDisplayed()
    }
}

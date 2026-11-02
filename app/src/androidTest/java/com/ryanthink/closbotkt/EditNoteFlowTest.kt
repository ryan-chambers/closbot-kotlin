package com.ryanthink.closbotkt

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.waitUntilExactlyOneExists
import com.ryanthink.closbotkt.data.notes.NewWineNote
import com.ryanthink.closbotkt.data.notes.WineNoteRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Follows a note from the gallery to the edit screen and back, through the real navigation graph and the real
 * ViewModels. Only the database is swapped, for an in-memory one that each test seeds through the repository.
 *
 * Room reports changes from its own threads, which Compose's test clock knows nothing about, so anything that depends
 * on the database is awaited with `waitUntilExactlyOneExists` rather than asserted straight away.
 */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class EditNoteFlowTest {

    // Hilt's rule must run first: it builds the dependency graph that the activity then starts with.
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var repository: WineNoteRepository

    @Before
    fun saveANote() {
        hiltRule.inject()
        runBlocking {
            repository.addNote(
                NewWineNote(photoFileName = "flow-test.jpg", details = "Lovely with duck", labels = listOf("Chablis"))
            )
        }
    }

    private fun text(id: Int, vararg args: Any) = composeRule.activity.getString(id, *args)

    private fun noteLabelled(label: String) = hasContentDescription(text(R.string.gallery_photo_labelled, label))

    private fun dialogButton(id: Int) = hasText(text(id)) and hasAnyAncestor(isDialog())

    private fun openTheNote() {
        composeRule.onNodeWithText(text(R.string.nav_gallery)).performClick()
        composeRule.waitUntilExactlyOneExists(noteLabelled("Chablis"))
        composeRule.onNode(noteLabelled("Chablis")).performClick()
        composeRule.waitUntilExactlyOneExists(hasText(text(R.string.edit_note_save)))
    }

    // Does what the system Back button does, without depending on the device's navigation mode.
    private fun pressBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
    }

    @Test
    fun tappingANoteOpensItForEditing() {
        openTheNote()

        composeRule.onNodeWithText("Lovely with duck").assertIsDisplayed()
        composeRule.onNodeWithText("Chablis").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.nav_gallery)).assertDoesNotExist()
    }

    @Test
    fun backReturnsToTheGalleryWithoutChangingTheNote() {
        openTheNote()

        pressBack()

        composeRule.onNodeWithText(text(R.string.gallery_search_hint)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.nav_gallery)).assertIsDisplayed()
        composeRule.onNode(noteLabelled("Chablis")).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.edit_note_save)).assertDoesNotExist()
    }

    @Test
    fun savingReturnsToTheGalleryShowingTheChange() {
        openTheNote()

        composeRule.onNodeWithText("Chablis").performTextReplacement("Riesling")
        composeRule.onNodeWithText(text(R.string.edit_note_save)).performClick()

        composeRule.waitUntilExactlyOneExists(noteLabelled("Riesling"))
        composeRule.onNode(noteLabelled("Chablis")).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.edit_note_save)).assertDoesNotExist()
    }

    @Test
    fun deletingReturnsToTheGalleryWithoutTheNote() {
        openTheNote()

        composeRule.onNodeWithText(text(R.string.edit_note_delete)).performClick()
        composeRule.onNode(dialogButton(R.string.edit_note_delete)).performClick()

        composeRule.waitUntilExactlyOneExists(hasText(text(R.string.gallery_empty)))
        composeRule.onNode(noteLabelled("Chablis")).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.edit_note_save)).assertDoesNotExist()
    }
}

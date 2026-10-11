package com.ryanthink.closbotkt.feature.notes

import androidx.lifecycle.SavedStateHandle
import com.ryanthink.closbotkt.MainDispatcherRule
import com.ryanthink.closbotkt.data.notes.FakeCameraPhotoFiles
import com.ryanthink.closbotkt.data.notes.FakePhotoImporter
import com.ryanthink.closbotkt.data.notes.FakeWineNoteRepository
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddNoteViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeWineNoteRepository()
    private val importer = FakePhotoImporter()
    private val cameraFiles = FakeCameraPhotoFiles()
    private val savedStateHandle = SavedStateHandle()
    private val viewModel = AddNoteViewModel(repository, importer, cameraFiles, savedStateHandle)

    private suspend fun savedNotes() = repository.observeNotes().first()

    @Test
    fun `starts as an empty form that cannot be saved`() {
        assertEquals(AddNoteUiState(), viewModel.uiState.value)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `choosing a photo allows saving`() {
        viewModel.onPhotoPicked("content://photo/1")

        assertEquals("content://photo/1", viewModel.uiState.value.photoSource)
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `saving without a photo does nothing`() = runTest {
        viewModel.onDetailsChange("Lovely")

        viewModel.onSave()

        assertTrue(savedNotes().isEmpty())
        assertFalse(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `saving imports the photo and stores the note`() = runTest {
        viewModel.onPhotoPicked("content://photo/1")
        viewModel.onDetailsChange("  Lovely with fish  ")
        viewModel.onLabelsChange("Chablis, Chardonnay")

        viewModel.onSave()

        assertEquals(listOf("content://photo/1"), importer.imported)
        val note = savedNotes().single()
        assertEquals("photo-1", note.photoFileName)
        assertEquals("Lovely with fish", note.details)
        assertEquals(listOf("Chablis", "Chardonnay"), note.labels)
    }

    @Test
    fun `labels are trimmed and blank or repeated ones are dropped`() = runTest {
        viewModel.onPhotoPicked("content://photo/1")
        viewModel.onLabelsChange(" Barolo ,, Nebbiolo,Barolo , ")

        viewModel.onSave()

        assertEquals(listOf("Barolo", "Nebbiolo"), savedNotes().single().labels)
    }

    @Test
    fun `a saved note resets the form and signals the screen once`() = runTest {
        viewModel.onPhotoPicked("content://photo/1")
        viewModel.onDetailsChange("Lovely")

        viewModel.onSave()

        assertEquals(AddNoteUiState(isSaved = true), viewModel.uiState.value)

        viewModel.onSavedHandled()

        assertEquals(AddNoteUiState(), viewModel.uiState.value)
    }

    @Test
    fun `a failed import keeps the form, reports the error and can be retried`() = runTest {
        viewModel.onPhotoPicked("content://photo/1")
        viewModel.onDetailsChange("Lovely")
        importer.failure = IOException("no space")

        viewModel.onSave()

        val failed = viewModel.uiState.value
        assertTrue(failed.hasError)
        assertFalse(failed.isSaving)
        assertEquals("Lovely", failed.details)
        assertEquals("content://photo/1", failed.photoSource)
        assertTrue(savedNotes().isEmpty())

        importer.failure = null
        viewModel.onSave()

        assertFalse(viewModel.uiState.value.hasError)
        assertEquals(1, savedNotes().size)
    }

    @Test
    fun `saving again while a save is under way adds only one note`() = runTest {
        importer.gate = CompletableDeferred()
        viewModel.onPhotoPicked("content://photo/1")

        viewModel.onSave()
        assertTrue(viewModel.uiState.value.isSaving)
        assertFalse(viewModel.uiState.value.canSave)
        viewModel.onSave()
        importer.gate?.complete(Unit)

        assertEquals(1, savedNotes().size)
        assertEquals(listOf("content://photo/1"), importer.imported)
    }

    @Test
    fun `a taken photo becomes the chosen photo`() {
        val target = viewModel.onTakePhotoRequested()

        viewModel.onPhotoTaken(taken = true)

        assertEquals(listOf(target), cameraFiles.created)
        assertEquals(target, viewModel.uiState.value.photoSource)
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `backing out of the camera discards the target and keeps the form`() {
        viewModel.onPhotoPicked("content://photo/1")
        val target = viewModel.onTakePhotoRequested()

        viewModel.onPhotoTaken(taken = false)

        assertEquals(listOf(target), cameraFiles.discarded)
        assertEquals("content://photo/1", viewModel.uiState.value.photoSource)
    }

    @Test
    fun `a camera result with no pending request is ignored`() {
        viewModel.onPhotoTaken(taken = true)

        assertEquals(AddNoteUiState(), viewModel.uiState.value)
        assertTrue(cameraFiles.discarded.isEmpty())
    }

    @Test
    fun `a camera result is only used once`() {
        viewModel.onTakePhotoRequested()
        viewModel.onPhotoTaken(taken = true)
        viewModel.onPhotoPicked("content://photo/1")

        viewModel.onPhotoTaken(taken = true)

        assertEquals("content://photo/1", viewModel.uiState.value.photoSource)
    }

    @Test
    fun `asking for the camera again discards the unanswered target`() {
        val first = viewModel.onTakePhotoRequested()

        viewModel.onTakePhotoRequested()

        assertEquals(listOf(first), cameraFiles.discarded)
    }

    @Test
    fun `replacing the photo discards the one it replaces`() {
        viewModel.onPhotoPicked("content://photo/1")

        viewModel.onPhotoPicked("content://photo/2")

        assertEquals(listOf("content://photo/1"), cameraFiles.discarded)
    }

    @Test
    fun `choosing the same photo again discards nothing`() {
        viewModel.onPhotoPicked("content://photo/1")

        viewModel.onPhotoPicked("content://photo/1")

        assertTrue(cameraFiles.discarded.isEmpty())
    }

    @Test
    fun `saving discards the capture once the note has its own copy`() = runTest {
        viewModel.onPhotoPicked("content://photo/1")

        viewModel.onSave()

        assertEquals(listOf("content://photo/1"), cameraFiles.discarded)
    }

    @Test
    fun `a failed save keeps the capture so the user can retry`() = runTest {
        viewModel.onPhotoPicked("content://photo/1")
        importer.failure = IOException("no space")

        viewModel.onSave()

        assertTrue(cameraFiles.discarded.isEmpty())
    }

    @Test
    fun `the pending camera request survives the process being recreated`() {
        val target = viewModel.onTakePhotoRequested()

        // A new ViewModel over the same saved state is what Android builds after killing the process.
        val restored = AddNoteViewModel(repository, importer, cameraFiles, savedStateHandle)
        restored.onPhotoTaken(taken = true)

        assertEquals(target, restored.uiState.value.photoSource)
    }
}

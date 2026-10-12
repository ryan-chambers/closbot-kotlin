package com.ryanthink.closbotkt.feature.notes

import androidx.lifecycle.SavedStateHandle
import com.ryanthink.closbotkt.MainDispatcherRule
import com.ryanthink.closbotkt.data.notes.FakeWineNoteRepository
import com.ryanthink.closbotkt.data.notes.NewWineNote
import com.ryanthink.closbotkt.data.notes.PhotoStorage
import com.ryanthink.closbotkt.data.notes.WineNote
import com.ryanthink.closbotkt.data.notes.WineNoteRepository
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The spec for [EditNoteViewModel]. It expects:
 * - the constructor `EditNoteViewModel(repository, photoStorage, savedStateHandle)`, with the note's id stored in the
 *   handle under `"noteId"` (the name of [com.ryanthink.closbotkt.app.EditNoteRoute]'s property);
 * - `uiState: StateFlow<EditNoteUiState>`, a MutableStateFlow behind `asStateFlow()` as in `AddNoteViewModel`, so its
 *   `value` is current as soon as it is read. The note is loaded when the ViewModel is created;
 * - `EditNoteUiState` is one of `Loading`, `NotFound`, or `Editing(photoFileName, details, labelsText, isBusy = false,
 *   hasError = false, isFinished = false)`;
 * - the events `onDetailsChange`, `onLabelsChange`, `onSave`, `onDeleteConfirmed` and `onFinishedHandled`.
 *
 * `isBusy` is true while a save or delete is running. `isFinished` is the one-shot "leave this screen" signal, like
 * `AddNoteUiState.isSaved`.
 */
class EditNoteViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeWineNoteRepository()

    private suspend fun savedNote(
        photo: String = "a.jpg",
        details: String = "Lovely with fish",
        labels: List<String> = emptyList(),
    ): Long = repository.addNote(NewWineNote(photo, details, labels))

    private fun viewModelFor(noteId: Long, repository: WineNoteRepository = this.repository) =
        EditNoteViewModel(repository, PhotoStorage(File("photos")), SavedStateHandle(mapOf("noteId" to noteId)))

    private fun EditNoteViewModel.editing() = uiState.value as EditNoteUiState.Editing

    /** A repository that behaves like [repository] except where a test overrides a call. */
    private fun repositoryWith(
        update: suspend (WineNote) -> Unit = repository::updateNote,
        delete: suspend (WineNote) -> Unit = repository::deleteNote,
    ): WineNoteRepository =
        object : WineNoteRepository by repository {
            override suspend fun updateNote(note: WineNote) = update(note)

            override suspend fun deleteNote(note: WineNote) = delete(note)
        }

    @Test
    fun `shows the saved note in the form`() = runTest {
        val id = savedNote(photo = "wine.jpg", details = "Lovely with fish", labels = listOf("Chablis", "Chardonnay"))

        val form = viewModelFor(id).editing()

        assertEquals("wine.jpg", form.photoFileName)
        assertEquals("Lovely with fish", form.details)
        assertEquals("Chablis, Chardonnay", form.labelsText)
        assertFalse(form.isBusy)
        assertFalse(form.hasError)
        assertFalse(form.isFinished)
    }

    @Test
    fun `is loading until the note arrives`() = runTest {
        val id = savedNote()
        val arrival = CompletableDeferred<Unit>()
        val slow =
            object : WineNoteRepository by repository {
                override suspend fun getNote(id: Long): WineNote? {
                    arrival.await()
                    return repository.getNote(id)
                }
            }

        val viewModel = viewModelFor(id, slow)
        assertEquals(EditNoteUiState.Loading, viewModel.uiState.value)

        arrival.complete(Unit)
        assertTrue(viewModel.uiState.value is EditNoteUiState.Editing)
    }

    @Test
    fun `a note that no longer exists is reported as not found`() = runTest {
        assertEquals(EditNoteUiState.NotFound, viewModelFor(noteId = 999).uiState.value)
    }

    @Test
    fun `editing changes the form but saves nothing until asked`() = runTest {
        val id = savedNote(details = "Lovely with fish", labels = listOf("Chablis"))
        val viewModel = viewModelFor(id)

        viewModel.onDetailsChange("Better than I remembered")
        viewModel.onLabelsChange("Chablis, Premier Cru")

        assertEquals("Better than I remembered", viewModel.editing().details)
        assertEquals("Chablis, Premier Cru", viewModel.editing().labelsText)
        val stored = repository.getNote(id)!!
        assertEquals("Lovely with fish", stored.details)
        assertEquals(listOf("Chablis"), stored.labels)
    }

    @Test
    fun `saving stores the trimmed details and parsed labels, then finishes`() = runTest {
        val id = savedNote(details = "Lovely with fish", labels = listOf("Chablis"))
        val viewModel = viewModelFor(id)
        viewModel.onDetailsChange("  Better than I remembered  ")
        viewModel.onLabelsChange("Chablis, , Premier Cru, Chablis")

        viewModel.onSave()

        val stored = repository.getNote(id)!!
        assertEquals("Better than I remembered", stored.details)
        assertEquals(listOf("Chablis", "Premier Cru"), stored.labels)
        assertTrue(viewModel.editing().isFinished)
        assertFalse(viewModel.editing().isBusy)
    }

    @Test
    fun `saving keeps the note's photo, id and date`() = runTest {
        val id = savedNote(photo = "wine.jpg")
        val before = repository.getNote(id)!!
        val viewModel = viewModelFor(id)
        viewModel.onDetailsChange("Changed")

        viewModel.onSave()

        val after = repository.getNote(id)!!
        assertEquals(before.id, after.id)
        assertEquals("wine.jpg", after.photoFileName)
        assertEquals(before.createdAt, after.createdAt)
    }

    @Test
    fun `saving twice at once saves once`() = runTest {
        val id = savedNote()
        val release = CompletableDeferred<Unit>()
        val updates = mutableListOf<WineNote>()
        val slow =
            repositoryWith(
                update = { note ->
                    updates += note
                    release.await()
                    repository.updateNote(note)
                }
            )
        val viewModel = viewModelFor(id, slow)

        viewModel.onSave()
        assertTrue(viewModel.editing().isBusy)
        viewModel.onSave()
        release.complete(Unit)

        assertEquals(1, updates.size)
        assertFalse(viewModel.editing().isBusy)
    }

    @Test
    fun `deleting removes the note, then finishes`() = runTest {
        val id = savedNote()
        val other = savedNote(photo = "b.jpg")
        val viewModel = viewModelFor(id)

        viewModel.onDeleteConfirmed()

        assertNull(repository.getNote(id))
        assertNotNull(repository.getNote(other))
        assertTrue(viewModel.editing().isFinished)
    }

    @Test
    fun `a failed delete keeps the form, reports the error and can be retried`() = runTest {
        val id = savedNote(details = "Lovely with fish")
        var failing = true
        val flaky =
            repositoryWith(
                delete = { note ->
                    if (failing) throw IOException("disk trouble")
                    repository.deleteNote(note)
                }
            )
        val viewModel = viewModelFor(id, flaky)

        viewModel.onDeleteConfirmed()

        val afterFailure = viewModel.editing()
        assertTrue(afterFailure.hasError)
        assertFalse(afterFailure.isBusy)
        assertFalse(afterFailure.isFinished)
        assertEquals("Lovely with fish", afterFailure.details)

        failing = false
        viewModel.onDeleteConfirmed()

        assertNull(repository.getNote(id))
        assertTrue(viewModel.editing().isFinished)
        assertFalse(viewModel.editing().hasError)
    }

    @Test
    fun `finishing is signalled once`() = runTest {
        val viewModel = viewModelFor(savedNote())
        viewModel.onSave()

        viewModel.onFinishedHandled()

        assertFalse(viewModel.editing().isFinished)
    }

    @Test
    fun `events for a missing note do nothing`() = runTest {
        val viewModel = viewModelFor(noteId = 999)

        viewModel.onDetailsChange("Ghost")
        viewModel.onSave()
        viewModel.onDeleteConfirmed()
        viewModel.onFinishedHandled()

        assertEquals(EditNoteUiState.NotFound, viewModel.uiState.value)
    }
}

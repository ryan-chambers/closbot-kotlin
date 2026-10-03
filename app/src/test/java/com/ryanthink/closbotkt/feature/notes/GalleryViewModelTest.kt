package com.ryanthink.closbotkt.feature.notes

import app.cash.turbine.test
import com.ryanthink.closbotkt.MainDispatcherRule
import com.ryanthink.closbotkt.data.notes.FakeWineNoteRepository
import com.ryanthink.closbotkt.data.notes.NewWineNote
import com.ryanthink.closbotkt.data.notes.WineNote
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GalleryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeWineNoteRepository()
    private val viewModel = GalleryViewModel(repository)

    private suspend fun save(photo: String, vararg labels: String) =
        repository.addNote(NewWineNote(photo, details = "", labels = labels.toList()))

    // The fake's newest-first order is unspecified when notes are saved in the same millisecond, so
    // compare photo names as a set; ordering is the repository's concern, not the ViewModel's.
    private fun GalleryUiState.photos(): Set<String> = notes.map(WineNote::photoFileName).toSet()

    @Test
    fun `starts loading, then reports an empty gallery`() = runTest {
        viewModel.uiState.test {
            assertTrue(awaitItem().isLoading)

            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertFalse(loaded.hasAnyNotes)
            assertTrue(loaded.notes.isEmpty())
        }
    }

    @Test
    fun `saved notes appear and new ones are picked up`() = runTest {
        save("a.jpg")

        viewModel.uiState.test {
            awaitItem() // loading
            assertEquals(setOf("a.jpg"), awaitItem().photos())

            save("b.jpg")

            assertEquals(setOf("a.jpg", "b.jpg"), awaitItem().photos())
        }
    }

    @Test
    fun `search keeps notes with a label containing the term, ignoring case`() = runTest {
        save("a.jpg", "Chablis", "Chardonnay")
        save("b.jpg", "Merlot")

        viewModel.uiState.test {
            awaitItem() // loading
            awaitItem() // all notes

            viewModel.onSearchTermChange("chard")

            val state = awaitItem()
            assertEquals(setOf("a.jpg"), state.photos())
            assertEquals("chard", state.searchTerm)
            assertTrue(state.hasAnyNotes)
        }
    }

    @Test
    fun `search offers matching labels as suggestions`() = runTest {
        save("a.jpg", "Chablis", "Chardonnay")
        save("b.jpg", "Merlot", "Chianti")

        viewModel.uiState.test {
            awaitItem() // loading
            awaitItem() // all notes

            viewModel.onSearchTermChange(" ch ")

            assertEquals(listOf("Chablis", "Chardonnay", "Chianti"), awaitItem().matchingLabels)
        }
    }

    @Test
    fun `selecting a tag filters by that exact label and clears the search`() = runTest {
        save("a.jpg", "Chablis")
        save("b.jpg", "Chablis Premier Cru")

        viewModel.uiState.test {
            awaitItem() // loading
            awaitItem() // all notes
            viewModel.onSearchTermChange("chab")
            awaitItem()

            viewModel.onTagSelected("Chablis")

            val state = awaitItem()
            assertEquals("Chablis", state.selectedTag)
            assertEquals("", state.searchTerm)
            assertEquals(setOf("a.jpg"), state.photos())
            assertTrue(state.matchingLabels.isEmpty())
        }
    }

    @Test
    fun `selecting the same tag again clears it`() = runTest {
        save("a.jpg", "Chablis")
        save("b.jpg", "Merlot")

        viewModel.uiState.test {
            awaitItem() // loading
            awaitItem() // all notes
            viewModel.onTagSelected("Chablis")
            awaitItem()

            viewModel.onTagSelected("Chablis")

            val state = awaitItem()
            assertEquals(null, state.selectedTag)
            assertEquals(setOf("a.jpg", "b.jpg"), state.photos())
        }
    }

    @Test
    fun `a tag no note carries any more is dropped`() = runTest {
        val id = save("a.jpg", "Chablis")
        save("b.jpg", "Merlot")

        viewModel.uiState.test {
            awaitItem() // loading
            awaitItem() // all notes
            viewModel.onTagSelected("Chablis")
            awaitItem()

            repository.deleteNote(requireNotNull(repository.getNote(id)))

            val state = awaitItem()
            assertEquals(null, state.selectedTag)
            assertEquals(setOf("b.jpg"), state.photos())
        }
    }

    @Test
    fun `a search with no matches still reports that notes exist`() = runTest {
        save("a.jpg", "Chablis")

        viewModel.uiState.test {
            awaitItem() // loading
            awaitItem() // all notes

            viewModel.onSearchTermChange("zinfandel")

            val state = awaitItem()
            assertTrue(state.notes.isEmpty())
            assertTrue(state.hasAnyNotes)
        }
    }
}

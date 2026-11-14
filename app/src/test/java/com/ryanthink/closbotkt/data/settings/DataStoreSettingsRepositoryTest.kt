package com.ryanthink.closbotkt.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Specifies [DataStoreSettingsRepository] against a real DataStore file in a temporary folder.
 *
 * Nothing is faked: the point of this class is that values really reach disk, and a fake store would hide a repository
 * that only remembers things in memory.
 */
class DataStoreSettingsRepositoryTest {

    @get:Rule val temporaryFolder = TemporaryFolder()

    private lateinit var file: File
    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        file = File(temporaryFolder.root, "settings.preferences_pb")
        scope = newScope()
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    // DataStore allows one active instance per file, so each "app launch" gets its own scope.
    private fun newScope() = CoroutineScope(Dispatchers.IO + Job())

    private fun dataStore(scope: CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope) { file }

    private fun repository() = DataStoreSettingsRepository(dataStore(scope))

    @Test
    fun `burgundy focus is on before anything has been stored`() = runTest {
        assertTrue(repository().burgundyFocus.first())
    }

    @Test
    fun `burgundy focus can be turned off`() = runTest {
        val repository = repository()

        repository.setBurgundyFocus(false)

        assertFalse(repository.burgundyFocus.first())
    }

    @Test
    fun `burgundy focus can be turned back on`() = runTest {
        val repository = repository()
        repository.setBurgundyFocus(false)

        repository.setBurgundyFocus(true)

        assertTrue(repository.burgundyFocus.first())
    }

    @Test
    fun `observers are told when burgundy focus changes`() = runTest {
        val repository = repository()

        repository.burgundyFocus.test {
            assertTrue(awaitItem())

            repository.setBurgundyFocus(false)
            assertFalse(awaitItem())

            repository.setBurgundyFocus(true)
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `burgundy focus survives the app being restarted`() = runBlocking {
        DataStoreSettingsRepository(dataStore(scope)).setBurgundyFocus(false)
        scope.cancel()

        val afterRestart = DataStoreSettingsRepository(dataStore(newScope().also { scope = it }))

        assertFalse(afterRestart.burgundyFocus.first())
    }
}

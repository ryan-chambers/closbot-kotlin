package com.ryanthink.closbotkt.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.io.File
import java.util.UUID
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Replaces [SettingsModule] in every `@HiltAndroidTest`.
 *
 * Each test gets its own Hilt component, so each builds its own DataStore, but all of them would open the same file on
 * the device, and DataStore refuses to have two instances on one file. Giving every test a file of its own also starts
 * each one from the default settings, whatever the app on the device has stored. The real [DataStoreSettingsRepository]
 * is still used, so the tests exercise it too.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [SettingsModule::class])
abstract class TestSettingsModule {

    @Binds abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    companion object {
        @Provides
        @Singleton
        fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + SupervisorJob())) {
                File(context.cacheDir, "test-settings-${UUID.randomUUID()}.preferences_pb")
            }
    }
}

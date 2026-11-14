package com.ryanthink.closbotkt.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Keeps settings in a Preferences [DataStore] so they survive the app being closed. */
class DataStoreSettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) :
    SettingsRepository {

    val burgundyFocusKey = booleanPreferencesKey("burgundyFocus")

    override val burgundyFocus: Flow<Boolean>
        get() = dataStore.data.map { preferences -> preferences[burgundyFocusKey] ?: true }

    override suspend fun setBurgundyFocus(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[burgundyFocusKey] = enabled
        }
    }
}

package com.ryanthink.closbotkt.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Keeps settings in a Preferences [DataStore] so they survive the app being closed. */
class DataStoreSettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) :
    SettingsRepository {

    override val burgundyFocus: Flow<Boolean>
        get() = TODO("Read the stored value, defaulting to true when nothing has been stored")

    override suspend fun setBurgundyFocus(enabled: Boolean) {
        TODO("Store the value")
    }
}

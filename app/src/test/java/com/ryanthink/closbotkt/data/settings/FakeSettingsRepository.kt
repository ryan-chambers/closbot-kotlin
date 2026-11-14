package com.ryanthink.closbotkt.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** For ViewModel and repository tests: keeps settings in memory, so nothing touches DataStore or disk. */
class FakeSettingsRepository(burgundyFocus: Boolean = true) : SettingsRepository {

    private val burgundyFocusState = MutableStateFlow(burgundyFocus)

    override val burgundyFocus: Flow<Boolean> = burgundyFocusState

    override suspend fun setBurgundyFocus(enabled: Boolean) {
        burgundyFocusState.value = enabled
    }
}

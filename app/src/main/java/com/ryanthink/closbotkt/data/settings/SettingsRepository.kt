package com.ryanthink.closbotkt.data.settings

import kotlinx.coroutines.flow.Flow

/** User preferences that outlive the app process. */
interface SettingsRepository {

    /**
     * Whether answers should draw on Burgundy knowledge: personal notes and wine facts retrieved from the knowledge
     * base. On until the user turns it off, including on first launch.
     *
     * A [Flow] rather than a plain value so screens and the assistant see a change as soon as it is made.
     */
    val burgundyFocus: Flow<Boolean>

    suspend fun setBurgundyFocus(enabled: Boolean)
}

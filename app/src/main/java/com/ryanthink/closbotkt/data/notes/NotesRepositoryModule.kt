package com.ryanthink.closbotkt.data.notes

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

/**
 * Notes are stored in Room in every flavor, so unlike the assistant binding this one lives in
 * `main` rather than being swapped per flavor.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotesRepositoryModule {

    @Binds
    abstract fun bindWineNoteRepository(impl: RoomWineNoteRepository): WineNoteRepository

    companion object {
        /** Injected rather than calling `Instant.now()` directly so tests can pin the time. */
        @Provides
        fun provideClock(): Clock = Clock.systemUTC()
    }
}

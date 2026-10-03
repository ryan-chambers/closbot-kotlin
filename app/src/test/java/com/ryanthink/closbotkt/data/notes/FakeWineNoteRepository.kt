package com.ryanthink.closbotkt.data.notes

import java.time.Clock
import kotlinx.coroutines.flow.Flow

/**
 * For ViewModel tests: a [WineNoteRepository] backed by [FakeWineNoteDao], so it behaves like the
 * real one (ids assigned, newest first, [observeNotes] re-emitting) without a database.
 */
class FakeWineNoteRepository(
    clock: Clock = Clock.systemUTC(),
) : WineNoteRepository by RoomWineNoteRepository(FakeWineNoteDao(), clock)

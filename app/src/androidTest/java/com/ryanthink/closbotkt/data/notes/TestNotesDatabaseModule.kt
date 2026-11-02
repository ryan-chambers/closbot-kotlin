package com.ryanthink.closbotkt.data.notes

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Replaces [NotesDatabaseModule] in every `@HiltAndroidTest`, so those tests run against an empty database that lives
 * in memory and disappears with the test, instead of the notes saved on the device.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [NotesDatabaseModule::class])
object TestNotesDatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

    @Provides fun provideWineNoteDao(database: AppDatabase): WineNoteDao = database.wineNoteDao()
}

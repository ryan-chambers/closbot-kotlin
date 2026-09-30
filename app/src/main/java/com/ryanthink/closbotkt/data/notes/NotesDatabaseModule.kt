package com.ryanthink.closbotkt.data.notes

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Wires the single [AppDatabase] instance and its DAO for injection. */
@Module
@InstallIn(SingletonComponent::class)
object NotesDatabaseModule {

    private const val DATABASE_NAME = "wine-notes.db"

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME).build()

    @Provides
    fun provideWineNoteDao(database: AppDatabase): WineNoteDao = database.wineNoteDao()
}

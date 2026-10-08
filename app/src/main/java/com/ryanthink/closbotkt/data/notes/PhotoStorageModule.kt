package com.ryanthink.closbotkt.data.notes

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

/**
 * Photos go in the app's private `filesDir`: other apps can't read them and no storage permission
 * is needed. They are removed on uninstall, which is the same lifetime as the notes database.
 */
@Module
@InstallIn(SingletonComponent::class)
object PhotoStorageModule {

    private const val PHOTOS_DIRECTORY = "photos"

    @Provides
    @Singleton
    fun providePhotoStorage(@ApplicationContext context: Context): PhotoStorage =
        PhotoStorage(File(context.filesDir, PHOTOS_DIRECTORY))
}

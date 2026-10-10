package com.ryanthink.closbotkt.data.notes

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads photos through the [android.content.ContentResolver], which is how the Photo Picker (and
 * later the camera) hands them over: the app gets a temporary right to read one URI, not a file path.
 */
class ContentResolverPhotoImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoStorage: PhotoStorage,
) : PhotoImporter {

    // Copying a photo is disk work, so it runs on the IO dispatcher rather than the main thread.
    override suspend fun importPhoto(source: String): String = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(Uri.parse(source))
            ?: throw IOException("Could not open $source")
        photoStorage.copyIn(input)
    }
}

package com.ryanthink.closbotkt.data.notes

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * Captures live in a `camera` folder inside the app's cache directory, which the system may clear
 * when space is short. That suits them: a capture only matters until the note is saved, and then
 * [ContentResolverPhotoImporter] copies it into permanent storage.
 *
 * The URIs come from a [FileProvider] declared in the manifest; `res/xml/file_paths.xml` says which
 * folder it may share.
 */
class FileProviderCameraPhotoFiles @Inject constructor(
    @ApplicationContext private val context: Context,
) : CameraPhotoFiles {

    private val directory = File(context.cacheDir, CAMERA_DIRECTORY)
    private val authority = "${context.packageName}$AUTHORITY_SUFFIX"

    // Creating an empty file is quick enough for the main thread. The slow part, copying the bytes,
    // happens later in the importer, on the IO dispatcher.
    override fun createTarget(): String {
        directory.mkdirs()
        val file = File.createTempFile("capture-", ".jpg", directory)
        return FileProvider.getUriForFile(context, authority, file).toString()
    }

    override fun discard(source: String) {
        val uri = Uri.parse(source)
        // Only our own provider's URIs; the provider's delete removes the file behind the URI.
        if (uri.authority == authority) context.contentResolver.delete(uri, null, null)
    }

    private companion object {
        const val CAMERA_DIRECTORY = "camera"
        const val AUTHORITY_SUFFIX = ".fileprovider"
    }
}

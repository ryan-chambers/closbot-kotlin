package com.ryanthink.closbotkt.data.notes

import java.io.IOException

/** Brings a photo the user chose into the app's own photo directory. */
interface PhotoImporter {

    /**
     * Copies the photo at [source] and returns the file name to store on the note.
     *
     * [source] is a content URI as text. It is a string, not an `android.net.Uri`, so that
     * ViewModels using this interface can be unit tested on the JVM, where `Uri` can't be built.
     *
     * @throws IOException if the photo can't be read or written
     */
    @Throws(IOException::class)
    suspend fun importPhoto(source: String): String
}

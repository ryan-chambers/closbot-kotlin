package com.ryanthink.closbotkt.data.notes

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID

/**
 * Where note photos live on disk. A note stores only the file name ([WineNote.photoFileName]), so
 * the directory can move without touching the database.
 */
class PhotoStorage(private val directory: File) {

    fun fileFor(photoFileName: String): File = File(directory, photoFileName)

    /**
     * Copies [source] into the photo directory under a new unique name, closes it, and returns the
     * name. A failed copy leaves no partial file behind.
     *
     * The name has no extension: the app never trusts it to say what the bytes are, and the image
     * loader works that out from the content.
     */
    @Throws(IOException::class)
    fun copyIn(source: InputStream): String {
        directory.mkdirs()
        val fileName = UUID.randomUUID().toString()
        val file = fileFor(fileName)
        try {
            source.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
        } catch (e: IOException) {
            file.delete()
            throw e
        }
        return fileName
    }
}

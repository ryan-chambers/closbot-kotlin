package com.ryanthink.closbotkt.data.notes

import java.io.File

/**
 * Where note photos live on disk. A note stores only the file name ([WineNote.photoFileName]), so
 * the directory can move without touching the database.
 */
class PhotoStorage(private val directory: File) {

    fun fileFor(photoFileName: String): File = File(directory, photoFileName)
}

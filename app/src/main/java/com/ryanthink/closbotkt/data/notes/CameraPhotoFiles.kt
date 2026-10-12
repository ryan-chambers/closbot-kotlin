package com.ryanthink.closbotkt.data.notes

/**
 * The files the camera app writes photos into before a note is saved.
 *
 * The camera runs in another process, so it can't write to our private storage directly. Each capture gets an empty
 * file, shared with the camera app through a `content://` URI that grants write access to just that file.
 */
interface CameraPhotoFiles {

    /** Creates an empty file for the camera to fill and returns its content URI as text. */
    fun createTarget(): String

    /**
     * Deletes a capture once it isn't needed, so abandoned photos don't pile up. Safe to call with anything: a URI that
     * isn't one of ours, or a capture that's already gone, is ignored. That lets callers discard whatever photo they
     * are dropping without checking where it came from.
     */
    fun discard(source: String)
}

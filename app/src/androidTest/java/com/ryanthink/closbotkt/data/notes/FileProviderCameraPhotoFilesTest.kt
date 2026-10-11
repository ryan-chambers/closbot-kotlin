package com.ryanthink.closbotkt.data.notes

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileNotFoundException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Runs on a device because it goes through the real [androidx.core.content.FileProvider] declared in the manifest.
 * These tests fail until that provider and `res/xml/file_paths.xml` exist.
 */
class FileProviderCameraPhotoFilesTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val contentResolver = context.contentResolver
    private val cameraFiles = FileProviderCameraPhotoFiles(context)

    @Test
    fun createsAContentUriOnTheAppsOwnProvider() {
        val target = Uri.parse(cameraFiles.createTarget())

        assertEquals("content", target.scheme)
        assertEquals("${context.packageName}.fileprovider", target.authority)
    }

    @Test
    fun theTargetCanBeWrittenAndReadBackThroughItsUri() {
        val target = Uri.parse(cameraFiles.createTarget())

        contentResolver.openOutputStream(target)!!.use { it.write(byteArrayOf(1, 2, 3)) }
        val read = contentResolver.openInputStream(target)!!.use { it.readBytes() }

        assertTrue(byteArrayOf(1, 2, 3).contentEquals(read))
    }

    @Test
    fun everyTargetIsADifferentFile() {
        assertTrue(cameraFiles.createTarget() != cameraFiles.createTarget())
    }

    @Test
    fun discardingATargetDeletesItsFile() {
        val target = cameraFiles.createTarget()

        cameraFiles.discard(target)

        val failure = runCatching { contentResolver.openInputStream(Uri.parse(target))!!.close() }.exceptionOrNull()
        assertTrue(failure is FileNotFoundException)
    }

    @Test
    fun discardingTwiceIsHarmless() {
        val target = cameraFiles.createTarget()

        cameraFiles.discard(target)
        cameraFiles.discard(target)
    }

    @Test
    fun discardingAForeignUriDoesNothing() {
        // Would throw if it were passed to a provider that doesn't know it.
        cameraFiles.discard("content://media/picker/0/com.android.providers.media.photopicker/media/1")
    }
}

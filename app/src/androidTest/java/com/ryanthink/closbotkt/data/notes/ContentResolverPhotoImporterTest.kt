package com.ryanthink.closbotkt.data.notes

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Runs on a device because it needs a real [android.content.ContentResolver]. */
class ContentResolverPhotoImporterTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val storage by lazy { PhotoStorage(File(temporaryFolder.root, "photos")) }
    private val importer by lazy {
        ContentResolverPhotoImporter(InstrumentationRegistry.getInstrumentation().targetContext, storage)
    }

    @Test
    fun copiesThePhotoIntoPhotoStorage() = runBlocking {
        val original = temporaryFolder.newFile("picked.jpg").apply { writeBytes(byteArrayOf(9, 8, 7)) }

        val name = importer.importPhoto(Uri.fromFile(original).toString())

        assertArrayEquals(byteArrayOf(9, 8, 7), storage.fileFor(name).readBytes())
    }

    @Test
    fun failsWithAnIoExceptionWhenThePhotoCannotBeOpened() = runBlocking {
        val missing = Uri.fromFile(File(temporaryFolder.root, "gone.jpg")).toString()

        val failure = runCatching { importer.importPhoto(missing) }.exceptionOrNull()

        assertTrue(failure is IOException)
    }
}

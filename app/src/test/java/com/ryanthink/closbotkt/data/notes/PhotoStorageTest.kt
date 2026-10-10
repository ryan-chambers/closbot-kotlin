package com.ryanthink.closbotkt.data.notes

import java.io.File
import java.io.IOException
import java.io.InputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PhotoStorageTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    // A folder that doesn't exist yet, like the photo directory on first use.
    private val directory by lazy { File(temporaryFolder.root, "photos") }
    private val storage by lazy { PhotoStorage(directory) }

    @Test
    fun `fileFor puts the file name inside the photo directory`() {
        assertEquals(File(directory, "a"), storage.fileFor("a"))
    }

    @Test
    fun `copyIn writes the bytes to a file named by the result, creating the directory`() {
        val name = storage.copyIn(byteArrayOf(1, 2, 3).inputStream())

        assertEquals(listOf<Byte>(1, 2, 3), storage.fileFor(name).readBytes().toList())
    }

    @Test
    fun `each copy gets its own name`() {
        val first = storage.copyIn(byteArrayOf(1).inputStream())
        val second = storage.copyIn(byteArrayOf(1).inputStream())

        assertNotEquals(first, second)
    }

    @Test
    fun `a copy that fails leaves no partial file and passes the error on`() {
        val failing = object : InputStream() {
            override fun read(): Int = throw IOException("disk on fire")
        }

        try {
            storage.copyIn(failing)
            fail("expected an IOException")
        } catch (e: IOException) {
            assertEquals("disk on fire", e.message)
        }
        assertTrue(directory.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `copyIn closes the source`() {
        var closed = false
        val source = object : InputStream() {
            override fun read(): Int = -1
            override fun close() { closed = true }
        }

        storage.copyIn(source)

        assertTrue(closed)
        assertFalse(directory.listFiles().orEmpty().isEmpty())
    }
}

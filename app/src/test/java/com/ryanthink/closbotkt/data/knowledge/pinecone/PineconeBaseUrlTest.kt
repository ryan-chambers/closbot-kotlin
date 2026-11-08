package com.ryanthink.closbotkt.data.knowledge.pinecone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Specifies [pineconeBaseUrl]: turn the configured index host into a base URL Retrofit accepts.
 *
 * Retrofit throws unless the base URL has a scheme and ends in a slash, but the host is copied from the Pinecone
 * console or `local.properties` in whatever form it was pasted. Normalising it in one place stops a stray space or a
 * missing slash from becoming a crash at startup.
 */
class PineconeBaseUrlTest {

    @Test
    fun `adds the trailing slash Retrofit requires`() {
        assertEquals("https://my-index-abc.svc.pinecone.io/", pineconeBaseUrl("https://my-index-abc.svc.pinecone.io"))
    }

    @Test
    fun `leaves a host that already ends in a slash alone`() {
        assertEquals("https://my-index-abc.svc.pinecone.io/", pineconeBaseUrl("https://my-index-abc.svc.pinecone.io/"))
    }

    @Test
    fun `collapses several trailing slashes to one`() {
        assertEquals("https://my-index-abc.svc.pinecone.io/", pineconeBaseUrl("https://my-index-abc.svc.pinecone.io//"))
    }

    @Test
    fun `adds https when the scheme is missing`() {
        // Pinecone's console and API describe an index host without a scheme.
        assertEquals("https://my-index-abc.svc.pinecone.io/", pineconeBaseUrl("my-index-abc.svc.pinecone.io"))
    }

    @Test
    fun `keeps an explicit http scheme`() {
        // Useful for pointing the app at a local stub; the caller chose it.
        assertEquals("http://10.0.2.2:8080/", pineconeBaseUrl("http://10.0.2.2:8080"))
    }

    @Test
    fun `trims whitespace`() {
        // java.util.Properties keeps trailing spaces on a value, so local.properties can add one unseen.
        assertEquals(
            "https://my-index-abc.svc.pinecone.io/",
            pineconeBaseUrl("  https://my-index-abc.svc.pinecone.io \n"),
        )
    }

    @Test
    fun `rejects a blank host with a message that names the setting`() {
        // The build falls back to an empty string when PINECONE_HOST is not set.
        val error = assertThrows(IllegalArgumentException::class.java) { pineconeBaseUrl("  ") }

        assertEquals(true, error.message.orEmpty().contains("PINECONE_HOST"))
    }
}

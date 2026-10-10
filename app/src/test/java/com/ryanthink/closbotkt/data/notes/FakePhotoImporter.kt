package com.ryanthink.closbotkt.data.notes

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred

/** For ViewModel tests: records what was imported and can be told to fail or to wait. */
class FakePhotoImporter : PhotoImporter {
    val imported = mutableListOf<String>()

    /** When set, imports throw it. */
    var failure: IOException? = null

    /** When set, imports suspend until it completes, to test what happens mid-save. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun importPhoto(source: String): String {
        gate?.await()
        failure?.let { throw it }
        imported += source
        return "photo-${imported.size}"
    }
}

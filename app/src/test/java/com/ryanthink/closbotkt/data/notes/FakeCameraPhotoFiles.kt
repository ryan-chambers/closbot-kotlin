package com.ryanthink.closbotkt.data.notes

/** For ViewModel tests: hands out predictable targets and records which were discarded. */
class FakeCameraPhotoFiles : CameraPhotoFiles {
    val created = mutableListOf<String>()
    val discarded = mutableListOf<String>()

    override fun createTarget(): String = "content://camera/${created.size + 1}".also { created += it }

    override fun discard(source: String) {
        discarded += source
    }
}

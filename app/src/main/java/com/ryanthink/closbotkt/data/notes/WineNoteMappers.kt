package com.ryanthink.closbotkt.data.notes

import java.time.Instant

internal fun WineNoteEntity.toDomain() = WineNote(
    id = id,
    photoFileName = photoFileName,
    details = details,
    labels = labels,
    createdAt = createdAt,
)

internal fun WineNote.toEntity() = WineNoteEntity(
    id = id,
    photoFileName = photoFileName,
    details = details,
    labels = labels,
    createdAt = createdAt,
)

internal fun NewWineNote.toEntity(createdAt: Instant) = WineNoteEntity(
    photoFileName = photoFileName,
    details = details,
    labels = labels,
    createdAt = createdAt,
)

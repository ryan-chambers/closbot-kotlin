package com.ryanthink.closbotkt.data.notes

import androidx.room.TypeConverter
import java.time.Instant

/**
 * Room only persists primitive columns, so [WineNoteEntity.labels] and [WineNoteEntity.createdAt]
 * need explicit conversions. Labels are joined with a unit separator rather than a comma, since
 * label text is free-form and could itself contain one.
 */
class Converters {

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun fromLabels(value: List<String>): String = value.joinToString(LABEL_SEPARATOR)

    @TypeConverter
    fun toLabels(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(LABEL_SEPARATOR)

    private companion object {
        const val LABEL_SEPARATOR = "\u001F"
    }
}

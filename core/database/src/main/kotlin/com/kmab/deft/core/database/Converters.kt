package com.kmab.deft.core.database

import androidx.room.TypeConverter
import com.kmab.deft.core.model.BookFormat
import java.time.Instant

class Converters {
    @TypeConverter fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter fun fromFormat(value: BookFormat): String = value.name

    @TypeConverter fun toFormat(value: String): BookFormat = BookFormat.valueOf(value)
}

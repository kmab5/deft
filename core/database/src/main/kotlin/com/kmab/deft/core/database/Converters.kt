package com.kmab.deft.core.database

import androidx.room.TypeConverter
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.ReadThroughOutcome
import java.time.Instant

class Converters {
    @TypeConverter fun fromOutcome(value: ReadThroughOutcome): String = value.name

    @TypeConverter fun toOutcome(value: String): ReadThroughOutcome = ReadThroughOutcome.valueOf(value)

    @TypeConverter fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter fun fromFormat(value: BookFormat): String = value.name

    @TypeConverter fun toFormat(value: String): BookFormat = BookFormat.valueOf(value)
}

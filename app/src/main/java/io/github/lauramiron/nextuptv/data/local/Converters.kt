package io.github.lauramiron.nextuptv.data.local

import androidx.room.TypeConverter
import io.github.lauramiron.nextuptv.data.local.entity.ArtworkType
import io.github.lauramiron.nextuptv.data.local.entity.CreditRole
import io.github.lauramiron.nextuptv.data.local.entity.StreamingService
import io.github.lauramiron.nextuptv.data.local.entity.TitleKind
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTitleKind(v: TitleKind) = v.name
    @TypeConverter fun toTitleKind(s: String) = TitleKind.valueOf(s)

    @TypeConverter fun fromArtworkType(v: ArtworkType) = v.name
    @TypeConverter fun toArtworkType(s: String) = ArtworkType.valueOf(s)

    @TypeConverter fun fromCreditRole(v: CreditRole) = v.name
    @TypeConverter fun toCreditRole(s: String) = CreditRole.valueOf(s)

    @TypeConverter
    fun fromStreamingProvider(provider: StreamingService) = provider.id
    @TypeConverter
    fun toStreamingProvider(value: String): StreamingService {
        return StreamingService.fromString(value)
            ?: throw IllegalArgumentException("Unknown StreamingProvider: $value")
    }

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

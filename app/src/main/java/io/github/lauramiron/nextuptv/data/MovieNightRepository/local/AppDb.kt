package io.github.lauramiron.nextuptv.data.MovieNightRepository.local

import androidx.databinding.adapters.Converters
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TitleEntity::class, SeasonEntity::class, EpisodeEntity::class,
        ArtworkEntity::class, GenreEntity::class, TitleGenreCrossRef::class,
        PersonEntity::class, CreditEntity::class, ExternalIdEntity::class
    ],
    version = 1, exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun titleDao(): TitleDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun externalIdDao(): ExternalIdDao
}
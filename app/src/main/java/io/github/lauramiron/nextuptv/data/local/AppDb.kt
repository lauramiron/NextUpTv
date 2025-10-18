package io.github.lauramiron.nextuptv.data.local

import androidx.databinding.adapters.Converters
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.lauramiron.nextuptv.data.local.dao.EpisodeDao
import io.github.lauramiron.nextuptv.data.local.dao.ExternalIdDao
import io.github.lauramiron.nextuptv.data.local.dao.ResumeDao
import io.github.lauramiron.nextuptv.data.local.dao.TitleDao
import io.github.lauramiron.nextuptv.data.local.entity.ArtworkEntity
import io.github.lauramiron.nextuptv.data.local.entity.CreditEntity
import io.github.lauramiron.nextuptv.data.local.entity.EpisodeEntity
import io.github.lauramiron.nextuptv.data.local.entity.ExternalIdEntity
import io.github.lauramiron.nextuptv.data.local.entity.GenreEntity
import io.github.lauramiron.nextuptv.data.local.entity.PersonEntity
import io.github.lauramiron.nextuptv.data.local.entity.SeasonEntity
import io.github.lauramiron.nextuptv.data.local.entity.TitleEntity
import io.github.lauramiron.nextuptv.data.local.entity.TitleGenreCrossRef

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

    abstract fun resumeDao(): ResumeDao
}
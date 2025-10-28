package io.github.lauramiron.nextuptv.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
//import io.github.lauramiron.nextuptv.data.local.dao.EpisodeDao
import io.github.lauramiron.nextuptv.data.local.dao.ExternalIdDao
import io.github.lauramiron.nextuptv.data.local.dao.GenreDao
import io.github.lauramiron.nextuptv.data.local.dao.PersonDao
import io.github.lauramiron.nextuptv.data.local.dao.PopularityDao
import io.github.lauramiron.nextuptv.data.local.dao.ResumeDao
import io.github.lauramiron.nextuptv.data.local.dao.TitleDao
import io.github.lauramiron.nextuptv.data.local.dao.TitleGenreCrossRefDao
import io.github.lauramiron.nextuptv.data.local.dao.TitlePersonCrossRefDao
import io.github.lauramiron.nextuptv.data.local.entity.TitlePersonCrossRef
import io.github.lauramiron.nextuptv.data.local.entity.EpisodeEntity
import io.github.lauramiron.nextuptv.data.local.entity.ExternalIdEntity
import io.github.lauramiron.nextuptv.data.local.entity.GenreEntity
import io.github.lauramiron.nextuptv.data.local.entity.PersonEntity
import io.github.lauramiron.nextuptv.data.local.entity.PopularityEntity
import io.github.lauramiron.nextuptv.data.local.entity.TitleEntity
import io.github.lauramiron.nextuptv.data.local.entity.TitleGenreCrossRef

@Database(
    entities = [
        TitleEntity::class, EpisodeEntity::class,
        GenreEntity::class, TitleGenreCrossRef::class,
        PersonEntity::class, TitlePersonCrossRef::class, ExternalIdEntity::class,
        PopularityEntity::class
    ],
    version = 1, exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun titleDao(): TitleDao
//    abstract fun episodeDao(): EpisodeDao
    abstract fun externalIdDao(): ExternalIdDao
    abstract fun genreDao(): GenreDao
    abstract fun personDao(): PersonDao
    abstract fun titleGenreDao(): TitleGenreCrossRefDao
    abstract fun titlePersonDao(): TitlePersonCrossRefDao
    abstract fun popularityDao(): PopularityDao

//    abstract fun resumeDao(): ResumeDao
}
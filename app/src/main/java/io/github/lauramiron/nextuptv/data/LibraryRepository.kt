package io.github.lauramiron.nextuptv.data

import io.github.lauramiron.nextuptv.data.local.AppDb
import io.github.lauramiron.nextuptv.data.local.dao.ExternalIdDao
import io.github.lauramiron.nextuptv.data.local.dao.GenreDao
import io.github.lauramiron.nextuptv.data.local.dao.PersonDao
import io.github.lauramiron.nextuptv.data.local.dao.TitleDao
import io.github.lauramiron.nextuptv.data.local.dao.TitleGenreCrossRefDao
import io.github.lauramiron.nextuptv.data.local.dao.TitlePersonCrossRefDao
import io.github.lauramiron.nextuptv.data.remote.movienight.MovieNightApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class LibraryRepository(
    private val api: MovieNightApi,
    private val db: AppDb,
    private val titleDao: TitleDao,
    private val externalIdDao: ExternalIdDao,
    private val genreDao: GenreDao,
    private val personDao: PersonDao,
    private val titleGenreDao: TitleGenreCrossRefDao,
    private val titlePersonDao: TitlePersonCrossRefDao,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {

    data class SyncReport(
        var pages: Int = 0,
        var titlesUpserted: Int = 0,
        var episodesUpserted: Int = 0,
        var externalIdsUpserted: Int = 0,
        var genresUpserted: Int = 0,
        var peopleUpserted: Int = 0,
        var titleGenreRefs: Int = 0,
        var titlePersonRefs: Int = 0,
        var lastCursor: String? = null
    )

    /**
     * Full library sync from MovieOfTheNight (RapidAPI) into local Room DB.
     * - Streams pages with backoff via MovieNightApi
     * - Upserts Titles, Episodes, ExternalIds, People, Genres, CrossRefs
     * - Artwork JSON lives inline on Title/Episode entities (your mappers set it)
     * - Streaming options: store only what you decided (e.g., serviceId only) in your mapper
     */
    suspend fun syncAll(
        catalogs: String = "netflix",
        startCursor: String? = null,
        maxPages: Int? = null
    ): SyncReport {
        val report = SyncReport()

        return report
    }

    /**
     * Maps a TitleDto (and nested episodes/ids/credits/genres) to entities and upserts them.
     * Increments the provided report counters.
     */
    /*private suspend fun upsertOneTitleTree(dto: TitleDto, report: SyncReport) {
        // 1) Title
        val titleEntity: TitleEntity = dto.toEntity() // your mapper sets: name, kind, year, imageSetJson, etc.
        val titleId: Long = titleDao.upsert(titleEntity)
        report.titlesUpserted += 1

        // 2) External IDs
        val streamingOptions: List<StreamingOptionDto> = dto.extractUsStreamingOptions()
        val externalIdEntities = streamingOptions.map { it.toExternalIdEntity(titleId) }
        report.externalIdsUpserted += externalIdDao.upsertAll(externalIdEntities)

        // 3) Genres (name->entity), then cross-ref
        val genreNames: List<String> = dto.toGenreNames() // mapper normalizes ids/names from response
        val genreIds   = genreDao.upsertAllByName(genreNames)
        val genreRefs  = genreIds.map { gid -> TitleGenreCrossRef(titleId = titleId, genreId = gid) }
        titleGenreDao.upsertAll(genreRefs)
        report.titleGenreRefs += titleGenreDao.upsertAll(genreRefs)
        report.genresUpserted += genreIds.count { it > 0 } // count new rows if your DAO returns rowIds

        // 4) People (directors/cast/writers …), then cross-ref
        val cast: List<PersonEntity> = dto.toCast()
        val castPersonIds = personDao.upsertAll(cast)
        val castPersonRefs = castPersonIds.map { it -> TitlePersonCrossRef(id = 0, titleId = titleId, personId = it, role = CreditRole.CAST ) }

        val directors: List<PersonEntity> = dto.toDirectors()
        val directorPersonIds = personDao.upsertAll(directors)
        val directorPersonRefs = directorPersonIds.map { it -> TitlePersonCrossRef(id = 0, titleId = titleId, personId = it, role = CreditRole.DIRECTOR ) }

        val personRefs = castPersonRefs + directorPersonRefs
        report.titlePersonRefs += titlePersonDao.upsertAll(personRefs)
        report.peopleUpserted += castPersonIds.count() + directorPersonIds.count()

//        // 5) Episodes (for shows). Your mapper returns per-episode entities with titleId set.
//        val episodes: List<EpisodeEntity> = dto.toEpisodes(titleId) // or emptyList for movies
//        if (episodes.isNotEmpty()) {
//            report.episodesUpserted += episodeDao.upsertAll(episodes)
//        }

        // 6) Optional: minimal streaming options (serviceId-only), if you persist them.
        // If you decided to inline or skip, this can be omitted or kept in mapper side-effects.
        // e.g., streamingOptionDao.upsertAll(dto.toStreamingOptions(titleId))
    }*/


//    suspend fun syncTitle(monId: String) = withContext(io) {
//        val titleDto = api.getTitle(monId)
//        val episodes = if (titleDto.showType == "SERIES") api.getEpisodes(monId) else emptyList()
//
//        db.withTransaction {
//            val titleEntity = titleDto.toEntity()          // mapping extension
//            val titleId = db.titleDao().upsertTitles(listOf(titleEntity)).first()
//
//            // genres, artwork, credits -> map & insert
//            val ext = mutableListOf<ExternalIdEntity>()
//
//            titleDto.streamingOptions.orEmpty().forEach { (countryCode, options) ->
//                options
//                    .asSequence()
//                    .filter { countryCode.equals("us") }
//                    .forEach { so ->
//                        parseNetflixId(so.videoLink)?.let { id ->
//                            ext += ExternalIdEntity(
//                                entityType = "title",
//                                entityId = titleId,
//                                provider = "netflix",
//                                providerId = id,
//                            )
//                        }
//                    }
//            }
//
//            db.externalIdDao().insertAll(ext)
//
//            if (episodes.isNotEmpty()) {
//                val eps = episodes.map { it.toEntity(titleId) }
//                db.episodeDao().upsertEpisodes(eps)
////                db.externalIdDao().insertAll(
////                    episodes.mapNotNull { e ->
////                        e.externalId?.let {
////                            ExternalIdEntity(
////                                "episode", /*entityId to resolve later*/,
////                                "mon",
////                                it
////                            )
////                        }
////                    }
////                )
//                // tip: after upsert, query back to resolve entityIds for external_ids if needed
//            }
//        }
//    }
}

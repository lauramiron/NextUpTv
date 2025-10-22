package io.github.lauramiron.nextuptv.data

import androidx.room.withTransaction
import io.github.lauramiron.nextuptv.data.local.AppDb
import io.github.lauramiron.nextuptv.data.local.entity.CreditRole
import io.github.lauramiron.nextuptv.data.local.entity.PersonEntity
import io.github.lauramiron.nextuptv.data.local.entity.TitleEntity
import io.github.lauramiron.nextuptv.data.local.entity.TitleGenreCrossRef
import io.github.lauramiron.nextuptv.data.local.entity.TitlePersonCrossRef
import io.github.lauramiron.nextuptv.data.mappers.extractUsStreamingOptions
import io.github.lauramiron.nextuptv.data.mappers.toCast
import io.github.lauramiron.nextuptv.data.mappers.toDirectors
import io.github.lauramiron.nextuptv.data.mappers.toEntity
import io.github.lauramiron.nextuptv.data.mappers.toExternalIdEntity
import io.github.lauramiron.nextuptv.data.mappers.toGenreNames
import io.github.lauramiron.nextuptv.data.remote.movienight.MovieNightApi
import io.github.lauramiron.nextuptv.data.remote.movienight.StreamingOptionDto
import io.github.lauramiron.nextuptv.data.remote.movienight.TitleDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LibraryRepository(
    private val api: MovieNightApi,
    private val db: AppDb,
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
    ) {
        operator fun plusAssign(other: SyncReport) {
            this.pages += other.pages
            this.titlesUpserted += other.titlesUpserted
            this.episodesUpserted += other.episodesUpserted
            this.externalIdsUpserted += other.externalIdsUpserted
            this.genresUpserted += other.genresUpserted
            this.peopleUpserted += other.peopleUpserted
            this.titleGenreRefs += other.titleGenreRefs
            this.titlePersonRefs += other.titlePersonRefs
            this.lastCursor = other.lastCursor
        }
    }

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
        maxPages: Int = -1
    ): SyncReport {
        val report = SyncReport()
        var pagesProcessed = 0
        var cursor = startCursor

        do {
            val (titleDtos, nextCursor) = api.fetchAllShows(catalogs=catalogs, startCursor=cursor, maxPages=1)

            val pageReport = SyncReport()
            titleDtos.forEach { titleDto ->
                pageReport += upsertOneTitleTree(titleDto)
            }

            pagesProcessed++
            report.pages = pagesProcessed
            report += pageReport
            cursor = nextCursor

            println("Page $pagesProcessed: ${titleDtos.size} titles | " +
                    "+${pageReport.titlesUpserted} titles, " +
                    "+${pageReport.genresUpserted} genres, " +
                    "+${pageReport.peopleUpserted} people")

        } while (((maxPages == -1) || (pagesProcessed < maxPages)) && (cursor != null))

        return report
    }

    /**
     * Maps a TitleDto (and nested episodes/ids/credits/genres) to entities and upserts them.
     * Returns a report of what was inserted/updated.
     */
    internal suspend fun upsertOneTitleTree(dto: TitleDto): SyncReport {
        val report = SyncReport()

        // 1) Title
        val titleEntity: TitleEntity = dto.toEntity() // your mapper sets: name, kind, year, imageSetJson, etc.
        val titleId: Long = db.titleDao().upsert(titleEntity)
        report.titlesUpserted = 1

        // 2) External IDs
        val streamingOptions: List<StreamingOptionDto> = dto.extractUsStreamingOptions()
        val externalIdEntities = streamingOptions.map { it.toExternalIdEntity(titleId) }
        report.externalIdsUpserted = db.externalIdDao().upsertAll(externalIdEntities)

        // 3) Genres (name->entity), then cross-ref
        val genreNames: List<String> = dto.toGenreNames() // mapper normalizes ids/names from response
        val genreIds   = db.genreDao().upsertAllByName(genreNames)
        val genreRefs  = genreIds.map { gid ->
            TitleGenreCrossRef(
                titleId = titleId,
                genreId = gid
            )
        }
        val genreRefIds = db.titleGenreDao().upsertAll(genreRefs)
        report.titleGenreRefs = genreRefIds.size
        report.genresUpserted = genreIds.count { it > 0 } // count new rows if your DAO returns rowIds

        // 4) People (directors/cast/writers …), then cross-ref
        val cast: List<PersonEntity> = dto.toCast()
        val castPersonIds = db.personDao().upsertAll(cast)
        val castPersonRefs = castPersonIds.map { it ->
            TitlePersonCrossRef(
                id = 0,
                titleId = titleId,
                personId = it,
                role = CreditRole.CAST
            )
        }

        val directors: List<PersonEntity> = dto.toDirectors()
        val directorPersonIds = db.personDao().upsertAll(directors)
        val directorPersonRefs = directorPersonIds.map { it -> TitlePersonCrossRef(id = 0, titleId = titleId, personId = it, role = CreditRole.DIRECTOR ) }

        val personRefs = castPersonRefs + directorPersonRefs
        val personRefIds = db.titlePersonDao().upsertAll(personRefs)
        report.titlePersonRefs = personRefIds.size
        report.peopleUpserted = castPersonIds.count() + directorPersonIds.count()

//        // 5) Episodes (for shows). Your mapper returns per-episode entities with titleId set.
//        val episodes: List<EpisodeEntity> = dto.toEpisodes(titleId) // or emptyList for movies
//        if (episodes.isNotEmpty()) {
//            report.episodesUpserted = episodeDao.upsertAll(episodes)
//        }

        // 6) Optional: minimal streaming options (serviceId-only), if you persist them.
        // If you decided to inline or skip, this can be omitted or kept in mapper side-effects.
        // e.g., streamingOptionDao.upsertAll(dto.toStreamingOptions(titleId))

        return report
    }


    suspend fun syncTitle(monId: String): SyncReport = withContext(io) {
        try {
            val titleDto = api.getTitle(monId)

            db.withTransaction {
                upsertOneTitleTree(titleDto)
            }

        } catch (e: Exception) {
            // Log error but don't throw - return empty report
            println("Error syncing title $monId: ${e.message}")
            SyncReport()
        }
    }
}

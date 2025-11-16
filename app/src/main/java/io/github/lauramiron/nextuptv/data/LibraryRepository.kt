package io.github.lauramiron.nextuptv.data

import androidx.room.withTransaction
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.lauramiron.nextuptv.data.local.AppDb
import io.github.lauramiron.nextuptv.data.local.entity.CreditRole
import io.github.lauramiron.nextuptv.data.local.entity.StreamingOptionEntity
import io.github.lauramiron.nextuptv.data.local.entity.LibrarySyncMetadataEntity
import io.github.lauramiron.nextuptv.data.local.entity.PersonEntity
import io.github.lauramiron.nextuptv.data.local.entity.PopularityEntity
import io.github.lauramiron.nextuptv.util.StreamingService
import io.github.lauramiron.nextuptv.data.local.entity.SyncType
import io.github.lauramiron.nextuptv.data.local.entity.TitleEntity
import io.github.lauramiron.nextuptv.data.local.entity.TitleGenreCrossRef
import io.github.lauramiron.nextuptv.data.local.entity.TitlePersonCrossRef
import io.github.lauramiron.nextuptv.data.local.entity.TitleWithExternalId
import java.util.Date
import io.github.lauramiron.nextuptv.data.mappers.extractUsStreamingOptions
import io.github.lauramiron.nextuptv.data.mappers.toCast
import io.github.lauramiron.nextuptv.data.mappers.toDirectors
import io.github.lauramiron.nextuptv.data.mappers.toEntity
import io.github.lauramiron.nextuptv.data.mappers.toStreamingOptionEntity
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
    // Moshi for serializing sync metadata
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val stringListAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )

    data class SyncReport(
        var pages: Int = 0,
        var titlesUpserted: Int = 0,
        var titleIdsUpserted: List<Long> = emptyList<Long>(),
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
            this.titleIdsUpserted += other.titleIdsUpserted
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
     * - Streams pages with backoff via MovieNightApi using Flow (similar to yield/generators)
     * - Inserts into database after each page for incremental progress
     * - Upserts Titles, Episodes, ExternalIds, People, Genres, CrossRefs
     * - Artwork JSON lives inline on Title/Episode entities (your mappers set it)
     * - Streaming options: store only what you decided (e.g., serviceId only) in your mapper
     * - Logs sync metadata for tracking and resuming interrupted syncs
     */
    suspend fun syncAll(
        catalogs: String,
        startCursor: String? = null,
        maxPages: Int = -1
    ): SyncReport {
        val servicesList = catalogs.split(",").map { it.trim() }
        val servicesJson = stringListAdapter.toJson(servicesList)

        // Special handling for HBO: transform to Prime addon catalog for API
        val isHboSync = catalogs.trim().equals("hbo", ignoreCase = true)
        val apiCatalogs = if (isHboSync) "prime.addon.hbomaxus" else catalogs

        // Determine sync type based on maxPages parameter
        val syncType = if (maxPages == -1) SyncType.FULL else SyncType.PARTIAL

        val syncMetadata = LibrarySyncMetadataEntity(
            updatedAt = Date(),
            syncType = syncType,
            success = false,
            services = servicesJson,
            metadataJson = null,
            nextCursor = startCursor
        )

        val metadataId = db.librarySyncMetadataDao().insert(syncMetadata)

        return try {
            val report = performSync(apiCatalogs, startCursor, maxPages, isHboSync)

            // Determine if this was a partial sync or a complete full sync
            // - If maxPages != -1, it's always a partial sync (save cursor for resumption)
            // - If maxPages == -1 but lastCursor != null, there are more pages (save cursor)
            // - Only clear cursor if maxPages == -1 AND lastCursor == null (true full sync completed)
            val isPartialSync = maxPages != -1 || report.lastCursor != null
            val cursorToSave = if (isPartialSync) report.lastCursor else null

            // Update the existing metadata record with success results
            val reportJson = moshi.adapter(SyncReport::class.java).toJson(report)
            db.librarySyncMetadataDao().update(
                syncMetadata.copy(
                    id = metadataId.toInt(),
                    updatedAt = Date(),     // Update timestamp to completion time
                    success = true,
                    metadataJson = reportJson,
                    nextCursor = cursorToSave  // Save cursor for partial syncs, null for complete syncs
                )
            )

            report
        } catch (e: Exception) {
            // Update the existing metadata record with failure results
            // Save any partial progress cursor for potential resumption
            val partialReportJson = moshi.adapter(SyncReport::class.java).toJson(SyncReport())
            db.librarySyncMetadataDao().update(
                syncMetadata.copy(
                    id = metadataId.toInt(),
                    updatedAt = Date(),     // Update timestamp to failure time
                    success = false,
                    metadataJson = partialReportJson,
                    nextCursor = startCursor  // Keep the cursor we started with for retry
                )
            )
            throw e
        }
    }

    private suspend fun performSync(
        catalogs: String,
        startCursor: String?,
        maxPages: Int,
        isHboSync: Boolean = false
    ): SyncReport {
        val report = SyncReport()
        var pagesProcessed = 0

        // Use Flow to process pages incrementally as they arrive
        api.fetchShowsPagingFlow(
            catalogs = catalogs,
            startCursor = startCursor,
            maxPages = if (maxPages == -1) null else maxPages
        ).collect { response ->
            // Process this page immediately and insert into database
            val pageReport = SyncReport()

            response.shows.forEach { titleDto ->
                pageReport += upsertOneTitleTree(titleDto, isHboSync)
            }

            pagesProcessed++
            report.pages = pagesProcessed
            report += pageReport
            report.lastCursor = response.nextCursor

            println("Page $pagesProcessed: ${response.shows.size} titles | " +
                    "+${pageReport.titlesUpserted} titles, " +
                    "+${pageReport.genresUpserted} genres, " +
                    "+${pageReport.peopleUpserted} people")
        }

        return report
    }

    /**
     * Maps a TitleDto (and nested episodes/ids/credits/genres) to entities and upserts them.
     * Returns a report of what was inserted/updated.
     */
    internal suspend fun upsertOneTitleTree(dto: TitleDto, isHboSync: Boolean = false): SyncReport {
        val report = SyncReport()

        // 1) Title
        val titleEntity: TitleEntity = dto.toEntity() // your mapper sets: name, kind, year, imageSetJson, etc.
        val titleId: Long = db.titleDao().upsert(titleEntity)
        report.titlesUpserted = 1
        report.titleIdsUpserted = longArrayOf(titleId).toList()

        // 2) Streaming Options
        val streamingOptionEntities = if (isHboSync) {
            // For HBO sync, ignore streaming options and create a single HBO streaming option with "unknown" values
            listOf(
                StreamingOptionEntity(
                    service = StreamingService.HBO,
                    serviceItemId = "unknown",
                    entityId = titleId,
                    available = true,
                    price = 0,
                    link = "unknown"
                )
            )
        } else {
            // Normal processing: extract streaming options and parse service-specific IDs
            val streamingOptions: List<StreamingOptionDto> = dto.extractUsStreamingOptions()
            streamingOptions.mapNotNull { it.toStreamingOptionEntity(titleId) }
        }
        report.externalIdsUpserted = db.streamingOptionDao().upsertAll(streamingOptionEntities)

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

    suspend fun syncTopShows(provider: StreamingService) {
        try {
            val topShows = api.getTopShows(provider)

            db.withTransaction {
                // Upsert each title and collect their IDs
                val titleIds = topShows.map { titleDto ->
                    val report = upsertOneTitleTree(titleDto)
                    report.titleIdsUpserted.first()
                }

                // Update the top shows list for this provider
                db.popularityDao().updateTopShows(service = provider, titleIds = titleIds)
            }

        } catch (e: Exception) {
            // Log error but don't throw - return empty report
            println("Error syncing $provider top shows: ${e.message}")
            SyncReport()
        }
    }

    /**
     * Get the top titles for a streaming service from the local database.
     * Returns titles ordered by their position in the popularity rankings.
     *
     * @param service The streaming service to query
     * @return List of TitleEntity objects representing the top shows
     */
    suspend fun topTitles(service: StreamingService): List<TitleEntity> = withContext(io) {
        db.popularityDao().getTopShowsForService(service)
    }

    /**
     * Get the top titles for a streaming service along with their external IDs.
     * This enables constructing service-specific launch URLs.
     *
     * @param service The streaming service to query
     * @return List of TitleWithExternalId objects containing both title data and external IDs
     */
    suspend fun topTitlesWithExternalIds(service: StreamingService): List<TitleWithExternalId> = withContext(io) {
        db.popularityDao().getTopShowsWithExternalIds(service)
    }
}

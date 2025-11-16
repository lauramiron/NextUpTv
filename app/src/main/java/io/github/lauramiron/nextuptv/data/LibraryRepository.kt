package io.github.lauramiron.nextuptv.data

import androidx.room.withTransaction
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.lauramiron.nextuptv.data.local.AppDb
import io.github.lauramiron.nextuptv.data.local.entity.CreditRole
import io.github.lauramiron.nextuptv.data.local.entity.LibrarySyncMetadataEntity
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
import kotlinx.coroutines.runBlocking
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

    data class UpsertTitleReport(
        val titleIdsUpserted: List<Long> = emptyList(),
        val externalIdsUpserted: Int = 0,
        val genresUpserted: Int = 0,
        val peopleUpserted: Int = 0,
        val titleGenreRefs: Int = 0,
        val titlePersonRefs: Int = 0
    )

    data class SyncPageReport(
        var pageResponseSize: Int = 0,
        var titlesUpserted: Int = 0,
        var titleIdsUpserted: List<Long> = emptyList(),
        var episodesUpserted: Int = 0,
        var externalIdsUpserted: Int = 0,
        var genresUpserted: Int = 0,
        var peopleUpserted: Int = 0,
        var titleGenreRefs: Int = 0,
        var titlePersonRefs: Int = 0,
        var lastCursor: String? = null
    ) {
        operator fun plusAssign(other: UpsertTitleReport) {
            this.titlesUpserted += 1
            this.titleIdsUpserted += other.titleIdsUpserted
            this.externalIdsUpserted += other.externalIdsUpserted
            this.genresUpserted += other.genresUpserted
            this.peopleUpserted += other.peopleUpserted
            this.titleGenreRefs += other.titleGenreRefs
            this.titlePersonRefs += other.titlePersonRefs
        }
    }

    data class SyncReport(
        var pagesProcessed: Int = 0,
        var pageResponseSize: Int = 0,
        var titlesUpserted: Int = 0,
        var titleIdsUpserted: List<Long> = emptyList(),
        var episodesUpserted: Int = 0,
        var externalIdsUpserted: Int = 0,
        var genresUpserted: Int = 0,
        var peopleUpserted: Int = 0,
        var titleGenreRefs: Int = 0,
        var titlePersonRefs: Int = 0,
        var lastCursor: String? = null,
        val startTime: Long = System.currentTimeMillis(),
        var lastPageReport: SyncPageReport? = null,
        private val db: AppDb? = null
    ) {
        private val initialDbCount: Int? = db?.let { runBlocking { it.titleDao().countAll() } }

        init {
            printSyncStarting()
        }
        operator fun plusAssign(pageReport: SyncPageReport) {
            this.pagesProcessed += 1
            this.titlesUpserted += pageReport.titlesUpserted
            this.titleIdsUpserted += pageReport.titleIdsUpserted
            this.episodesUpserted += pageReport.episodesUpserted
            this.externalIdsUpserted += pageReport.externalIdsUpserted
            this.genresUpserted += pageReport.genresUpserted
            this.peopleUpserted += pageReport.peopleUpserted
            this.titleGenreRefs += pageReport.titleGenreRefs
            this.titlePersonRefs += pageReport.titlePersonRefs
            this.lastCursor = pageReport.lastCursor
            this.lastPageReport = pageReport
        }

        fun copyFrom(other: SyncReport) {
            this.pagesProcessed = other.pagesProcessed
            this.pageResponseSize = other.pageResponseSize
            this.titlesUpserted = other.titlesUpserted
            this.titleIdsUpserted = other.titleIdsUpserted
            this.episodesUpserted = other.episodesUpserted
            this.externalIdsUpserted = other.externalIdsUpserted
            this.genresUpserted = other.genresUpserted
            this.peopleUpserted = other.peopleUpserted
            this.titleGenreRefs = other.titleGenreRefs
            this.titlePersonRefs = other.titlePersonRefs
            this.lastCursor = other.lastCursor
            this.lastPageReport = other.lastPageReport
            // startTime is NOT copied
        }

        private fun printSyncStarting() {
            initialDbCount?.let {
                println("Initial title count: $it")
                println()
            }
            println("Starting sync...")
        }

        fun printPageStats() {
            lastPageReport?.let { page ->
                println("Page $pagesProcessed: ${page.pageResponseSize} titles | " +
                        "+${page.titlesUpserted} titles, " +
                        "+${page.genresUpserted} genres, " +
                        "+${page.peopleUpserted} people")
            }
        }

        fun printSyncComplete() {
            val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0
            println()
            println("=== Sync Complete ===")
            println("Time elapsed: ${elapsedSeconds}s")
            println()
            println("=== Sync Report ===")
            println("Pages processed: $pagesProcessed")
            println("Titles upserted: $titlesUpserted")
            println("External IDs upserted: $externalIdsUpserted")
            println("Genres upserted: $genresUpserted")
            println("People upserted: $peopleUpserted")
            println("Title-Genre refs: $titleGenreRefs")
            println("Title-Person refs: $titlePersonRefs")
            println()
            initialDbCount?.let { initial ->
                val final = runBlocking { db!!.titleDao().countAll() }
                println("Database title count: $initial -> $final (+${final - initial})")
                println()
            }
            println("SUCCESS!")
        }

        fun printSyncFailed(e: Exception) {
            val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0
            println()
            println("=== Sync Failed ===")
            println("Time elapsed: ${elapsedSeconds}s")
            println("Error: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Sync a streaming service with optional resume and page limit support.
     * Prints sync progress and completion/failure messages.
     *
     * @param service The streaming service to sync
     * @param maxPages Maximum number of pages to sync, or -1 for full sync (default)
     * @param resume If true, attempts to resume from the last sync's cursor; if false, starts from beginning (default)
     */
    suspend fun syncService(service: StreamingService, maxPages: Int = -1, resume: Boolean = false) {
        syncServices(services = listOf(service), maxPages = maxPages, resume = resume)
    }

    /**
     * Sync all defined streaming services.
     * Prints sync progress and completion/failure messages for all services.
     *
     * @param maxPages Maximum number of pages to sync per service, or -1 for full sync (default)
     * @param resume If true, attempts to resume from the last sync's cursor; if false, starts from beginning (default)
     */
    suspend fun syncAll(maxPages: Int = -1, resume: Boolean = false) {
        syncServices(services = StreamingService.entries.toList(), maxPages = maxPages, resume = resume)
    }

    /**
     * Get the cursor to resume from for a service, or null if starting fresh.
     * Prints informative messages about the resume state.
     *
     * @param catalogs The catalog(s) to sync (e.g., "netflix", "apple")
     * @return The cursor to resume from, or null to start from beginning
     */
    suspend fun getLastNextCursor(catalogs: String): String? {
        val serviceName = catalogs.uppercase()
        val lastSync = db.librarySyncMetadataDao().getLastSyncForService(catalogs)

        return when {
            lastSync == null -> {
                println("No previous sync found for $serviceName. Starting from beginning.")
                null
            }
            lastSync.success && lastSync.nextCursor == null -> {
                println("Last sync for $serviceName completed fully. Restarting from beginning.")
                null
            }
            else -> {
                println("Resuming $serviceName sync from cursor: ${lastSync.nextCursor}")
                lastSync.nextCursor
            }
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
    suspend fun syncServices(
        services: List<StreamingService>,
        maxPages: Int = -1,
        resume: Boolean = false
    ): SyncReport {
        // Build catalogs string, replacing HBO with Prime addon catalog
        val catalogsList = services.map { service ->
            if (service == StreamingService.HBO) "prime.addon.hbomaxus" else service.id
        }
        val catalogs = catalogsList.joinToString(",")

        // Determine starting cursor based on resume parameter
        // Use first service's ID for cursor lookup when resuming
        val startCursor = if (resume) getLastNextCursor(services.first().id) else null

        val syncTypeDisplay = if (maxPages == -1) "Full" else "Partial (First $maxPages pages)"
        println("=== $syncTypeDisplay ${services.joinToString(", ") { it.id.uppercase() }} Sync ${if (resume && startCursor != null) "(Resuming)" else ""} ===\n")

        val report = SyncReport(db = db)
        val servicesList = stringListAdapter.toJson(services.map { it.id })

        // Determine sync type based on maxPages parameter
        val syncType = if (maxPages == -1) SyncType.FULL else SyncType.PARTIAL

        val syncMetadata = LibrarySyncMetadataEntity(
            updatedAt = Date(),
            syncType = syncType,
            success = false,
            services = servicesList,
            metadataJson = null,
            nextCursor = startCursor
        )

        val metadataId = db.librarySyncMetadataDao().insert(syncMetadata)

        return try {
            report.copyFrom(performSync(catalogs, startCursor, maxPages))

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

            report.printSyncComplete()
            report
        } catch (e: Exception) {
            // Print sync failure using report that has correct startTime
            report.printSyncFailed(e)

            // Update the existing metadata record with failure results
            // Save any partial progress cursor for potential resumption
            val partialReportJson = moshi.adapter(SyncReport::class.java).toJson(report)
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
    ): SyncReport {
        val report = SyncReport()

        // Use Flow to process pages incrementally as they arrive
        api.fetchShowsPagingFlow(
            catalogs = catalogs,
            startCursor = startCursor,
            maxPages = if (maxPages == -1) null else maxPages
        ).collect { response ->
            // Process this page immediately and insert into database
            val pageReport = SyncPageReport(
                pageResponseSize = response.shows.size,
                lastCursor = response.nextCursor
            )

            response.shows.forEach { titleDto ->
                pageReport += upsertOneTitleTree(titleDto)
            }

            report += pageReport
            report.printPageStats()
        }

        return report
    }

    /**
     * Maps a TitleDto (and nested episodes/ids/credits/genres) to entities and upserts them.
     * Returns a report of what was inserted/updated.
     */
    internal suspend fun upsertOneTitleTree(dto: TitleDto): UpsertTitleReport {
        // 1) Title
        val titleEntity: TitleEntity = dto.toEntity() // your mapper sets: name, kind, year, imageSetJson, etc.
        val titleId: Long = db.titleDao().upsert(titleEntity)

        // 2) Streaming Options
        val streamingOptions: List<StreamingOptionDto> = dto.extractUsStreamingOptions()
        val streamingOptionEntities = streamingOptions.mapNotNull { it.toStreamingOptionEntity(titleId) }
        val streamingOptionsUpserted = db.streamingOptionDao().upsertAll(streamingOptionEntities)

        // 3) Genres (name->entity), then cross-ref
        val genreIds   = db.genreDao().upsertAllByName(dto.toGenreNames())
        val genreRefs  = genreIds.map { gid ->
            TitleGenreCrossRef(
                titleId = titleId,
                genreId = gid
            )
        }
        val genreRefIds = db.titleGenreDao().upsertAll(genreRefs)

        // 4) People (directors/cast/writers …), then cross-ref
        val castPersonIds = db.personDao().upsertAll(dto.toCast())
        val castPersonRefs = castPersonIds.map { it ->
            TitlePersonCrossRef(
                id = 0,
                titleId = titleId,
                personId = it,
                role = CreditRole.CAST
            )
        }

        val directorPersonIds = db.personDao().upsertAll(dto.toDirectors())
        val directorPersonRefs = directorPersonIds.map { it -> TitlePersonCrossRef(id = 0, titleId = titleId, personId = it, role = CreditRole.DIRECTOR ) }

        val personRefs = castPersonRefs + directorPersonRefs
        val personRefIds = db.titlePersonDao().upsertAll(personRefs)

//        // 5) Episodes (for shows). Your mapper returns per-episode entities with titleId set.
//        val episodes: List<EpisodeEntity> = dto.toEpisodes(titleId) // or emptyList for movies
//        if (episodes.isNotEmpty()) {
//            report.episodesUpserted = episodeDao.upsertAll(episodes)
//        }

        return UpsertTitleReport(
            titleIdsUpserted = listOf(titleId),
            externalIdsUpserted = streamingOptionsUpserted,
            genresUpserted = genreIds.count { it > 0 },
            peopleUpserted = castPersonIds.count() + directorPersonIds.count(),
            titleGenreRefs = genreRefIds.size,
            titlePersonRefs = personRefIds.size
        )
    }


    suspend fun syncTitle(monId: String): SyncReport = withContext(io) {
        try {
            val titleDto = api.getTitle(monId)

            val upsertReport = db.withTransaction {
                upsertOneTitleTree(titleDto)
            }

            // Convert UpsertTitleReport to SyncReport
            SyncReport(
                pagesProcessed = 1,
                titlesUpserted = 1,
                titleIdsUpserted = upsertReport.titleIdsUpserted,
                externalIdsUpserted = upsertReport.externalIdsUpserted,
                genresUpserted = upsertReport.genresUpserted,
                peopleUpserted = upsertReport.peopleUpserted,
                titleGenreRefs = upsertReport.titleGenreRefs,
                titlePersonRefs = upsertReport.titlePersonRefs
            )
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
            // Log error but don't throw
            println("Error syncing $provider top shows: ${e.message}")
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

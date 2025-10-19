package io.github.lauramiron.nextuptv.data.remote.movienight

import io.github.lauramiron.nextuptv.data.remote.movienight.ShowSearchResponseDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface MovieNightService {
    @GET("shows/search/filters")
    suspend fun searchShows(
        @Header("X-RapidAPI-Key") apiKey: String,
        @Query("country") country: String = "us",
        @Query("catalogs") catalogs: String = "netflix",
        @Query("cursor") cursor: String? = null
    ): ShowSearchResponseDto
}

/**
 * A thin API wrapper that handles pagination and simple retry for 429/5xx.
 */
class MovieNightApi(
    private val service: MovieNightService,
    private val apiKey: String
) {
    /**
     * Fetch *pages* of shows until `hasMore` is false.
     *
     * @param catalogs e.g. "netflix" (can be comma-separated later)
     * @param startCursor optional cursor to resume
     * @param maxPages optional safety cap on pages (null = no cap)
     * @return Pair(allShows, lastNextCursor)
     */
    suspend fun fetchAllShows(
        catalogs: String = "netflix",
        startCursor: String? = null,
        maxPages: Int? = null
    ): Pair<List<TitleDto>, String?> {
        val out = mutableListOf<TitleDto>()
        var cursor = startCursor
        var pages = 0
        var lastNextCursor: String? = null

        do {
            val resp = retrying {
                service.searchShows(
                    apiKey = apiKey,
                    country = "us",
                    catalogs = catalogs,
                    cursor = cursor
                )
            }

            out += resp.shows
            pages += 1
            lastNextCursor = resp.nextCursor
            cursor = resp.nextCursor

            val more = resp.hasMore && (maxPages == null || pages < maxPages)
        } while (more)

        return out to lastNextCursor
    }

    /**
     * Emit pages incrementally (useful if you want to stream into DB page-by-page).
     */
    fun fetchShowsPagingFlow(
        catalogs: String = "netflix",
        startCursor: String? = null,
        maxPages: Int? = null
    ): Flow<ShowSearchResponseDto> = flow {
        var cursor = startCursor
        var pages = 0
        do {
            val resp = retrying {
                service.searchShows(
                    apiKey = apiKey,
                    country = "us",
                    catalogs = catalogs,
                    cursor = cursor
                )
            }
            emit(resp)
            pages += 1
            cursor = resp.nextCursor
        } while (resp.hasMore && (maxPages == null || pages < maxPages))
    }

    // --- simple retry helper (exponential backoff on 429/5xx) ---
    private suspend inline fun <T> retrying(
        times: Int = 3,
        initialDelayMs: Long = 500,
        maxDelayMs: Long = 4_000,
        factor: Double = 2.0,
        crossinline block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        repeat(times) {
            try {
                return block()
            } catch (e: HttpException) {
                if (e.code() != 429 && e.code() !in 500..599) throw e
            } catch (e: java.io.IOException) {
                // network glitch — retry
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
        }
        return block() // final attempt (let exceptions bubble)
    }
}

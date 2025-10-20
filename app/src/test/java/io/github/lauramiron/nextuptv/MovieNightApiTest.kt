package io.github.lauramiron.nextuptv

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.lauramiron.nextuptv.data.remote.movienight.MovieNightApiFactory
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MovieNightApiTest {

    private lateinit var api: io.github.lauramiron.nextuptv.data.remote.movienight.MovieNightApi

    @Before
    fun setUp() {
        // Use the same API key as in your test
        api = MovieNightApiFactory.create(apiKey = "96da59657emsh4a212c55a8a0cdep152371jsnc0a31a8bc448")
    }

    @Test
    fun `fetch one page of Netflix titles and log response`() = runTest {
        try {
            // Fetch one page of Netflix titles
            val (titles, nextCursor) = api.fetchAllShows(
                catalogs = "netflix",
                startCursor = null,
                maxPages = 1
            )

            // Log the results
            println("=== MovieNight API Response ===")
            println("Number of titles returned: ${titles.size}")
            println("Next cursor: $nextCursor")
            println()

            // Log first few titles for inspection
            titles.take(5).forEachIndexed { index, title ->
                println("Title ${index + 1}:")
                println("  ID: ${title.id}")
                println("  Name: ${title.title}")
                println("  Year: ${title.releaseYear}")
                println("  Type: ${title.showType}")
                println("  Overview: ${title.overview?.take(100)}...")
                println("  Genres: ${title.genres.map { it.name }}")
                println("  Directors: ${title.directors}")
                println("  Cast: ${title.cast.take(3)}")
                println("  Runtime: ${title.runtime} min")
                println("  US Streaming Options: ${title.streamingOptions?.get("us")?.size ?: 0}")
                println()
            }

            // Verify we got some results
            assert(titles.isNotEmpty()) { "Expected to receive some titles from API" }
            println("✅ API test completed successfully!")

        } catch (e: Exception) {
            println("❌ API test failed with error: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    @Test
    fun `fetch titles using flow and log first page`() = runTest {
        try {
            println("=== Testing Flow-based API ===")

            var pageCount = 0
            api.fetchShowsPagingFlow(
                catalogs = "netflix",
                maxPages = 1
            ).collect { response ->
                pageCount++
                println("Page $pageCount received:")
                println("  Shows in page: ${response.shows.size}")
                println("  Has more: ${response.hasMore}")
                println("  Next cursor: ${response.nextCursor}")

                // Log first title from this page
                response.shows.firstOrNull()?.let { title ->
                    println("  First title: ${title.title} (${title.releaseYear})")
                }
                println()
            }

            assert(pageCount > 0) { "Expected to receive at least one page" }
            println("✅ Flow API test completed successfully!")

        } catch (e: Exception) {
            println("❌ Flow API test failed with error: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    @Test
    fun `test different catalog types`() = runTest {
        val catalogs = listOf("netflix", "prime", "disney", "hulu")

        for (catalog in catalogs) {
            try {
                println("=== Testing catalog: $catalog ===")

                val (titles, cursor) = api.fetchAllShows(
                    catalogs = catalog,
                    maxPages = 1
                )

                println("$catalog results: ${titles.size} titles")
                if (titles.isNotEmpty()) {
                    println("Sample title: ${titles.first().title}")
                }
                println()

            } catch (e: Exception) {
                println("⚠️ $catalog failed: ${e.message}")
                // Don't throw - some catalogs might not be available
            }
        }
    }

    @Test
    fun `fetch single title by monId and log details`() = runTest {
        try {
//            // First get a title ID from the search results
//            val (titles, _) = api.fetchAllShows(
//                catalogs = "netflix",
//                maxPages = 1
//            )
//
//            assert(titles.isNotEmpty()) { "Need at least one title to test getTitle" }
//
//            val firstTitle = titles.first()
//            val monId = firstTitle.id

            val monId = "54321";

            println("=== Testing getTitle with ID: $monId ===")
//            println("Original title from search: ${firstTitle.title}")
            println()

            // Now fetch the same title by ID
            val titleDetails = api.getTitle(monId)

            println("=== Single Title Details ===")
            println("ID: ${titleDetails.id}")
            println("Title: ${titleDetails.title}")
            println("Original Title: ${titleDetails.originalTitle}")
            println("Year: ${titleDetails.releaseYear}")
            println("Type: ${titleDetails.showType}")
            println("Runtime: ${titleDetails.runtime} min")
            println("Rating: ${titleDetails.rating}/100")
            println("IMDB ID: ${titleDetails.imdbId}")
            println("TMDB ID: ${titleDetails.tmdbId}")
            println()

            println("Overview: ${titleDetails.overview}")
            println()

            println("Genres: ${titleDetails.genres.map { "${it.name} (${it.id})" }}")
            println("Directors: ${titleDetails.directors}")
            println("Cast: ${titleDetails.cast}")
            println()

            titleDetails.imageSet?.let { imageSet ->
                println("Image Set:")
                imageSet.verticalPoster?.let { println("  Vertical Posters: ${it.keys}") }
                imageSet.horizontalPoster?.let { println("  Horizontal Posters: ${it.keys}") }
                imageSet.verticalBackdrop?.let { println("  Vertical Backdrops: ${it.keys}") }
                imageSet.horizontalBackdrop?.let { println("  Horizontal Backdrops: ${it.keys}") }
                println()
            }

            titleDetails.streamingOptions?.forEach { (country, options) ->
                println("$country Streaming Options:")
                options.forEach { option ->
                    println("  ${option.service.id}: ${option.link ?: option.videoLink}")
                }
            }

            // Verify the response
            assert(titleDetails.id == monId) { "Returned title should have same ID" }
            assert(titleDetails.title.isNotBlank()) { "Title should not be empty" }

            println("✅ getTitle test completed successfully!")

        } catch (e: Exception) {
            println("❌ getTitle test failed with error: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}
package io.github.lauramiron.nextuptv

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.lauramiron.nextuptv.data.LibraryRepository
import io.github.lauramiron.nextuptv.data.local.AppDb
import io.github.lauramiron.nextuptv.data.local.DatabaseProvider
import io.github.lauramiron.nextuptv.data.local.entity.StreamingService
import io.github.lauramiron.nextuptv.data.remote.movienight.MovieNightApiFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test is designed to be run manually from command line with no timeout.
 *
 * Run with:
 *   ./gradlew :app:testDebugUnitTest --tests "io.github.lauramiron.nextuptv.LibrarySyncCliTest.runFullNetflixSync"
 *
 * Or from Android Studio:
 *   Right-click on the test method > Run 'runFullNetflixSync()'
 *
 * To disable timeout in Android Studio:
 *   Run > Edit Configurations > select the test > VM Options: -Djunit.jupiter.execution.timeout.default=0
 */
@RunWith(AndroidJUnit4::class)
class LibrarySyncCliTest {

    private lateinit var context: Context
    private lateinit var db: AppDb
    private lateinit var repository: LibraryRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Use DatabaseProvider singleton - it will load from snapshot if available
        db = DatabaseProvider.getInstance(context)

        repository = LibraryRepository(
            api = MovieNightApiFactory.create(apiKey = "96da59657emsh4a212c55a8a0cdep152371jsnc0a31a8bc448"),
            db = db,
            io = Dispatchers.Default
        )
    }

    @After
    fun tearDown() {
        // Export the snapshot - it will be saved to app/src/main/assets/
        DatabaseProvider.exportSnapshot(context)
    }

    /**
     * This is NOT a real test - it's a CLI wrapper.
     * It will always "pass" even if sync encounters errors.
     *
     * Use this to run the full Netflix sync without test timeout constraints.
     */
    @Test
    fun runFullNetflixSync() = runBlocking {
        println("=== Full Netflix Sync (No Timeout) ===")
        println("This will take several minutes. Be patient!")
        println()

        val startTime = System.currentTimeMillis()

        try {
            // Get initial count
            val initialCount = db.titleDao().countAll()
            println("Initial title count: $initialCount")
            println()

            // Run the sync with no page limit
            println("Starting sync...")
            val report = repository.syncAll(catalogs = "netflix")

            // Print results
            val finalCount = db.titleDao().countAll()
            val elapsed = (System.currentTimeMillis() - startTime) / 1000.0

            println()
            println("=== Sync Complete ===")
            println("Time elapsed: ${elapsed}s")
            println()
            println("=== Sync Report ===")
            println("Pages processed: ${report.pages}")
            println("Titles upserted: ${report.titlesUpserted}")
            println("External IDs upserted: ${report.externalIdsUpserted}")
            println("Genres upserted: ${report.genresUpserted}")
            println("People upserted: ${report.peopleUpserted}")
            println("Title-Genre refs: ${report.titleGenreRefs}")
            println("Title-Person refs: ${report.titlePersonRefs}")
            println()
            println("Database title count: $initialCount -> $finalCount (+${finalCount - initialCount})")
            println()
            println("SUCCESS!")

        } catch (e: Exception) {
            val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
            println()
            println("=== Sync Failed ===")
            println("Time elapsed: ${elapsed}s")
            println("Error: ${e.message}")
            e.printStackTrace()
            // Don't throw - let the test "pass" so you can see the output
        }
    }

    /**
     * Same as above but limits to a specific number of pages.
     * Useful for testing without waiting for the full sync.
     */
    @Test
    fun runPartialNetflixSync() = runBlocking {
        val maxPages = 5
        println("=== Partial Netflix Sync (First $maxPages pages) ===")
        println()

        val startTime = System.currentTimeMillis()

        try {
            val initialCount = db.titleDao().countAll()
            println("Initial title count: $initialCount")
            println()

            println("Starting sync...")
            val report = repository.syncAll(catalogs = "netflix", maxPages = maxPages)

            val finalCount = db.titleDao().countAll()
            val elapsed = (System.currentTimeMillis() - startTime) / 1000.0

            println()
            println("=== Sync Complete ===")
            println("Time elapsed: ${elapsed}s")
            println()
            println("=== Sync Report ===")
            println("Pages processed: ${report.pages}")
            println("Titles upserted: ${report.titlesUpserted}")
            println("External IDs upserted: ${report.externalIdsUpserted}")
            println("Genres upserted: ${report.genresUpserted}")
            println("People upserted: ${report.peopleUpserted}")
            println("Title-Genre refs: ${report.titleGenreRefs}")
            println("Title-Person refs: ${report.titlePersonRefs}")
            println()
            println("Database title count: $initialCount -> $finalCount (+${finalCount - initialCount})")
            println()
            println("SUCCESS!")

        } catch (e: Exception) {
            val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
            println()
            println("=== Sync Failed ===")
            println("Time elapsed: ${elapsed}s")
            println("Error: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Syncs top shows for a single streaming service.
     * This will fetch the top shows list and upsert all titles with their metadata.
     *
     * Run with:
     *   ./gradlew :app:testDebugUnitTest --tests "io.github.lauramiron.nextuptv.LibrarySyncCliTest.syncTopShowsForOneService"
     */
    @Test
    fun syncTopShowsForOneService() = runBlocking {
        val service = StreamingService.NETFLIX
        println("=== Sync Top Shows for ${service.id.uppercase()} ===")
        println()

        val startTime = System.currentTimeMillis()

        try {
            val initialCount = db.titleDao().countAll()
            println("Initial title count: $initialCount")
            println()

            println("Fetching and syncing top shows for ${service.id}...")
            repository.syncTopShows(service)

            val finalCount = db.titleDao().countAll()
            val elapsed = (System.currentTimeMillis() - startTime) / 1000.0

            println()
            println("=== Sync Complete ===")
            println("Time elapsed: ${elapsed}s")
            println("Database title count: $initialCount -> $finalCount (+${finalCount - initialCount})")
            println()
            println("SUCCESS!")

        } catch (e: Exception) {
            val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
            println()
            println("=== Sync Failed ===")
            println("Time elapsed: ${elapsed}s")
            println("Error: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Syncs top shows for all supported streaming services.
     * This will iterate through all StreamingService enum values and sync each one.
     *
     * Run with:
     *   ./gradlew :app:testDebugUnitTest --tests "io.github.lauramiron.nextuptv.LibrarySyncCliTest.syncTopShowsForAllServices"
     */
    @Test
    fun syncTopShowsForAllServices() = runBlocking {
        println("=== Sync Top Shows for ALL Services ===")
        println()

        val startTime = System.currentTimeMillis()
        val results = mutableMapOf<StreamingService, Boolean>()

        try {
            val initialCount = db.titleDao().countAll()
            println("Initial title count: $initialCount")
            println()

            // Sync top shows for each service
            StreamingService.entries.forEach { service ->
                println("--- Syncing ${service.id.uppercase()} ---")
                try {
                    repository.syncTopShows(service)
                    results[service] = true
                    println("✓ ${service.id.uppercase()} completed successfully")
                } catch (e: Exception) {
                    results[service] = false
                    println("✗ ${service.id.uppercase()} failed: ${e.message}")
                    e.printStackTrace()
                }
                println()
            }

            val finalCount = db.titleDao().countAll()
            val elapsed = (System.currentTimeMillis() - startTime) / 1000.0

            println()
            println("=== All Services Sync Complete ===")
            println("Time elapsed: ${elapsed}s")
            println("Database title count: $initialCount -> $finalCount (+${finalCount - initialCount})")
            println()
            println("=== Results Summary ===")
            results.forEach { (service, success) ->
                val status = if (success) "✓ SUCCESS" else "✗ FAILED"
                println("${service.id.uppercase()}: $status")
            }
            println()

            val successCount = results.values.count { it }
            val totalCount = results.size
            println("Overall: $successCount/$totalCount services synced successfully")

        } catch (e: Exception) {
            val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
            println()
            println("=== Sync Failed ===")
            println("Time elapsed: ${elapsed}s")
            println("Error: ${e.message}")
            e.printStackTrace()
        }
    }
}

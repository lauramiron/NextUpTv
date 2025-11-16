package io.github.lauramiron.nextuptv

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.lauramiron.nextuptv.data.LibraryRepository
import io.github.lauramiron.nextuptv.data.local.AppDb
import io.github.lauramiron.nextuptv.data.local.DatabaseProvider
import io.github.lauramiron.nextuptv.util.StreamingService
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
            api = MovieNightApiFactory.create(
                apiKey = "96da59657emsh4a212c55a8a0cdep152371jsnc0a31a8bc448",
                debugLogs = true
            ),
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
     * Helper function to run a sync for any streaming service with optional page limit.
     * This is NOT a real test - it's a CLI wrapper.
     * It will always "pass" even if sync encounters errors.
     *
     * @param service The streaming service to sync
     * @param maxPages Maximum number of pages to sync, or -1 for full sync (default)
     * @param resume If true, attempts to resume from the last sync's cursor; if false, starts from beginning (default)
     */
    private suspend fun trySyncService(service: StreamingService, maxPages: Int = -1, resume: Boolean = false) {
        try {
            repository.syncService(service, maxPages, resume)
        } catch (e: Exception) {
            // Error already printed by repository.syncAll()
            // Don't throw - let the test "pass" so you can see the output
        }
    }

    /**
     * This is NOT a real test - it's a CLI wrapper.
     * It will always "pass" even if sync encounters errors.
     *
     * Use this to run the full Netflix sync without test timeout constraints.
     */
    @Test
    fun runFullNetflixSync() = runBlocking {
        trySyncService(StreamingService.NETFLIX)
    }

    /**
     * This is NOT a real test - it's a CLI wrapper.
     * It will always "pass" even if sync encounters errors.
     *
     * Use this to run the full Apple sync without test timeout constraints.
     */
    @Test
    fun runFullAppleSync() = runBlocking {
        trySyncService(StreamingService.APPLE)
    }

    /**
     * This is NOT a real test - it's a CLI wrapper.
     * It will always "pass" even if sync encounters errors.
     *
     * Use this to run the full HBO sync without test timeout constraints.
     */
    @Test
    fun runFullHboSync() = runBlocking {
        trySyncService(StreamingService.HBO)
    }

    /**
     * This is NOT a real test - it's a CLI wrapper.
     * It will always "pass" even if sync encounters errors.
     *
     * Use this to run the full Prime sync without test timeout constraints.
     */
    @Test
    fun runFullPrimeSync() = runBlocking {
        trySyncService(StreamingService.PRIME, resume = true)
    }

    /**
     * Partial Netflix sync - limits to a specific number of pages.
     * Useful for testing without waiting for the full sync.
     */
    @Test
    fun runPartialNetflixSync() = runBlocking {
        trySyncService(StreamingService.NETFLIX, maxPages = 5)
    }

    /**
     * Partial Apple sync - limits to a specific number of pages.
     * Useful for testing without waiting for the full sync.
     */
    @Test
    fun runPartialAppleSync() = runBlocking {
        trySyncService(StreamingService.APPLE, maxPages = 5, resume = true)
    }
    /**
     * Partial Prime sync - limits to a specific number of pages.
     * Useful for testing without waiting for the full sync.
     */
    @Test
    fun runPartialPrimeSync() = runBlocking {
        trySyncService(StreamingService.PRIME, maxPages = 400, resume = true)
    }
    /**
     * Partial hulu sync - limits to a specific number of pages.
     * Useful for testing without waiting for the full sync.
     */
    @Test
    fun runPartialHuluSync() = runBlocking {
        trySyncService(StreamingService.HULU, maxPages = 1, resume = false)
    }
    /**
     * Partial Hbo sync - limits to a specific number of pages.
     * Useful for testing without waiting for the full sync.
     */
    @Test
    fun runPartialHboSync() = runBlocking {
        trySyncService(StreamingService.HBO, maxPages = 50, resume = true)
    }
    /**
     * Partial Hbo sync - limits to a specific number of pages.
     * Useful for testing without waiting for the full sync.
     */
    @Test
    fun runPartialParamountSync() = runBlocking {
        trySyncService(StreamingService.PARAMOUNT, maxPages = 30, resume = true)
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
        val service = StreamingService.HBO
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

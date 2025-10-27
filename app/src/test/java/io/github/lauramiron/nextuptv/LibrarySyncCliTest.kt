package io.github.lauramiron.nextuptv

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.lauramiron.nextuptv.data.LibraryRepository
import io.github.lauramiron.nextuptv.data.local.AppDb
import io.github.lauramiron.nextuptv.data.remote.movienight.MovieNightApiFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

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

        // Create database directly in app's files directory where it will be used
        val dbFile = File(context.filesDir, "wm-snapshot.db")

        println("Database location: ${dbFile.absolutePath}")
        println()

        db = Room.databaseBuilder(
            context,
            AppDb::class.java,
            dbFile.absolutePath
        ).fallbackToDestructiveMigration().build()

        repository = LibraryRepository(
            api = MovieNightApiFactory.create(apiKey = "96da59657emsh4a212c55a8a0cdep152371jsnc0a31a8bc448"),
            db = db,
            io = Dispatchers.Default
        )
    }

    @After
    fun tearDown() {
        val dbPath = db.openHelper.writableDatabase.path
        println()
        println("Snapshot saved to: $dbPath")
        println("This will be automatically loaded when running debug builds on emulator")
        db.close()
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
}

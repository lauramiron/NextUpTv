package io.github.lauramiron.nextuptv

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.lauramiron.nextuptv.data.ResumeRepository
import io.github.lauramiron.nextuptv.data.local.AppDb
import io.github.lauramiron.nextuptv.data.local.DatabaseProvider
import io.github.lauramiron.nextuptv.data.local.entity.StreamingService
import io.github.lauramiron.nextuptv.data.remote.resume.ResumeApiFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test is designed to be run manually to sync resume data from the scraper API.
 *
 * Run with:
 *   ./gradlew :app:testDebugUnitTest --tests "io.github.lauramiron.nextuptv.ResumeSyncCliTest.syncResumeData"
 *
 * Or from Android Studio:
 *   Right-click on the test method > Run 'syncResumeData()'
 */
@RunWith(AndroidJUnit4::class)
class ResumeSyncCliTest {

    private lateinit var context: Context
    private lateinit var db: AppDb
    private lateinit var repository: ResumeRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Use DatabaseProvider singleton - it will load from snapshot if available
        db = DatabaseProvider.getInstance(context)

        repository = ResumeRepository(
            api = ResumeApiFactory.create(debugLogs = true),
            db = db
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
     * Use this to fetch resume data from the scraper API and insert it into the database.
     */
    @Test
    fun syncResumeData() = runBlocking {
        println("=== Resume Data Sync ===")
        println("Fetching resume data from https://ss-scraper.onrender.com/resume")
        println()

        val startTime = System.currentTimeMillis()

        try {
            // Get initial count of resume entries
            val initialCount = db.resumeDao().countAll()
            println("Initial resume entry count: $initialCount")
            println()

            // Run the sync
            println("Starting sync...")
            repository.syncResume(limit = 50)

            // Get final count
            val finalCount = db.resumeDao().countAll()
            val elapsed = (System.currentTimeMillis() - startTime) / 1000.0

            println()
            println("=== Sync Complete ===")
            println("Time elapsed: ${elapsed}s")
            println("Resume entry count: $initialCount -> $finalCount (+${finalCount - initialCount})")
            println()

            // Show breakdown by service
            println("=== Resume Entries by Service ===")
            StreamingService.entries.forEach { service ->
                try {
                    val count = db.resumeDao().countByService(service)
                    if (count > 0) {
                        println("${service.id}: $count entries")
                    }
                } catch (e: Exception) {
                    // Skip if there's an error
                }
            }
            println()

            // Show resolution status
            val unresolvedCount = db.resumeDao().countUnresolved()
            val resolvedCount = finalCount - unresolvedCount
            println("=== Resolution Status ===")
            println("Resolved to titles: $resolvedCount")
            println("Unresolved: $unresolvedCount")
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
}

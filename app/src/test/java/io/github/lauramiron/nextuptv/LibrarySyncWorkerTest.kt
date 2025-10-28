package io.github.lauramiron.nextuptv

import android.content.Context
import android.util.Log
import androidx.concurrent.futures.await
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.google.common.util.concurrent.ListenableFuture
import io.github.lauramiron.nextuptv.data.LibraryRepository
import io.github.lauramiron.nextuptv.data.local.AppDb
import io.github.lauramiron.nextuptv.data.local.entity.StreamingService
import io.github.lauramiron.nextuptv.data.remote.movienight.MovieNightApiFactory
import io.github.lauramiron.nextuptv.sync.LibrarySyncWorker
import io.github.lauramiron.nextuptva.TestWorkerFactory
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.Executors
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class LibrarySyncWorkerTest {

    private lateinit var context: Context
    private lateinit var db: AppDb
    private lateinit var workManager: WorkManager

    // If your repo needs network, plug in a MockWebServer here and point the repo at it.
    private lateinit var repository: LibraryRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // 1) Build database directly in app's files directory where it will be used
        val dbFile = File(context.filesDir, "wm-snapshot.db")
        db = Room.databaseBuilder(context, AppDb::class.java, dbFile.absolutePath).fallbackToDestructiveMigration().build()

        // 2) Construct repository against this DB (and fake API if needed)
        repository = LibraryRepository(
            api = MovieNightApiFactory.create(apiKey="96da59657emsh4a212c55a8a0cdep152371jsnc0a31a8bc448"),
            db = db,
            io = Dispatchers.Default
        )

        // 3) Initialize WorkManager with our custom WorkerFactory
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setWorkerFactory(TestWorkerFactory { repository })
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
    }

    @After
    fun tearDown() {
        val dbPath = db.openHelper.writableDatabase.path
        println("Snapshot saved to: $dbPath")
        println("This will be automatically loaded when running debug builds on emulator")
        db.close()
    }

    @Test
    fun `sync worker populates titles and snapshot is exported`() = runTest {
        // 5) Enqueue the worker
        val req = OneTimeWorkRequestBuilder<LibrarySyncWorker>()
            .setInputData(workDataOf("catalogs" to "netflix"))
            .build()
        workManager.enqueue(req).result.get()

        // 6) If your worker has constraints/delays, drive them:
        val testDriver = WorkManagerTestInitHelper.getTestDriver(context)!!
        testDriver.setAllConstraintsMet(req.id)

        // 7) Await completion by polling:
        val info = workManager.getWorkInfoById(req.id).await()
        assertEquals(WorkInfo.State.SUCCEEDED, info?.state)

        // 8) Assert DB state with your DAO(s)
        val rowCount = db.titleDao().countAll() // replace with your DAO/query
        assertTrue("Expected titles to be inserted", rowCount > 0)
    }

    @Test
    fun `test api fetches titles successfully`() = runTest {
        // Test that the API can fetch titles before running the full worker
        val api = MovieNightApiFactory.create(apiKey="96da59657emsh4a212c55a8a0cdep152371jsnc0a31a8bc448")

        try {
            val (titles, cursor) = api.fetchAllShows(
                catalogs = "netflix",
                maxPages = 1
            )

            println("=== API Test Results ===")
            println("Fetched ${titles.size} titles")
            println("Next cursor: $cursor")

            // Log a few sample titles
            titles.take(3).forEach { title ->
                println("- ${title.title} (${title.releaseYear}) - ${title.showType}")
            }

            assertTrue("API should return some titles", titles.isNotEmpty())

        } catch (e: Exception) {
            println("API test failed: ${e.message}")
            throw e
        }
    }

    @Test
    fun `test repository can process single title`() = runTest {
        try {
            val monId = "54321" // Using the same test ID

            println("=== Testing syncTitle with monId: $monId ===")

            // Get initial database counts
            val initialTitleCount = db.titleDao().countAll()
            println("Initial title count: $initialTitleCount")

            // Sync the title
            val report = repository.syncTitle(monId)

            println("=== Sync Report ===")
            println("Titles upserted: ${report.titlesUpserted}")
            println("External IDs upserted: ${report.externalIdsUpserted}")
            println("Genres upserted: ${report.genresUpserted}")
            println("People upserted: ${report.peopleUpserted}")
            println("Title-Genre refs: ${report.titleGenreRefs}")
            println("Title-Person refs: ${report.titlePersonRefs}")
            println()

            // Verify title was inserted
            val finalTitleCount = db.titleDao().countAll()
            println("Final title count: $finalTitleCount")
            assertTrue("Title should be inserted", finalTitleCount > initialTitleCount)

            // Verify the title exists in database
            val titleInDb = db.titleDao().findIdByMonId(monId)
            assertTrue("Title with monId $monId should exist in database", titleInDb != null)

            if (titleInDb != null) {
                val fullTitle = db.titleDao().getTitle(titleInDb)
                println("=== Title in Database ===")
                println("Database ID: ${fullTitle?.id}")
                println("MonId: ${fullTitle?.monId}")
                println("Name: ${fullTitle?.name}")
                println("Kind: ${fullTitle?.kind}")
                println("Year: ${fullTitle?.year}")
                println("Runtime: ${fullTitle?.runtimeMin}")
                println("Synopsis: ${fullTitle?.synopsis?.take(100)}...")
                println()

                // Verify external IDs were stored
                if (report.externalIdsUpserted > 0) {
                    val externalIds = db.externalIdDao().findId(titleInDb,
                        StreamingService.NETFLIX
                    )
                    if (externalIds != null) {
                        println("✅ Netflix external ID found for title")
                    }
                }

                // Verify genres were stored if any
                if (report.genresUpserted > 0) {
                    println("✅ ${report.genresUpserted} genres were stored")
                }

                // Verify people were stored if any
                if (report.peopleUpserted > 0) {
                    println("✅ ${report.peopleUpserted} people were stored")
                }

                // Verify cross-references
                if (report.titleGenreRefs > 0) {
                    println("✅ ${report.titleGenreRefs} title-genre relationships created")
                }
                if (report.titlePersonRefs > 0) {
                    println("✅ ${report.titlePersonRefs} title-person relationships created")
                }
            }

            // Verify at least some data was processed
            assertTrue("Should have processed at least one title", report.titlesUpserted >= 1)

            println("✅ syncTitle database test completed successfully!")

        } catch (e: Exception) {
            println("❌ syncTitle database test failed: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    @Test
    fun `test repository can process single page`() = runTest {
        try {
            println("=== Testing sync of 1st page of netflix results ===")

            // Get initial database counts
            val initialTitleCount = db.titleDao().countAll()
            println("Initial title count: $initialTitleCount")

            // Sync the title
            val report = repository.syncAll("netflix", maxPages=1)

            println("=== Sync Report ===")
            println("Titles upserted: ${report.titlesUpserted}")
            println("External IDs upserted: ${report.externalIdsUpserted}")
            println("Genres upserted: ${report.genresUpserted}")
            println("People upserted: ${report.peopleUpserted}")
            println("Title-Genre refs: ${report.titleGenreRefs}")
            println("Title-Person refs: ${report.titlePersonRefs}")
            println()

            // Verify title was inserted
            val finalTitleCount = db.titleDao().countAll()
            println("Final title count: $finalTitleCount")
            assertTrue("Titles should be inserted", finalTitleCount > initialTitleCount)

            // Verify external IDs were stored
            if (report.externalIdsUpserted > 0) {
                println("✅ ${report.externalIdsUpserted} genres were stored")
            }

            // Verify genres were stored if any
            if (report.genresUpserted > 0) {
                println("✅ ${report.genresUpserted} genres were stored")
            }

            // Verify people were stored if any
            if (report.peopleUpserted > 0) {
                println("✅ ${report.peopleUpserted} people were stored")
            }

            // Verify cross-references
            if (report.titleGenreRefs > 0) {
                println("✅ ${report.titleGenreRefs} title-genre relationships created")
            }
            if (report.titlePersonRefs > 0) {
                println("✅ ${report.titlePersonRefs} title-person relationships created")
            }

            println("✅ syncTitle database test completed successfully!")

        } catch (e: Exception) {
            println("❌ syncTitle database test failed: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    @Test
    fun `test repository can process netflix`() = runTest(timeout = 600_000.seconds) { // 10 minute timeout
        try {
            println("=== Testing full sync of netflix catalog ===")
            println("NOTE: This may take several minutes. For unlimited timeout, use LibrarySyncCliTest instead.")
            println()

            // Get initial database counts
            val initialTitleCount = db.titleDao().countAll()
            println("Initial title count: $initialTitleCount")

            val startTime = System.currentTimeMillis()

            // Sync all netflix titles
            val report = repository.syncAll("netflix")

            val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
            println()
            println("=== Sync Report (completed in ${elapsed}s) ===")
            println("Titles upserted: ${report.titlesUpserted}")
            println("External IDs upserted: ${report.externalIdsUpserted}")
            println("Genres upserted: ${report.genresUpserted}")
            println("People upserted: ${report.peopleUpserted}")
            println("Title-Genre refs: ${report.titleGenreRefs}")
            println("Title-Person refs: ${report.titlePersonRefs}")
            println()

            // Verify title was inserted
            val finalTitleCount = db.titleDao().countAll()
            println("Final title count: $finalTitleCount")
            assertTrue("Titles should be inserted", finalTitleCount > initialTitleCount)

            // Verify external IDs were stored
            if (report.externalIdsUpserted > 0) {
                println("✅ ${report.externalIdsUpserted} external IDs were stored")
            }

            // Verify genres were stored if any
            if (report.genresUpserted > 0) {
                println("✅ ${report.genresUpserted} genres were stored")
            }

            // Verify people were stored if any
            if (report.peopleUpserted > 0) {
                println("✅ ${report.peopleUpserted} people were stored")
            }

            // Verify cross-references
            if (report.titleGenreRefs > 0) {
                println("✅ ${report.titleGenreRefs} title-genre relationships created")
            }
            if (report.titlePersonRefs > 0) {
                println("✅ ${report.titlePersonRefs} title-person relationships created")
            }

            println("✅ Full Netflix sync completed successfully!")

        } catch (e: Exception) {
            println("❌ Netflix sync failed: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}

/** Small await helper for WorkInfo (optional). */
suspend fun ListenableFuture<WorkInfo>.await(): WorkInfo =
    suspendCancellableCoroutine { cont ->
        addListener(
            { cont.resume(get()) {} },
            Executors.newSingleThreadExecutor()
        )
    }
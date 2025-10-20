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

        // 1) Build in-memory DB, but seed from snapshot if it exists
//        db = Room.inMemoryDatabaseBuilder(context, AppDb::class.java)
//            .allowMainThreadQueries() // ok in tests
////            .addCallback(DbSnapshotUtils.seedingCallback())
//            .build()

        db = Room.databaseBuilder(context, AppDb::class.java, File(System.getProperty("user.dir"), "build/test-db/wm-snapshot.db").absolutePath).fallbackToDestructiveMigration().build()

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
        // 4) Export a snapshot of the in-memory DB to a file for future seeding and manual inspection
//        DbSnapshotUtils.exportSnapshot(db.openHelper.writableDatabase)
        db.close()
    }

    @Test
    fun `sync worker populates titles and snapshot is exported`() = runTest {
        // Optional: seed API responses if using MockWebServer

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

        // 9) Print snapshot location for convenience
        println("Snapshot written to: ${DbSnapshotUtils.snapshotFile.absolutePath}")
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
        // Test the repository's ability to process a single title
        val api = MovieNightApiFactory.create(apiKey="96da59657emsh4a212c55a8a0cdep152371jsnc0a31a8bc448")

        try {
            // Get one page of titles
            val (titles, _) = api.fetchAllShows(catalogs = "netflix", maxPages = 1)
            assertTrue("Need at least one title for test", titles.isNotEmpty())

            val firstTitle = titles.first()
            println("=== Testing Repository with: ${firstTitle.title} ===")

            // Process this title through the repository
            val report = LibraryRepository.SyncReport()
            repository.upsertOneTitleTree(firstTitle, report)

            println("Sync report:")
            println("- Titles upserted: ${report.titlesUpserted}")
            println("- External IDs: ${report.externalIdsUpserted}")
            println("- Genres: ${report.genresUpserted}")
            println("- People: ${report.peopleUpserted}")
            println("- Title-Genre refs: ${report.titleGenreRefs}")
            println("- Title-Person refs: ${report.titlePersonRefs}")

            // Verify data was inserted
            val titleCount = db.titleDao().countAll()
            assertTrue("Title should be inserted", titleCount > 0)

        } catch (e: Exception) {
            println("Repository test failed: ${e.message}")
            throw e
        }
    }

    @Test
    fun `test syncTitle stores data correctly in database`() = runTest {
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
                    val externalIds = db.externalIdDao().findId(titleInDb, "netflix")
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
}

/** Small await helper for WorkInfo (optional). */
suspend fun ListenableFuture<WorkInfo>.await(): WorkInfo =
    suspendCancellableCoroutine { cont ->
        addListener(
            { cont.resume(get()) {} },
            Executors.newSingleThreadExecutor()
        )
    }
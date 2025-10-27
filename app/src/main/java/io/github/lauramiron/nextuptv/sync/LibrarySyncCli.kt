package io.github.lauramiron.nextuptv.sync

import android.content.Context
import androidx.room.Room
import io.github.lauramiron.nextuptv.data.LibraryRepository
import io.github.lauramiron.nextuptv.data.local.AppDb
import io.github.lauramiron.nextuptv.data.remote.movienight.MovieNightApiFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Command-line utility to run the full Netflix sync without test timeouts.
 *
 * Usage from Android Studio:
 * 1. Create a Run Configuration: Run > Edit Configurations > Add New > Android App
 * 2. Set the module to 'app'
 * 3. Set the launch activity to this class as a Kotlin script
 *
 * Or run from command line with Gradle task (see app/build.gradle.kts)
 */
//object LibrarySyncCli {
//
//    /**
//     * Run the full Netflix sync operation.
//     *
//     * @param context Android application context
//     * @param catalog Which catalog to sync (default: "netflix")
//     * @param maxPages Maximum pages to fetch (null = all pages)
//     * @param dbPath Custom database path (null = use default app database)
//     */
//    fun runSync(
//        context: Context,
//        catalog: String = "netflix",
//        maxPages: Int? = null,
//        dbPath: String? = null,
//        apiKey: String = "96da59657emsh4a212c55a8a0cdep152371jsnc0a31a8bc448"
//    ) = runBlocking {
//        println("=== Starting Library Sync CLI ===")
//        println("Catalog: $catalog")
//        println("Max Pages: ${maxPages ?: "unlimited"}")
//        println()
//
//        val startTime = System.currentTimeMillis()
//
//        try {
//            // Build database
//            val db = if (dbPath != null) {
//                Room.databaseBuilder(context, AppDb::class.java, dbPath)
//                    .fallbackToDestructiveMigration()
//                    .build()
//            } else {
//                Room.databaseBuilder(context, AppDb::class.java, "nextuptv.db")
//                    .fallbackToDestructiveMigration()
//                    .build()
//            }
//
//            // Create repository
//            val repository = LibraryRepository(
//                api = MovieNightApiFactory.create(apiKey = apiKey),
//                db = db,
//                io = Dispatchers.IO
//            )
//
//            // Get initial count
//            val initialCount = db.titleDao().countAll()
//            println("Initial title count: $initialCount")
//            println()
//
//            // Run the sync
//            println("Starting sync... (this may take several minutes)")
//            val report = repository.syncAll(
//                catalogs = catalog,
//                maxPages = maxPages
//            )
//
//            // Print results
//            val finalCount = db.titleDao().countAll()
//            val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
//
//            println()
//            println("=== Sync Complete ===")
//            println("Time elapsed: ${elapsed}s")
//            println()
//            println("=== Sync Report ===")
//            println("Pages processed: ${report.pages}")
//            println("Titles upserted: ${report.titlesUpserted}")
//            println("External IDs upserted: ${report.externalIdsUpserted}")
//            println("Genres upserted: ${report.genresUpserted}")
//            println("People upserted: ${report.peopleUpserted}")
//            println("Title-Genre refs: ${report.titleGenreRefs}")
//            println("Title-Person refs: ${report.titlePersonRefs}")
//            println()
//            println("Database title count: $initialCount -> $finalCount (+${finalCount - initialCount})")
//            println()
//            println("SUCCESS")
//
//            db.close()
//
//        } catch (e: Exception) {
//            val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
//            println()
//            println("=== Sync Failed ===")
//            println("Time elapsed: ${elapsed}s")
//            println("Error: ${e.message}")
//            e.printStackTrace()
//            throw e
//        }
//    }
//
//    /**
//     * Simpler version that uses a test database file (like in the tests).
//     * Useful for running sync without affecting the main app database.
//     */
//    fun runSyncToTestDb(
//        context: Context,
//        catalog: String = "netflix",
//        maxPages: Int? = null,
//        apiKey: String = "96da59657emsh4a212c55a8a0cdep152371jsnc0a31a8bc448"
//    ) {
//        val testDbFile = File(System.getProperty("user.dir"), "build/sync-cli/netflix-sync.db")
//        testDbFile.parentFile?.mkdirs()
//
//        println("Database file: ${testDbFile.absolutePath}")
//        println()
//
//        runSync(
//            context = context,
//            catalog = catalog,
//            maxPages = maxPages,
//            dbPath = testDbFile.absolutePath,
//            apiKey = apiKey
//        )
//    }
//}

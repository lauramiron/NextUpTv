package io.github.lauramiron.nextuptv

import android.app.Application
import android.util.Log
import io.github.lauramiron.nextuptv.data.LibraryRepository
import io.github.lauramiron.nextuptv.data.local.DatabaseProvider
import io.github.lauramiron.nextuptv.data.local.entity.StreamingService
import io.github.lauramiron.nextuptv.data.remote.movienight.MovieNightApiFactory
import kotlinx.coroutines.runBlocking

class NextUpTvApplication : Application() {

    lateinit var libraryRepository: LibraryRepository
        private set

    override fun onCreate() {
        super.onCreate()

        Log.i(TAG, "========================================")
        Log.i(TAG, "NextUpTvApplication.onCreate() starting")
        Log.i(TAG, "========================================")

        // Initialize database
        Log.d(TAG, "Initializing DatabaseProvider...")
        DatabaseProvider.initialize(this)

        // Create API instance (you'll need to provide your RapidAPI key)
        val apiKey = BuildConfig.RAPIDAPI_KEY // Add this to your build.gradle.kts
        val api = MovieNightApiFactory.create(
            apiKey = apiKey,
            debugLogs = BuildConfig.DEBUG
        )

        // Create repository
        Log.d(TAG, "Getting database instance...")
        val db = DatabaseProvider.getInstance(this)
        Log.d(TAG, "Database instance: $db")
        Log.d(TAG, "Database class: ${db.javaClass.name}")

        // Check if DAOs are accessible
        try {
            val titleDao = db.titleDao()
            Log.d(TAG, "TitleDao: $titleDao")
        } catch (e: Exception) {
            Log.e(TAG, "ERROR: Failed to get TitleDao", e)
        }

        libraryRepository = LibraryRepository(api, db)

        // DEBUG: Check database title count at startup
        runBlocking {
            try {
                val titleCount = db.titleDao().countAll()
                Log.i(TAG, "========================================")
                Log.i(TAG, "Database loaded at startup")
                Log.i(TAG, "Total titles in database: $titleCount")
                Log.i(TAG, "========================================")

                // Also log if we have any popularity data
                try {
                    val netflixTop = db.popularityDao().getTopShowsForService(StreamingService.NETFLIX)
                    Log.i(TAG, "Netflix top shows count: ${netflixTop.size}")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not query Netflix top shows: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "FATAL ERROR: Could not query database", e)
            }
        }

        Log.i(TAG, "NextUpTvApplication.onCreate() completed")
        Log.i(TAG, "========================================")
    }

    companion object {
        private const val TAG = "NextUpTvApplication"

        /**
         * Helper extension function to get LibraryRepository from any Context
         */
        fun getRepository(context: android.content.Context): LibraryRepository {
            return (context.applicationContext as NextUpTvApplication).libraryRepository
        }
    }
}

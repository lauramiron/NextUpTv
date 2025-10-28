package io.github.lauramiron.nextuptv

import android.app.Application
import io.github.lauramiron.nextuptv.data.LibraryRepository
import io.github.lauramiron.nextuptv.data.local.DatabaseProvider
import io.github.lauramiron.nextuptv.data.remote.movienight.MovieNightApiFactory

class NextUpTvApplication : Application() {

    lateinit var libraryRepository: LibraryRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize database
        DatabaseProvider.initialize(this)

        // Create API instance (you'll need to provide your RapidAPI key)
        val apiKey = BuildConfig.RAPIDAPI_KEY // Add this to your build.gradle.kts
        val api = MovieNightApiFactory.create(
            apiKey = apiKey,
            debugLogs = BuildConfig.DEBUG
        )

        // Create repository
        val db = DatabaseProvider.getInstance(this)
        libraryRepository = LibraryRepository(api, db)
    }

    companion object {
        /**
         * Helper extension function to get LibraryRepository from any Context
         */
        fun getRepository(context: android.content.Context): LibraryRepository {
            return (context.applicationContext as NextUpTvApplication).libraryRepository
        }
    }
}

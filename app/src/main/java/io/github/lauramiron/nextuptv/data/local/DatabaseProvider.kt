package io.github.lauramiron.nextuptv.data.local

import android.content.Context
import androidx.room.Room
import io.github.lauramiron.nextuptv.BuildConfig
import java.io.File

/**
 * Provides a singleton instance of the AppDb database.
 *
 * In debug builds, it can load from a pre-populated snapshot file for faster development.
 */
object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDb? = null

    fun initialize(context: Context) {
        if (INSTANCE == null) {
            synchronized(this) {
                if (INSTANCE == null) {
                    INSTANCE = createDatabase(context.applicationContext)
                }
            }
        }
    }

    fun getInstance(context: Context): AppDb {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: createDatabase(context.applicationContext).also { INSTANCE = it }
        }
    }

    private fun createDatabase(context: Context): AppDb {
        val builder = Room.databaseBuilder(
            context,
            AppDb::class.java,
            "nextuptv.db"
        )

        // In debug builds, try to load from snapshot file in app's files directory
        if (BuildConfig.DEBUG) {
            val snapshotFile = File(context.filesDir, "wm-snapshot.db")

            if (snapshotFile.exists()) {
                android.util.Log.d("DatabaseProvider", "Loading database from snapshot: ${snapshotFile.absolutePath}")
                builder.createFromFile(snapshotFile)
            } else {
                android.util.Log.d("DatabaseProvider", "No snapshot file found. Creating empty database. Run tests to generate snapshot.")
                builder.fallbackToDestructiveMigration()
            }
        } else {
            builder.fallbackToDestructiveMigration()
        }

        return builder.build()
    }

    /**
     * For testing only - allows replacing the instance
     */
    fun setInstance(instance: AppDb) {
        INSTANCE = instance
    }

    /**
     * Clear the instance - useful for testing
     */
    fun reset() {
        INSTANCE?.close()
        INSTANCE = null
    }
}

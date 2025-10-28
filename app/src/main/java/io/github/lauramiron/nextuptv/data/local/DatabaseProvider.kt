package io.github.lauramiron.nextuptv.data.local

import android.content.Context
import androidx.room.Room
import io.github.lauramiron.nextuptv.BuildConfig
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Provides a singleton instance of the AppDb database.
 *
 * In debug builds, it can load from a pre-populated snapshot file for faster development.
 * Snapshots are stored in a persistent location that works across tests, emulator, and device.
 */
object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDb? = null

    private const val SNAPSHOT_FILENAME = "wm-snapshot.db"
    private const val DB_NAME = "nextuptv.db"

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

    /**
     * Get the persistent snapshot directory based on the runtime environment.
     * - In Robolectric tests: Uses project's app/src/main/assets directory
     * - In app runtime: Uses app's files directory
     */
    private fun getSnapshotDirectory(context: Context): File {
        return if (isRunningInRobolectric()) {
            // Use project assets directory for tests
            val projectRoot = System.getProperty("user.dir") ?: ""
            File(projectRoot, "src/main/assets").apply { mkdirs() }
        } else {
            // Use app files directory for runtime
            context.filesDir
        }
    }

    /**
     * Detect if running under Robolectric test environment
     */
    private fun isRunningInRobolectric(): Boolean {
        return try {
            Class.forName("org.robolectric.Robolectric")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    /**
     * Find a snapshot file to load from, checking multiple locations:
     * 1. Assets (bundled with app)
     * 2. Snapshot directory (persistent storage)
     */
    private fun findSnapshotFile(context: Context): File? {
        // First check if snapshot exists in assets (bundled with app)
        try {
            context.assets.open(SNAPSHOT_FILENAME).use { inputStream ->
                // Found in assets, copy to files directory for Room to use
                val tempFile = File(context.filesDir, SNAPSHOT_FILENAME)
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                android.util.Log.d("DatabaseProvider", "Copied snapshot from assets to: ${tempFile.absolutePath}")
                return tempFile
            }
        } catch (e: Exception) {
            // Not in assets, continue to check other locations
        }

        // Check snapshot directory
        val snapshotDir = getSnapshotDirectory(context)
        val snapshotFile = File(snapshotDir, SNAPSHOT_FILENAME)
        return if (snapshotFile.exists()) snapshotFile else null
    }

    private fun createDatabase(context: Context): AppDb {
        val builder = Room.databaseBuilder(
            context,
            AppDb::class.java,
            DB_NAME
        )

        // In debug builds, try to load from snapshot
        if (BuildConfig.DEBUG) {
            val snapshotFile = findSnapshotFile(context)

            if (snapshotFile != null && snapshotFile.exists()) {
                android.util.Log.d("DatabaseProvider", "Loading database from snapshot: ${snapshotFile.absolutePath}")
                builder.createFromFile(snapshotFile)
            } else {
                val snapshotDir = getSnapshotDirectory(context)
                android.util.Log.d("DatabaseProvider", "No snapshot file found. Creating empty database.")
                android.util.Log.d("DatabaseProvider", "Snapshots will be saved to: ${File(snapshotDir, SNAPSHOT_FILENAME).absolutePath}")
                android.util.Log.d("DatabaseProvider", "Run tests to generate snapshot, or place $SNAPSHOT_FILENAME in app/src/main/assets/")
                builder.fallbackToDestructiveMigration()
            }
        } else {
            builder.fallbackToDestructiveMigration()
        }

        return builder.build()
    }

    /**
     * Export the current database to a snapshot file.
     * This saves to the persistent snapshot directory which can be:
     * - Copied to assets for bundling with the app
     * - Used directly by tests
     *
     * Call this after running sync operations to create a snapshot.
     */
    fun exportSnapshot(context: Context): File? {
        val db = INSTANCE ?: return null

        // Close and checkpoint the database to ensure all data is written
        db.close()

        // Get the current database file
        val currentDbFile = context.getDatabasePath(DB_NAME)
        if (!currentDbFile.exists()) {
            android.util.Log.w("DatabaseProvider", "Database file not found: ${currentDbFile.absolutePath}")
            INSTANCE = null
            return null
        }

        // Copy to snapshot location
        val snapshotDir = getSnapshotDirectory(context)
        val snapshotFile = File(snapshotDir, SNAPSHOT_FILENAME)

        try {
            FileInputStream(currentDbFile).use { input ->
                FileOutputStream(snapshotFile).use { output ->
                    input.copyTo(output)
                }
            }
            android.util.Log.i("DatabaseProvider", "Snapshot exported to: ${snapshotFile.absolutePath}")
            if (isRunningInRobolectric()) {
                println()
                println("=== Snapshot Exported ===")
                println("Location: ${snapshotFile.absolutePath}")
                println("This file is in app/src/main/assets/ and will be bundled with the app")
                println("When running on emulator/device, this snapshot will be automatically loaded")
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseProvider", "Failed to export snapshot", e)
            INSTANCE = null
            return null
        }

        // Reinitialize the database instance
        INSTANCE = null
        initialize(context)

        return snapshotFile
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

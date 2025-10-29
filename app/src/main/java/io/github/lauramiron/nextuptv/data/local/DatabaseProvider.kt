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
        android.util.Log.d("DatabaseProvider", "findSnapshotFile() - searching for snapshot...")

        // First check if snapshot exists in assets (bundled with app)
        android.util.Log.d("DatabaseProvider", "Checking assets for: $SNAPSHOT_FILENAME")
        try {
            context.assets.open(SNAPSHOT_FILENAME).use { inputStream ->
                // Found in assets, copy to files directory for Room to use
                val tempFile = File(context.filesDir, SNAPSHOT_FILENAME)
                android.util.Log.i("DatabaseProvider", "Found snapshot in assets, copying to: ${tempFile.absolutePath}")
                FileOutputStream(tempFile).use { outputStream ->
                    val bytesCopied = inputStream.copyTo(outputStream)
                    android.util.Log.i("DatabaseProvider", "Copied $bytesCopied bytes from assets")
                }
                return tempFile
            }
        } catch (e: Exception) {
            android.util.Log.d("DatabaseProvider", "Not found in assets: ${e.message}")
        }

        // Check snapshot directory
        val snapshotDir = getSnapshotDirectory(context)
        val snapshotFile = File(snapshotDir, SNAPSHOT_FILENAME)
        android.util.Log.d("DatabaseProvider", "Checking snapshot directory: ${snapshotFile.absolutePath}")

        return if (snapshotFile.exists()) {
            android.util.Log.i("DatabaseProvider", "Found snapshot in directory: ${snapshotFile.absolutePath} (${snapshotFile.length()} bytes)")
            snapshotFile
        } else {
            android.util.Log.w("DatabaseProvider", "Snapshot file not found anywhere")
            null
        }
    }

    private fun createDatabase(context: Context): AppDb {
        android.util.Log.d("DatabaseProvider", "createDatabase() called")
        android.util.Log.d("DatabaseProvider", "Context: ${context.javaClass.simpleName}")

        val builder = Room.databaseBuilder(
            context,
            AppDb::class.java,
            DB_NAME
        )

        // Try to load from snapshot
        android.util.Log.d("DatabaseProvider", "DEBUG build - checking for snapshot")
        val snapshotFile = findSnapshotFile(context)

        if (snapshotFile != null && snapshotFile.exists()) {
            android.util.Log.i("DatabaseProvider", "Found snapshot at: ${snapshotFile.absolutePath}")
            android.util.Log.i("DatabaseProvider", "Snapshot size: ${snapshotFile.length()} bytes")
            try {
                builder.createFromFile(snapshotFile)
                android.util.Log.i("DatabaseProvider", "createFromFile() called successfully")
            } catch (e: Exception) {
                android.util.Log.e("DatabaseProvider", "Error calling createFromFile()", e)
                builder.fallbackToDestructiveMigration()
            }
        } else {
            val snapshotDir = getSnapshotDirectory(context)
            android.util.Log.w("DatabaseProvider", "No snapshot file found. Creating empty database.")
            android.util.Log.d("DatabaseProvider", "Checked locations:")
            android.util.Log.d("DatabaseProvider", "  - Assets: $SNAPSHOT_FILENAME")
            android.util.Log.d("DatabaseProvider", "  - Snapshot dir: ${File(snapshotDir, SNAPSHOT_FILENAME).absolutePath}")
            builder.fallbackToDestructiveMigration(false)
        }

        android.util.Log.d("DatabaseProvider", "Building database...")
        val db = try {
            builder.build()
        } catch (e: Exception) {
            android.util.Log.e("DatabaseProvider", "FATAL: Failed to build database", e)
            throw e
        }

        android.util.Log.i("DatabaseProvider", "Database built successfully: ${db.javaClass.simpleName}")
        android.util.Log.d("DatabaseProvider", "Database path: ${context.getDatabasePath(DB_NAME).absolutePath}")

        return db
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

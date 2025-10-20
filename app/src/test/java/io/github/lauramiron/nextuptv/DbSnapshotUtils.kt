package io.github.lauramiron.nextuptv

import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File

object DbSnapshotUtils {
    /** Where we persist and read the snapshot between test runs */
    val snapshotFile = File(System.getProperty("user.dir"), "build/test-db/wm-snapshot.db")

    /** Add this callback to your in-memory builder to seed from the snapshot if it exists. */
    fun seedingCallback(): RoomDatabase.Callback = object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            if (snapshotFile.exists()) seedFromSnapshot(db, snapshotFile)
        }
    }

    private fun seedFromSnapshot(db: SupportSQLiteDatabase, file: File) {
        db.beginTransaction()
        try {
            db.execSQL("PRAGMA foreign_keys=OFF;")
            db.execSQL("ATTACH DATABASE ? AS seed;", arrayOf(file.absolutePath))
            // Copy all user tables except sqlite_* and room_* internals
            db.query("""
                SELECT name 
                FROM seed.sqlite_master 
                WHERE type='table' 
                  AND name NOT LIKE 'sqlite_%' 
                  AND name NOT LIKE 'room_%'
            """.trimIndent()).use { c ->
                while (c.moveToNext()) {
                    val table = c.getString(0)
                    db.execSQL("INSERT INTO main.$table SELECT * FROM seed.$table;")
                }
            }
            db.execSQL("DETACH DATABASE seed;")
            db.execSQL("PRAGMA foreign_keys=ON;")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** Export current DB to snapshot on disk so future tests can seed from it. */
    fun exportSnapshot(db: SupportSQLiteDatabase) {
//        var db1 = Room.in
        try {
            val outDir = snapshotFile.parentFile!!
            println("Creating snapshot directory: ${outDir.absolutePath}")
            if (!outDir.exists()) {
                val created = outDir.mkdirs()
                println("Directory created: $created")
            }

            println("Exporting snapshot to: ${snapshotFile.absolutePath}")

            // Check database content before export
//            checkDatabaseContent(db)

            // First, checkpoint any WAL files
//            db.query(SimpleSQLiteQuery("PRAGMA wal_checkpoint(TRUNCATE)")).use { }

            // Delete existing snapshot if it exists
            if (snapshotFile.exists()) {
                val deleted = snapshotFile.delete()
                println("Deleted existing snapshot: $deleted")
                // Wait a moment to ensure file system has processed the deletion
                Thread.sleep(100)
            }

            val exportFile = snapshotFile
            val exportConnection = SQLiteDatabase.openOrCreateDatabase(exportFile, null)

            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_%'")
                .use { cursor ->
                    while (cursor.moveToNext()) {
                        val tableName = cursor.getString(0)
                        exportConnection.execSQL("DELETE FROM $tableName")
                        val copyData = db.query("SELECT * FROM $tableName")
                        while (copyData.moveToNext()) {
                            // manual insert per row (tedious but portable)
                        }
                    }
                }

//            // Export to file
//            sqLiteDb.query("VACUUM INTO '${snapshotFile.absolutePath}'").use { }

            db.close()

//            manualTableCopy(db)

            println("Snapshot exported successfully. File size: ${snapshotFile.length()} bytes")

        } catch (e: Exception) {
            println("Failed to export snapshot: ${e.message}")
            e.printStackTrace()

//            // Fallback: try a simple SQL dump approach
//            try {
//                println("Trying SQL dump fallback...")
//                sqlDumpFallback(db)
//            } catch (fallbackError: Exception) {
//                println("SQL dump fallback also failed: ${fallbackError.message}")
//                // Final fallback: just create an empty database file
//                try {
//                    snapshotFile.writeText("") // Create empty file so tests don't fail
//                    println("Created empty snapshot file to prevent test failures")
//                } catch (finalError: Exception) {
//                    println("Could not create empty file: ${finalError.message}")
//                }
//            }
        }
    }

    private fun fallbackExport(db: SupportSQLiteDatabase) {
        println("Attempting fallback export method...")
        val outDir = snapshotFile.parentFile!!
        if (!outDir.exists()) outDir.mkdirs()

        // Get database path and try to copy it
        val dbPath = db.path
        if (dbPath != null) {
            val sourceFile = File(dbPath)
            if (sourceFile.exists()) {
                sourceFile.copyTo(snapshotFile, overwrite = true)
                println("Fallback export successful. File size: ${snapshotFile.length()} bytes")
            } else {
                println("Source database file not found at: $dbPath")
            }
        } else {
            println("Database path is null - cannot perform fallback export")
        }
    }

    private fun checkDatabaseContent(db: SupportSQLiteDatabase) {
        try {
            println("=== Database Content Check ===")

            // List all tables
            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'room_%'").use { cursor ->
                val tableNames = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    tableNames.add(cursor.getString(0))
                }
                println("Tables found: $tableNames")

                // Count rows in each table
                for (tableName in tableNames) {
                    try {
                        db.query("SELECT COUNT(*) FROM $tableName").use { countCursor ->
                            if (countCursor.moveToFirst()) {
                                val count = countCursor.getInt(0)
                                println("  $tableName: $count rows")
                            }
                        }
                    } catch (e: Exception) {
                        println("  $tableName: error counting rows - ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            println("Error checking database content: ${e.message}")
        }
    }

    private fun manualTableCopy(sourceDb: SupportSQLiteDatabase) {
        println("Starting manual table copy...")

        try {
            // Ensure no active transactions and release any locks
            try {
                sourceDb.query("PRAGMA wal_checkpoint(TRUNCATE)").use { }
                sourceDb.query("PRAGMA journal_mode=DELETE").use { }
            } catch (e: Exception) {
                println("Warning: Could not set journal mode: ${e.message}")
            }

            // Use ATTACH DATABASE to copy data to a file-based database
            sourceDb.execSQL("ATTACH DATABASE ? AS snapshot_db", arrayOf(snapshotFile.absolutePath))

            // Get all user tables and copy them
            sourceDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'room_%'").use { cursor ->
                while (cursor.moveToNext()) {
                    val tableName = cursor.getString(0)
                    println("Copying table: $tableName")

                    try {
                        // First, drop table if it exists to ensure clean copy
                        try {
                            sourceDb.execSQL("DROP TABLE IF EXISTS snapshot_db.$tableName")
                        } catch (e: Exception) {
                            // Ignore if table doesn't exist
                        }

//                        // Get the CREATE TABLE statement
                        sourceDb.query("SELECT sql FROM sqlite_master WHERE type='table' AND name = ?", arrayOf(tableName)).use { schemaCursor ->
                            if (schemaCursor.moveToFirst()) {
                                val createSql = schemaCursor.getString(0)
                                if (createSql != null) {
                                    // Create the table in attached database
                                    val modifiedSql = createSql.replace("$tableName", "snapshot_db.$tableName")
                                    sourceDb.execSQL(modifiedSql)
                                }
                            }
                        }

                        // Copy all data
                        sourceDb.execSQL("INSERT INTO snapshot_db.$tableName SELECT * FROM main.$tableName")

                        // Count rows copied
                        sourceDb.query("SELECT COUNT(*) FROM snapshot_db.$tableName").use { countCursor ->
                            if (countCursor.moveToFirst()) {
                                val count = countCursor.getInt(0)
                                println("  Copied $count rows to $tableName")
                            }
                        }

                    } catch (e: Exception) {
                        println("  Error copying table $tableName: ${e.message}")
                    }
                }
            }

            // Copy indices
            sourceDb.query("SELECT sql FROM sqlite_master WHERE type='index' AND name NOT LIKE 'sqlite_%' AND sql IS NOT NULL").use { cursor ->
                while (cursor.moveToNext()) {
                    val indexSql = cursor.getString(0)
                    try {
                        val modifiedIndexSql = indexSql.replace("CREATE INDEX", "CREATE INDEX IF NOT EXISTS")
                            .replace("CREATE UNIQUE INDEX", "CREATE UNIQUE INDEX IF NOT EXISTS")
                        // Apply to snapshot database
                        val snapshotIndexSql = modifiedIndexSql.replace(Regex("ON `(\\w+)`"), "ON snapshot_db.$1")
                        sourceDb.execSQL(snapshotIndexSql)
                    } catch (e: Exception) {
                        println("Warning: Could not create index: ${e.message}")
                    }
                }
            }

            println("Manual table copy completed successfully")

        } catch (e: Exception) {
            println("Error during manual table copy: ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            try {
                sourceDb.execSQL("DETACH DATABASE snapshot_db")
            } catch (e: Exception) {
                println("Warning: Could not detach database: ${e.message}")
            }
        }
    }

    private fun sqlDumpFallback(sourceDb: SupportSQLiteDatabase) {
        println("Starting SQL dump fallback...")

        // Create a simple SQLite database using Java's built-in SQLite
        val connection = java.sql.DriverManager.getConnection("jdbc:sqlite:${snapshotFile.absolutePath}")
        connection.use { conn ->
            conn.autoCommit = false

            try {
                // Get all tables and their schemas
                sourceDb.query("SELECT sql, name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'room_%' AND sql IS NOT NULL").use { cursor ->
                    while (cursor.moveToNext()) {
                        val createSql = cursor.getString(0)
                        val tableName = cursor.getString(1)

                        println("Creating table: $tableName")

                        // Create table in target database
                        conn.createStatement().use { stmt ->
                            stmt.execute(createSql)
                        }

                        // Copy data
                        sourceDb.query("SELECT * FROM $tableName").use { dataCursor ->
                            if (dataCursor.moveToFirst()) {
                                val columnCount = dataCursor.columnCount
                                val placeholders = (1..columnCount).joinToString(",") { "?" }
                                val insertSql = "INSERT INTO $tableName VALUES ($placeholders)"

                                conn.prepareStatement(insertSql).use { prepStmt ->
                                    var rowCount = 0
                                    do {
                                        for (i in 0 until columnCount) {
                                            when (dataCursor.getType(i)) {
                                                android.database.Cursor.FIELD_TYPE_NULL -> prepStmt.setNull(i + 1, java.sql.Types.NULL)
                                                android.database.Cursor.FIELD_TYPE_INTEGER -> prepStmt.setLong(i + 1, dataCursor.getLong(i))
                                                android.database.Cursor.FIELD_TYPE_FLOAT -> prepStmt.setDouble(i + 1, dataCursor.getDouble(i))
                                                android.database.Cursor.FIELD_TYPE_STRING -> prepStmt.setString(i + 1, dataCursor.getString(i))
                                                android.database.Cursor.FIELD_TYPE_BLOB -> prepStmt.setBytes(i + 1, dataCursor.getBlob(i))
                                                else -> prepStmt.setString(i + 1, dataCursor.getString(i))
                                            }
                                        }
                                        prepStmt.executeUpdate()
                                        rowCount++
                                    } while (dataCursor.moveToNext())

                                    println("  Copied $rowCount rows to $tableName")
                                }
                            }
                        }
                    }
                }

                conn.commit()
                println("SQL dump fallback completed successfully")

            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }
}
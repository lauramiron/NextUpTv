package io.github.lauramiron.nextuptv.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Database migrations for AppDb.
 */

/**
 * Migration from version 5 to 6:
 * - Add 'link' TEXT column to external_ids table
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add link column to external_ids table (non-destructive)
        database.execSQL("ALTER TABLE external_ids ADD COLUMN link TEXT")
    }
}

/**
 * Migration from version 6 to 7:
 * - Add 'updatedAt' TEXT column to external_ids table in ISO 8601 format (UTC)
 * - Sets current timestamp in ISO format as default for existing rows
 * Note: MIGRATION_7_8 will update this to US/Pacific timezone
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add updatedAt column to external_ids table with current time in ISO 8601 format (UTC)
        // This makes timestamps human-readable in database browsers
        val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val currentIsoTimestamp = isoFormatter.format(Date())
        database.execSQL("ALTER TABLE external_ids ADD COLUMN updatedAt TEXT NOT NULL DEFAULT '$currentIsoTimestamp'")
    }
}

/**
 * Migration from version 7 to 8:
 * - Update external_ids.updatedAt to use US/Pacific timezone instead of UTC
 * - Add library_sync_metadata.updatedAt column and migrate data from timestamp column
 * - Rename library_sync_metadata.servicesJson to services
 *
 * Strategy:
 * 1. external_ids: Clear updatedAt values (will be repopulated on next sync)
 * 2. library_sync_metadata: Add updatedAt column, convert existing timestamp data, leave old column
 * 3. library_sync_metadata: Rename servicesJson to services
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val pacificFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("America/Los_Angeles")
        }
        val currentPacificTimestamp = pacificFormatter.format(Date())

        // 1. Update external_ids: Clear existing UTC timestamps (they'll be regenerated on next sync)
        database.execSQL("UPDATE external_ids SET updatedAt = '$currentPacificTimestamp'")

        // 2. Rename servicesJson column to services first
        database.execSQL("ALTER TABLE library_sync_metadata RENAME COLUMN servicesJson TO services")

        // 3. Add updatedAt column to library_sync_metadata with NOT NULL constraint
        // First add as nullable to allow setting values
        database.execSQL("ALTER TABLE library_sync_metadata ADD COLUMN updatedAt TEXT")

        // 4. Convert existing timestamp (INTEGER milliseconds) to ISO format in Pacific timezone
        database.query("SELECT id, timestamp FROM library_sync_metadata").use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getInt(0)
                val timestampMillis = cursor.getLong(1)
                val isoTimestamp = pacificFormatter.format(Date(timestampMillis))
                database.execSQL("UPDATE library_sync_metadata SET updatedAt = ? WHERE id = ?",
                    arrayOf(isoTimestamp, id))
            }
        }

        // 5. SQLite doesn't support adding NOT NULL constraint after the fact or dropping columns easily,
        // so we recreate the table without the timestamp column
        database.execSQL("""
            CREATE TABLE library_sync_metadata_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                updatedAt TEXT NOT NULL,
                syncType TEXT NOT NULL,
                success INTEGER NOT NULL,
                services TEXT NOT NULL,
                metadataJson TEXT,
                nextCursor TEXT
            )
        """)

        // 6. Copy data from old table to new table
        database.execSQL("""
            INSERT INTO library_sync_metadata_new (id, updatedAt, syncType, success, services, metadataJson, nextCursor)
            SELECT id, updatedAt, syncType, success, services, metadataJson, nextCursor
            FROM library_sync_metadata
        """)

        // 7. Drop old table and rename new table
        database.execSQL("DROP TABLE library_sync_metadata")
        database.execSQL("ALTER TABLE library_sync_metadata_new RENAME TO library_sync_metadata")
    }
}

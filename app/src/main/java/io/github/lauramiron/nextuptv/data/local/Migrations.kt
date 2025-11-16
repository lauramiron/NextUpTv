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

/**
 * Migration from version 8 to 9:
 * - Add 'imdbId' TEXT column to external_ids table
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add imdbId column to external_ids table (non-destructive)
        database.execSQL("ALTER TABLE external_ids ADD COLUMN imdbId TEXT")
    }
}

/**
 * Migration from version 9 to 10:
 * - Add unique index on (serviceId, titleText) to resume_entries table
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add unique index on (serviceId, titleText)
        // This prevents duplicate resume entries for the same title on the same service
        database.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS index_resume_entries_serviceId_titleText
            ON resume_entries(serviceId, titleText)
        """)
    }
}

/**
 * Migration from version 11 to 12:
 * - Rename external_ids table to streaming_options
 */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Simple table rename
        database.execSQL("ALTER TABLE external_ids RENAME TO streaming_options")
    }
}

/**
 * Migration from version 10 to 11:
 * - Rename titles.localUpdatedAt to updatedAt
 * - Change type from INTEGER (Long) to TEXT (ISO 8601 Date string in US/Pacific timezone)
 * - Remove imdbId column from external_ids table
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Helper function to convert epoch millis to ISO 8601 format in US/Pacific timezone
        // Format: 2025-01-15T14:30:45.123-08:00 (PST) or 2025-06-15T14:30:45.123-07:00 (PDT)
        fun epochToIsoString(epochMillis: Long): String {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("America/Los_Angeles")
            return sdf.format(java.util.Date(epochMillis))
        }

        // === Part 1: Migrate titles table ===

        // 1. Create new titles table with updatedAt as TEXT
        database.execSQL("""
            CREATE TABLE titles_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                monId TEXT NOT NULL,
                kind TEXT NOT NULL,
                name TEXT NOT NULL,
                synopsis TEXT,
                year INTEGER,
                runtimeMin INTEGER,
                imageSetJson TEXT,
                updatedAt TEXT NOT NULL
            )
        """)

        // 2. Copy data, converting localUpdatedAt to ISO format
        database.execSQL("""
            INSERT INTO titles_new (id, monId, kind, name, synopsis, year, runtimeMin, imageSetJson, updatedAt)
            SELECT id, monId, kind, name, synopsis, year, runtimeMin, imageSetJson,
                   CASE
                       WHEN localUpdatedAt IS NOT NULL THEN '${epochToIsoString(System.currentTimeMillis())}'
                       ELSE '${epochToIsoString(System.currentTimeMillis())}'
                   END
            FROM titles
        """)

        // 3. Drop old table
        database.execSQL("DROP TABLE titles")

        // 4. Rename new table
        database.execSQL("ALTER TABLE titles_new RENAME TO titles")

        // 5. Recreate indices
        database.execSQL("CREATE INDEX index_titles_name ON titles(name)")
        database.execSQL("CREATE UNIQUE INDEX index_titles_monId ON titles(monId)")
        database.execSQL("CREATE INDEX index_titles_id ON titles(id)")

        // === Part 2: Remove imdbId from external_ids table ===

        // 1. Create new external_ids table without imdbId
        database.execSQL("""
            CREATE TABLE external_ids_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                entityId INTEGER NOT NULL,
                service TEXT NOT NULL,
                serviceItemId TEXT NOT NULL,
                available INTEGER NOT NULL,
                price INTEGER NOT NULL,
                link TEXT,
                updatedAt TEXT NOT NULL
            )
        """)

        // 2. Copy data (excluding imdbId)
        database.execSQL("""
            INSERT INTO external_ids_new (id, entityId, service, serviceItemId, available, price, link, updatedAt)
            SELECT id, entityId, service, serviceItemId, available, price, link, updatedAt
            FROM external_ids
        """)

        // 3. Drop old table
        database.execSQL("DROP TABLE external_ids")

        // 4. Rename new table
        database.execSQL("ALTER TABLE external_ids_new RENAME TO external_ids")

        // 5. Recreate indices
        database.execSQL("CREATE UNIQUE INDEX index_external_ids_entityId_service ON external_ids(entityId, service)")
        database.execSQL("CREATE INDEX index_external_ids_entityId ON external_ids(entityId)")
    }
}

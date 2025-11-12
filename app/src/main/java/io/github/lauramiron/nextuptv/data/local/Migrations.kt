package io.github.lauramiron.nextuptv.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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

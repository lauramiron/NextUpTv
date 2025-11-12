package io.github.lauramiron.nextuptv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Type of library sync operation
 */
enum class SyncType {
    FULL,     // Full sync of entire catalog
    CHANGES   // Incremental sync of changes since last sync (not yet implemented)
}

/**
 * Metadata about library sync operations.
 * Tracks history of syncs including success/failure and detailed reports.
 */
@Entity(tableName = "library_sync_metadata")
data class LibrarySyncMetadataEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Date,
    val syncType: SyncType,
    val success: Boolean,
    val servicesJson: String,   // JSON array of service names, e.g., ["netflix", "prime", "disney"]
    val metadataJson: String?,  // JSON blob containing SyncReport data
    val nextCursor: String? = null  // Next cursor if sync was interrupted (for resuming)
)

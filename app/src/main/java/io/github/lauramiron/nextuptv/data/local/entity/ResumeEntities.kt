package io.github.lauramiron.nextuptv.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "resume_entries",
    indices = [
        Index(value = ["serviceId", "serviceItemId"]),
        Index(value = ["lastWatchedAt"]),
        Index(value = ["resolvedTitleId"]),
        Index(value = ["hashKey"], unique = true)
    ]
)
data class ResumeEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    // Provider info
    val serviceId: StreamingService,    // streaming service provider
    val serviceItemId: String?,          // e.g., Netflix numeric id if you can scrape it (optional)

    // Scraped human-readable fields
    val titleText: String,               // show/movie title as scraped
    val seasonNumber: Int?,              // optional
    val episodeNumber: Int?,             // optional

//    // Progress & timing
    val resumeIndex: Int?,
//    val progressMinutes: Int?,           // optional
//    val progressSeconds: Int?,           // optional
//    val lastWatchedAt: Long,             // epoch millis

    // Resolution to library
    val resolvedTitleId: Long?,          // FK -> titles(id), nullable
    val resolvedEpisodeId: Long?,        // FK -> episodes(id), nullable

//    // Book-keeping
//    val createdAt: Long = System.currentTimeMillis(),
//    val updatedAt: Long = System.currentTimeMillis(),

//    // De-dupe key per provider entry content (hash of the meaningful fields)
//    val hashKey: String                  // unique across the table
)

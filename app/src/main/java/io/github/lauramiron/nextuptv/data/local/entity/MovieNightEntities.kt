package io.github.lauramiron.nextuptv.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.lauramiron.nextuptv.util.StreamingService
import java.util.Date

// --- enums as @IntDef/@StringDef or sealed classes; using String here for brevity ---
enum class TitleKind { MOVIE, SERIES }
enum class ArtworkType { POSTER, BACKDROP, LOGO }
enum class CreditRole { CAST, DIRECTOR, WRITER }
enum class PopularityType { TOP_SHOWS }

// ---- TITLES (movie or series root) ----
@Entity(
    tableName = "titles",
    indices = [Index("name"), Index(value = ["monId"], unique = true), Index("id")]
)
data class TitleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monId: String,                // MovieOfTheNight canonical ID for the title
    val kind: TitleKind,
    val name: String,
    val synopsis: String?,
    val year: Int?,
    val runtimeMin: Int?,
//    val language: String?,
//    val maturityRating: String?,
    val imageSetJson: String?,          // JSON string of the imageSet object
    val localUpdatedAt: Long = System.currentTimeMillis()
)


// ---- EPISODES ----
@Entity(
    tableName = "episodes",
    indices = [Index(value = ["titleId", "seasonNumber", "episodeNumber"], unique = true)],
    foreignKeys = [
        ForeignKey(entity = TitleEntity::class,
            parentColumns = ["id"], childColumns = ["titleId"],
            onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE)
    ]
)
data class EpisodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titleId: Long,                 // series root (or same as movie title for 1:1)
    val monId: String,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val name: String?,
    val synopsis: String?,
    val runtimeMin: Int?,
    val airDate: String?,              // keep as ISO string or epoch
    val imageSetJson: String?,     // NEW: entire episode-level imageSet blob
    val sourceUpdatedAt: Long?,
    val localUpdatedAt: Long = System.currentTimeMillis()
)

//// ---- ARTWORK (posters, backdrops, logos) ----
//@Entity(
//    tableName = "artwork",
//    indices = [Index("titleId"), Index("episodeId")]
//)
//data class ArtworkEntity(
//    @PrimaryKey(autoGenerate = true) val id: Long = 0,
//    val titleId: Long?,                // for title-level art
//    val episodeId: Long?,              // or episode-level art
//    val type: ArtworkType,
//    val url: String,
//    val width: Int?,
//    val height: Int?
//)

// ---- GENRES ----
@Entity(tableName = "genres", indices = [Index(value = ["name"], unique = true)])
data class GenreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "title_genres",
    indices = [Index("genreId"), Index("titleId")],
    foreignKeys = [
        ForeignKey(entity = TitleEntity::class, parentColumns = ["id"], childColumns = ["titleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = GenreEntity::class, parentColumns = ["id"], childColumns = ["genreId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class TitleGenreCrossRef(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titleId: Long,
    val genreId: Long
)

// ---- PEOPLE / CREDITS ----
@Entity(tableName = "people", indices = [Index(value = ["name"], unique = true)])
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "credits",
    indices = [Index("titleId"), Index("personId")],
    foreignKeys = [
        ForeignKey(entity = TitleEntity::class, parentColumns = ["id"], childColumns = ["titleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PersonEntity::class, parentColumns = ["id"], childColumns = ["personId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class TitlePersonCrossRef(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titleId: Long?,                // for movie/series-level credit
    val personId: Long,
    val role: CreditRole,
)

// ---- EXTERNAL IDS (lookup by Netflix/etc or by MovieOfTheNight itself) ----
@Entity(
    tableName = "external_ids",
    indices = [Index(value = ["entityId", "service"], unique = true), Index("entityId")])
data class ExternalIdEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
//    val entityType: String,            // "title" | "episode"
    val entityId: Long,                // FK to titles.id or episodes.id (enforce in code)
    val service: StreamingService,     // streaming service
    val serviceItemId: String,         // service-specific ID for this item
    val available: Boolean,
    val price: Short,
    val link: String? = null,          // Original URL from API (showLink or videoLink)
    @ColumnInfo(typeAffinity = ColumnInfo.TEXT)
    val updatedAt: Date = Date(),      // ISO 8601 timestamp in US/Pacific timezone stored as TEXT
    val imdbId: String? = null         // IMDb ID for cross-referencing (e.g., "tt1234567")
)

@Entity(
    tableName = "title_popularities",
    indices =[Index("titleId")],
    foreignKeys = [
        ForeignKey(
            entity = TitleEntity::class,
            parentColumns = ["id"], childColumns = ["titleId"],
            onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE
        ),
    ]
)
data class PopularityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val service: StreamingService,   // streaming service provider
    val popularityType: PopularityType,
    val titleId: Long,                // FK to titles.id or episodes.id (enforce in code)
    val updatedAt: Date
)

/**
 * Result class for queries that join titles with external IDs.
 * Used to construct launch URLs for streaming services.
 */
data class TitleWithExternalId(
    @Embedded val title: TitleEntity,
    val externalId: String?,  // serviceItemId from external_ids table, null if no external ID exists
    val link: String?         // Original link from external_ids table (preferred for launch URLs)
)

package io.github.lauramiron.nextuptv.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

// --- enums as @IntDef/@StringDef or sealed classes; using String here for brevity ---
enum class TitleKind { MOVIE, SERIES }
enum class ArtworkType { POSTER, BACKDROP, LOGO }
enum class CreditRole { CAST, DIRECTOR, WRITER }
enum class StreamingService(val id: String) {
    NETFLIX("netflix"),
    PRIME("prime"),
    DISNEY("disney"),
    APPLE("apple"),
    HBO("hbo"),
    PEACOCK("peacock"),
    HULU("hulu");

    /**
     * Build the launch URL for this streaming service.
     *
     * @param externalId The provider-specific ID from ExternalIdEntity.providerId
     * @return The deep link URL for launching the title in the streaming service app.
     *         For Netflix, includes {userId} placeholder to be substituted by calling code.
     */
    fun buildLaunchUrl(externalId: String): String {
        return when (this) {
            NETFLIX -> {
                // {userId} is a placeholder for user-specific Netflix ID, to be substituted by calling code
                "https://www.netflix.com/watch/{userId}?trackId=$externalId"
            }
            PRIME -> {
                // TODO: Verify Prime Video URL format and test deep linking
                "https://www.primevideo.com/detail/$externalId"
            }
            DISNEY -> {
                // TODO: Verify Disney+ URL format and test deep linking
                "https://www.disneyplus.com/video/$externalId"
            }
            APPLE -> {
                // TODO: Verify Apple TV+ URL format and test deep linking
                // May need to differentiate between movies and series
                "https://tv.apple.com/us/movie/$externalId"
            }
            HBO -> {
                // TODO: Verify HBO Max URL format and test deep linking
                "https://play.hbomax.com/page/$externalId"
            }
            PEACOCK -> {
                // TODO: Verify Peacock URL format and test deep linking
                "https://www.peacocktv.com/watch/playback/$externalId"
            }
            HULU -> {
                // TODO: Verify Hulu URL format and test deep linking
                "https://www.hulu.com/watch/$externalId"
            }
        }
    }

    companion object {
        fun fromString(id: String): StreamingService? {
            return entries.find { it.id.equals(id, ignoreCase = true) }
        }
    }
}
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
    indices = [Index(value = ["entityId", "provider"], unique = true), Index("entityId")])
data class ExternalIdEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
//    val entityType: String,            // "title" | "episode"
    val entityId: Long,                // FK to titles.id or episodes.id (enforce in code)
    val provider: StreamingService,   // streaming service provider
    val providerId: String,
    val available: Boolean,
    val price: Short
//    val showLink: String,
//    val videoLink: String
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
    val externalId: String?  // providerId from external_ids table, null if no external ID exists
)

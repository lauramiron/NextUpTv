package io.github.lauramiron.nextuptv.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// --- enums as @IntDef/@StringDef or sealed classes; using String here for brevity ---
enum class TitleKind { MOVIE, SERIES }
enum class ArtworkType { POSTER, BACKDROP, LOGO }
enum class CreditRole { CAST, DIRECTOR, WRITER }

// ---- TITLES (movie or series root) ----
@Entity(
    tableName = "titles",
    indices = [Index("name"), Index(value = ["monId"], unique = true)]
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

//// ---- SEASONS (only for SERIES) ----
//@Entity(
//    tableName = "seasons",
//    indices = [Index(value = ["titleId", "seasonNumber"], unique = true)],
//    foreignKeys = [
//        ForeignKey(
//            entity = TitleEntity::class,
//            parentColumns = ["id"], childColumns = ["titleId"],
//            onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE
//        )
//    ]
//)
//data class SeasonEntity(
//    @PrimaryKey(autoGenerate = true) val id: Long = 0,
//    val titleId: Long,                 // FK -> titles.id
//    val seasonNumber: Int,
//    val name: String?,
//    val synopsis: String?,
//    val posterUrl: String?
//)

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
    val provider: String,              // e.g. "mon" | "netflix" | "tmdb" | "imdb"
    val providerId: String,
    val available: Boolean,
    val price: Short
//    val showLink: String,
//    val videoLink: String
)

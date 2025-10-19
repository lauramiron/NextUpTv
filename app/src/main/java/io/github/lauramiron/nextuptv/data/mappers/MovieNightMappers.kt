package io.github.lauramiron.nextuptv.data.mappers

import io.github.lauramiron.nextuptv.data.local.entity.CreditRole
import io.github.lauramiron.nextuptv.data.local.entity.ExternalIdEntity
import io.github.lauramiron.nextuptv.data.local.entity.PersonEntity
import io.github.lauramiron.nextuptv.data.local.entity.TitleEntity
import io.github.lauramiron.nextuptv.data.local.entity.TitleKind
import io.github.lauramiron.nextuptv.data.local.entity.TitlePersonCrossRef
import io.github.lauramiron.nextuptv.data.remote.movienight.StreamingOptionDto
import io.github.lauramiron.nextuptv.data.remote.movienight.TitleDto

fun TitleDto.toEntity(): TitleEntity {
    val kind = when (showType?.lowercase()) {
        "series", "tv", "show" -> TitleKind.SERIES
        "movie", null, ""      -> TitleKind.MOVIE
        else                   -> TitleKind.MOVIE
    }
    return TitleEntity(
        id=0L,
        monId = this.id,
        kind = kind,
        name = this.title,
        synopsis = this.overview,
        year = this.releaseYear,
        runtimeMin = this.runtime,
        imageSetJson = this.imageSet?.toString());
}

//fun EpisodeDto.toEntity(titleId: Long): EpisodeEntity {
//    // TODO
//    return EpisodeEntity(id=0, mon_id = this.id, titleId = titleId, seasonNumber = this.seasonNumber, episodeNumber = null, synopsis = null, name = null, runtimeMin = null, airDate = null, stillUrl = null, sourceUpdatedAt = null);
//}

//fun TitleDto.toExternalIds(titleId: Long): List<ExternalIdEntity> {
//    val out = mutableListOf<ExternalIdEntity>()
//
//    // Optionally capture a Netflix title id from US streamingOptions, if present
//    extractUsNetflixTitleId()?.let { nflx ->
//        out += ExternalIdEntity(id = 0,titleId, "netflix", nflx, available = true, price = 0)
//    }
//
//    return out
//}

fun TitleDto.extractUsStreamingOptions(): List<StreamingOptionDto> {
    return streamingOptions?.get("us") ?: emptyList()
}

private fun parseNetflixId(url: String?): String? =
    url?.let { u -> Regex("""/(title|watch)/(\d+)""").find(u)?.groupValues?.getOrNull(2) }

// Pulls “/title/########/” from a US netflix link, if present
private fun TitleDto.extractUsNetflixTitleId(): String? {
    val country = "us"
    val list = this.streamingOptions?.get(country) ?: return null
    val firstNetflix = list.firstOrNull { it.service.id.equals("netflix", ignoreCase = true) }
        ?: return null
    val link = firstNetflix.link ?: firstNetflix.videoLink ?: return null
    return parseNetflixId(link)
}

// --- Genres ---

fun TitleDto.toGenreNames(): List<String> =
    this.genres
        .mapNotNull { g ->
            val name = g.name.trim()
            name.takeIf { it.isNotEmpty() }
        }
        .distinctBy { it.lowercase() }

// --- People ---

fun TitleDto.toCast(): List<PersonEntity> {
    val names = mutableSetOf<String>()
    this.cast.forEach { if (!it.isNullOrBlank()) names += it.trim() }
    return names.map { PersonEntity(id = 0, name = it) }
}

fun TitleDto.toDirectors(): List<PersonEntity> {
    val names = mutableSetOf<String>()
    this.directors.forEach { if (!it.isNullOrBlank()) names += it.trim() }
    return names.map { PersonEntity(id = 0, name = it) }
}

/**
 * Build Title↔Person cross-refs using a name→id map returned from PersonDao.upsertAll.
 *
 * Suggested call site in repository:
 *   val people = dto.toPeople()
 *   val personIds = personDao.upsertAll(people)
 *   val nameToId = people.mapIndexed { i, p -> p.name to personIds[i] }.toMap()
 *   val refs = dto.toTitlePersonRefs(titleId, nameToId)
 */
fun TitleDto.toTitlePersonRefs(
    titleId: Long,
    nameToId: Map<String, Long>
): List<TitlePersonCrossRef> {
    val out = mutableListOf<TitlePersonCrossRef>()
    this.directors.forEach { n ->
        val id = n.trim().let { nameToId[it] } ?: return@forEach
        out += TitlePersonCrossRef(titleId = titleId, personId = id, role = CreditRole.DIRECTOR)
    }
    this.cast.forEach { n ->
        val id = n.trim().let { nameToId[it] } ?: return@forEach
        out += TitlePersonCrossRef(titleId = titleId, personId = id, role = CreditRole.CAST)
    }
    return out
}

// --- Episodes ---

//fun TitleDto.toEpisodes(titleId: Long): List<EpisodeEntity> {
//    // If your DTO for “showType == series” contains seasons/episodes, map them here.
//    // The sample payload for "The Godfather" is a MOVIE, so episodes = empty.
//    if (!this.isSeries()) return emptyList()
//
//    // Example if your DTO had: seasons: List<SeasonDto> with nested episodes:
//    val episodes = mutableListOf<EpisodeEntity>()
//    this.seasons?.forEach { season ->
//        season.episodes?.forEach { ep ->
//            episodes += EpisodeEntity(
//                id = 0,
//                titleId = titleId,
//                seasonNumber = season.seasonNumber,
//                episodeNumber = ep.episodeNumber,
//                name = ep.name,
//                synopsis = ep.overview,
//                runtimeMin = ep.runtime,
//                airDate = ep.airDate,    // adapt to your type
//                stillUrl = ep.imageSetJson, // or parse to a specific url if you prefer
//                sourceUpdatedAt = System.currentTimeMillis()
//            )
//        }
//    }
//    return episodes
//}

private fun TitleDto.isSeries(): Boolean =
    this.showType.equals("series", true)

fun StreamingOptionDto.toExternalIdEntity(titleId: Long): ExternalIdEntity {
    val provider = service.id.lowercase()
    val providerId = when (provider) {
        "netflix" -> parseNetflixId(link ?: videoLink) ?: "unknown"
        else -> "unknown" // add cases for disney/hbo/prime/etc later
    }
    return ExternalIdEntity(
        provider = provider,
        providerId = providerId,
        entityId = titleId,
        available = true,
        price = 0
//        price = price.toShortAmountOrNull()
    )
}
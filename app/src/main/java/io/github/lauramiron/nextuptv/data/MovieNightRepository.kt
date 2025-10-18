package io.github.lauramiron.nextuptv.data


import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

import androidx.room.withTransaction
import io.github.lauramiron.nextuptv.data.local.AppDb
import io.github.lauramiron.nextuptv.data.local.entity.ExternalIdEntity
import io.github.lauramiron.nextuptv.data.mappers.toEntity
import io.github.lauramiron.nextuptv.data.remote.movienight.EpisodeDto
import io.github.lauramiron.nextuptv.data.remote.movienight.TitleDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Retrofit service (shape the DTOs to match MovieOfTheNight)
interface MovieNightApi {
    @GET("v1/titles/{id}")
    suspend fun getTitle(@Path("id") monId: String): TitleDto

    @GET("v1/titles/{id}/episodes")
    suspend fun getEpisodes(@Path("id") monId: String): List<EpisodeDto>

    @GET("v1/search")
    suspend fun search(@Query("q") query: String): List<TitleDto>

    // etc. add endpoints you need
}

class MovieNightRepository(
    private val api: MovieNightApi,
    private val db: AppDb,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {

    private val netflixRegex = Regex("""/(title|watch)/(\d+)""")
    private fun parseNetflixId(url: String?): String? =
        url?.let { u -> netflixRegex.find(u)?.groupValues?.getOrNull(2) }

    suspend fun syncTitle(monId: String) = withContext(io) {
        val titleDto = api.getTitle(monId)
        val episodes = if (titleDto.showType == "SERIES") api.getEpisodes(monId) else emptyList()

        db.withTransaction {
            val titleEntity = titleDto.toEntity()          // mapping extension
            val titleId = db.titleDao().upsertTitles(listOf(titleEntity)).first()

            // genres, artwork, credits -> map & insert
            val ext = mutableListOf<ExternalIdEntity>()

            titleDto.streamingOptions.orEmpty().forEach { (countryCode, options) ->
                options
                    .asSequence()
                    .filter { countryCode.equals("us") }
                    .forEach { so ->
                        parseNetflixId(so.videoLink)?.let { id ->
                            ext += ExternalIdEntity(
                                entityType = "title",
                                entityId = titleId,
                                provider = "netflix",
                                providerId = id,
                            )
                        }
                    }
            }

            db.externalIdDao().insertAll(ext)

            if (episodes.isNotEmpty()) {
                val eps = episodes.map { it.toEntity(titleId) }
                db.episodeDao().upsertEpisodes(eps)
//                db.externalIdDao().insertAll(
//                    episodes.mapNotNull { e ->
//                        e.externalId?.let {
//                            ExternalIdEntity(
//                                "episode", /*entityId to resolve later*/,
//                                "mon",
//                                it
//                            )
//                        }
//                    }
//                )
                // tip: after upsert, query back to resolve entityIds for external_ids if needed
            }
        }
    }
}

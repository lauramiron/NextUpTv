package io.github.lauramiron.nextuptv.data.MovieNightRepository.mappers

import io.github.lauramiron.nextuptv.data.MovieNightRepository.local.EpisodeEntity
import io.github.lauramiron.nextuptv.data.MovieNightRepository.local.TitleEntity
import io.github.lauramiron.nextuptv.data.MovieNightRepository.local.TitleKind
import io.github.lauramiron.nextuptv.data.MovieNightRepository.remote.EpisodeDto
import io.github.lauramiron.nextuptv.data.MovieNightRepository.remote.TitleDto

fun TitleDto.toEntity(): TitleEntity {
    // TODO
    return TitleEntity(0, this.id, TitleKind.MOVIE, this.title, this.overview, this.releaseYear, null, null, null, null, null, null, 0);
}

fun EpisodeDto.toEntity(titleId: Long): EpisodeEntity {
    // TODO
    return EpisodeEntity(id=0, mon_id = this.id, titleId = titleId, seasonNumber = this.seasonNumber, episodeNumber = null, synopsis = null, name = null, runtimeMin = null, airDate = null, stillUrl = null, sourceUpdatedAt = null);
}
package io.github.lauramiron.nextuptv.data.mappers

import io.github.lauramiron.nextuptv.data.local.entity.EpisodeEntity
import io.github.lauramiron.nextuptv.data.local.entity.TitleEntity
import io.github.lauramiron.nextuptv.data.local.entity.TitleKind
import io.github.lauramiron.nextuptv.data.remote.movienight.EpisodeDto
import io.github.lauramiron.nextuptv.data.remote.movienight.TitleDto

fun TitleDto.toEntity(): TitleEntity {
    // TODO
    return TitleEntity(0, this.id, TitleKind.MOVIE, this.title, this.overview, this.releaseYear, null, null, null, null, null, null, 0);
}

fun EpisodeDto.toEntity(titleId: Long): EpisodeEntity {
    // TODO
    return EpisodeEntity(id=0, mon_id = this.id, titleId = titleId, seasonNumber = this.seasonNumber, episodeNumber = null, synopsis = null, name = null, runtimeMin = null, airDate = null, stillUrl = null, sourceUpdatedAt = null);
}
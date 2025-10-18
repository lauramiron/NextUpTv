package io.github.lauramiron.nextuptv.data.remote.movienight

// data/remote/dto/ShowDto.kt
data class TitleDto(
//    val itemType: String?,        // "show"
    val showType: String?,        // "movie" | "series"
    val id: String,               // "82"
    val imdbId: String?,
    val tmdbId: String?,          // "movie/238"
    val title: String,
    val originalTitle: String?,
    val overview: String?,
    val releaseYear: Int?,
    val rating: Int?,             // 0..100
    val runtime: Int?,
    val genres: List<GenreDto> = emptyList(),
    val directors: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val imageSet: ImageSetDto? = null,
    val streamingOptions: Map<String, List<StreamingOptionDto>>? = null
)

data class GenreDto(
    val id: String,
    val name: String
)

// Keep this flexible; you can also strongly type every sub-object if you want.
data class ImageSetDto(
    val verticalPoster: Map<String, String>?,     // keys like "w240", "w360"
    val horizontalPoster: Map<String, String>?,
    val verticalBackdrop: Map<String, String>?,
    val horizontalBackdrop: Map<String, String>?
)

data class StreamingOptionDto(
    val service: StreamingServiceDto,
    val type: String,             // subscription/rent/buy/addon
    val link: String?,
    val videoLink: String?,
    val quality: String?,
    val audios: List<AudioDto> = emptyList(),
    val subtitles: List<SubtitleDto> = emptyList(),
    val expiresSoon: Boolean?,
    val expiresOn: Long?,
    val availableSince: Long?,
    val price: PriceDto? = null,
    val addon: AddonDto? = null
)

data class StreamingServiceDto(
    val id: String,               // "netflix"
    val name: String,             // "Netflix"
    val homePage: String?,
    val themeColorCode: String?,
    val imageSet: Map<String, String>? = null
)

data class EpisodeDto(
    val id: String,
    val mon_id: String,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val name: String?,
    val synopsis: String?,
    val runtimeMin: Int?,
    val airDate: String?,              // keep as ISO string or epoch
    val stillUrl: String?,
    val sourceUpdatedAt: Long?,
    val localUpdatedAt: Long = System.currentTimeMillis()
)

data class AudioDto(val language: String?, val region: String? = null)
data class SubtitleDto(val closedCaptions: Boolean?, val locale: LocaleDto?)
data class LocaleDto(val language: String?, val region: String? = null)
data class PriceDto(val amount: String?, val currency: String?, val formatted: String?)
data class AddonDto(val id: String?, val name: String?, val homePage: String?, val themeColorCode: String?)

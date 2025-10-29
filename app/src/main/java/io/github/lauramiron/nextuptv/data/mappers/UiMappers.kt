package io.github.lauramiron.nextuptv.data.mappers

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.github.lauramiron.nextuptv.data.local.entity.StreamingService
import io.github.lauramiron.nextuptv.data.local.entity.TitleEntity
import io.github.lauramiron.nextuptv.data.local.entity.TitleWithExternalId
import io.github.lauramiron.nextuptv.ui.details.MovieItem

/**
 * Convert TitleEntity to MovieItem for UI display (without launch URL)
 */
fun TitleEntity.toMovieItem(): MovieItem {
    // Parse the imageSet JSON to extract card and background URLs
    val imageSet = parseImageSet(imageSetJson)

    return MovieItem(
        id = id,
        title = name,
        description = synopsis,
        backgroundImageUrl = imageSet?.get("horizontalBackdrop")?.get("w1440")
            ?: imageSet?.get("verticalBackdrop")?.get("w720"),
        cardImageUrl = imageSet?.get("horizontalPoster")?.get("w360")
            ?: imageSet?.get("verticalPoster")?.get("w240"),
        videoUrl = null,
        studio = null
    )
}

/**
 * Convert TitleWithExternalId to MovieItem for UI display with service-specific launch URL.
 *
 * @param service The streaming service to build the launch URL for
 * @return MovieItem with videoUrl populated using the service's buildLaunchUrl() function
 */
fun TitleWithExternalId.toMovieItem(service: StreamingService): MovieItem {
    // Parse the imageSet JSON to extract card and background URLs
    val imageSet = parseImageSet(title.imageSetJson)

    // Build the launch URL if we have an external ID
    val videoUrl = externalId?.let { service.buildLaunchUrl(it) }

    return MovieItem(
        id = title.id,
        title = title.name,
        description = title.synopsis,
        backgroundImageUrl = imageSet?.get("horizontalBackdrop")?.get("w1440")
            ?: imageSet?.get("verticalBackdrop")?.get("w720"),
        cardImageUrl = imageSet?.get("horizontalPoster")?.get("w360")
            ?: imageSet?.get("verticalPoster")?.get("w240"),
        videoUrl = videoUrl,
        studio = service.id.replaceFirstChar { it.uppercase() } // Use service name as studio
    )
}

/**
 * Parse imageSet JSON string to extract image URLs
 * Returns a map of image type -> size -> url
 */
private fun parseImageSet(jsonString: String?): Map<String, Map<String, String>>? {
    if (jsonString.isNullOrBlank()) return null

    return try {
        val moshi = Moshi.Builder().build()
        val type = Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
        )
        val adapter: JsonAdapter<Map<String, Map<String, String>>> = moshi.adapter(type)
        adapter.fromJson(jsonString)
    } catch (e: Exception) {
        null
    }
}

package io.github.lauramiron.nextuptv.data.mappers

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.github.lauramiron.nextuptv.data.local.dao.ResumeWithTitleRow
import io.github.lauramiron.nextuptv.util.StreamingService
import io.github.lauramiron.nextuptv.util.tvAppPackage
import io.github.lauramiron.nextuptv.data.local.entity.TitleEntity
import io.github.lauramiron.nextuptv.data.local.entity.TitleWithExternalId
import io.github.lauramiron.nextuptv.ui.details.TitleItem
import io.github.lauramiron.nextuptv.ui.resume.ResumeItem
import io.github.lauramiron.nextuptv.util.buildLaunchUrl

/**
 * Convert TitleEntity to TitleItem for UI display (without launch URL)
 */
fun TitleEntity.toTitleItem(): TitleItem {
    // Parse the imageSet JSON to extract card and background URLs
    val imageSet = parseImageSet(imageSetJson)

    return TitleItem(
        id = id,
        title = name,
        description = synopsis,
        backgroundImageUrl = imageSet?.get("horizontalBackdrop")?.get("w1440")
            ?: imageSet?.get("verticalBackdrop")?.get("w720"),
        cardImageUrl = imageSet?.get("horizontalPoster")?.get("w360")
            ?: imageSet?.get("verticalPoster")?.get("w240"),
        videoUrl = null,
        studio = null,
        service = null
    )
}

/**
 * Convert TitleWithExternalId to TitleItem for UI display with service-specific launch URL.
 *
 * Launch URL logic:
 * - For Prime Video: constructs URL from externalId if available, otherwise uses link
 * - For other services: uses link from API
 *
 * @param service The streaming service to build the launch URL for
 * @return TitleItem with videoUrl populated using the best available URL
 */
fun TitleWithExternalId.toTitleItem(service: StreamingService): TitleItem {
    // Parse the imageSet JSON to extract card and background URLs
    val imageSet = parseImageSet(title.imageSetJson)

    // Build the launch URL using service-specific logic
    val videoUrl = service.buildLaunchUrl(link, externalId)

    return TitleItem(
        id = title.id,
        title = title.name,
        description = title.synopsis,
        backgroundImageUrl = imageSet?.get("horizontalBackdrop")?.get("w1440")
            ?: imageSet?.get("verticalBackdrop")?.get("w720"),
        cardImageUrl = imageSet?.get("horizontalPoster")?.get("w360")
            ?: imageSet?.get("verticalPoster")?.get("w240"),
        videoUrl = videoUrl,
        studio = service.id.replaceFirstChar { it.uppercase() }, // Use service name as studio
        service = service
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

/**
 * Convert ResumeWithTitleRow to ResumeItem for UI display.
 *
 * Launch URL logic:
 * 1. If externalLink is not null, use it directly
 * 2. Otherwise, construct URL from serviceItemId using buildLaunchUrl()
 * 3. If neither is available, fall back to entry.serviceItemId
 */
fun ResumeWithTitleRow.toResumeItem(context: Context): ResumeItem? {
    // Get the package name for the streaming service
    val packageName = entry.serviceId.tvAppPackage ?: return null

    // Build the deep link intent
    val deepLink = externalLink?.let { url ->
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage(packageName)
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("source", "30")
        }
    }

    // Parse image set for poster
    val imageSet = parseImageSet(resolvedTitleImage)
    val posterUrl = imageSet?.get("verticalPoster")?.get("w240")
        ?: imageSet?.get("horizontalPoster")?.get("w360")

    // Build subtitle (season/episode info if available)
    val subtitle = buildString {
        entry.seasonNumber?.let { append("S$it") }
        if (entry.seasonNumber != null && entry.episodeNumber != null) append(" • ")
        entry.episodeNumber?.let { append("E$it") }
    }.takeIf { it.isNotBlank() } ?: "Continue Watching"

    return ResumeItem(
        title = resolvedTitleName ?: entry.titleText,
        subtitle = subtitle,
        progressPercent = 0, // TODO: Add progress tracking
        poster = null, // TODO: Load poster drawable from URL
        appPackage = packageName,
        appBadge = null, // TODO: Load app badge
        deepLink = deepLink
    )
}

/**
 * Convert ResumeWithTitleRow to TitleItem for UI display.
 * This allows resume entries to be displayed using the standard CardPresenter.
 *
 * Launch URL logic:
 * 1. If externalLink is not null, use it directly
 * 2. Otherwise returns null for videoUrl (item won't be launchable)
 */
fun ResumeWithTitleRow.toTitleItem(): TitleItem? {
    // Parse image set
    val imageSet = parseImageSet(resolvedTitleImage)

    return TitleItem(
        id = entry.resolvedTitleId ?: 0,
        title = resolvedTitleName ?: entry.titleText,
        description = null, // Resume entries don't typically show descriptions
        backgroundImageUrl = imageSet?.get("horizontalBackdrop")?.get("w1440")
            ?: imageSet?.get("verticalBackdrop")?.get("w720"),
        cardImageUrl = imageSet?.get("verticalPoster")?.get("w240")
            ?: imageSet?.get("horizontalPoster")?.get("w360"),
        videoUrl = externalLink,
        studio = entry.serviceId.id.replaceFirstChar { it.uppercase() },
        service = entry.serviceId
    )
}

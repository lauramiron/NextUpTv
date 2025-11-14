package io.github.lauramiron.nextuptv.util

enum class StreamingService(val id: String, val displayName: String) {
    NETFLIX("netflix", "Netflix"),
    PRIME("prime", "Prime Video"),
    DISNEY("disney", "Disney+"),
    APPLE("apple", "Apple TV+"),
    HBO("hbo", "HBO Max"),
    PEACOCK("peacock", "Peacock"),
    HULU("hulu", "Hulu"),
    PARAMOUNT("paramount", "Paramount+");

    companion object {
        fun fromString(id: String): StreamingService? {
            return entries.find { it.id.equals(id, ignoreCase = true) }
        }
    }
}

/**
 * Android TV app package names for each streaming service.
 * Returns null if the service doesn't have a known TV app package.
 */
val StreamingService.tvAppPackage: String?
    get() = when (this) {
        StreamingService.NETFLIX -> "com.netflix.ninja"
//        StreamingService.APPLE -> "com.apple.atve.android"
        StreamingService.APPLE -> "com.apple.atve.androidtv.appletv"
//        StreamingService.PRIME -> "com.amazon.amazonvideo.livingroom.nvidia"
        StreamingService.PRIME -> "com.amazon.amazonvideo.livingroom"
        StreamingService.DISNEY -> "com.disney.disneyplus"
        StreamingService.HBO -> "com.hbo.hbonow"
//        StreamingService.HULU -> "com.hulu.plus"
        StreamingService.HULU -> "com.hulu.livingroomplus"
        StreamingService.PEACOCK -> "com.peacocktv.peacockandroid"
        StreamingService.PARAMOUNT -> "com.cbs.ca"
    }

/**
 * Build the launch URL for a title on this streaming service.
 * For Prime Video, constructs a custom URL using the externalId.
 * For other services, returns the movieDbLink (which may be null).
 *
 * @param movieDbLink The original link from the MovieNight API
 * @param externalId The service-specific external ID for the title
 * @return The launch URL to use, or null if unavailable
 */
fun StreamingService.buildLaunchUrl(movieDbLink: String?, externalId: String?): String? {
    return when (this) {
        StreamingService.PRIME -> {
            if (!externalId.isNullOrBlank()) {
                "https://app.primevideo.com/detail?gti=$externalId"
            } else {
                movieDbLink
            }
        }
        else -> movieDbLink
    }
}
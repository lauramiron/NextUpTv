package io.github.lauramiron.nextuptv.ui.deeplinktest

import io.github.lauramiron.nextuptv.util.StreamingService

/**
 * Represents a specific deeplink launch method to test.
 * Each card shows a different way to launch the same title.
 */
data class DeepLinkItem(
    val methodName: String,         // e.g., "HTTPS + Package", "HTTPS Only", "Custom Scheme"
    val service: StreamingService,   // which streaming service
    val externalId: String,          // service-specific ID (e.g., Netflix ID, Apple ID)
    val titleName: String,           // title being tested (for display)
    val launchMethod: LaunchMethod   // how to construct the intent
)

enum class LaunchMethod {
    HTTPS_WITH_PACKAGE,    // ACTION_VIEW with https URL and explicit package
    HTTPS_NO_PACKAGE,      // ACTION_VIEW with https URL, let system choose

    HTTPS_WITH_PACKAGE_AND_SOURCE,
    CUSTOM_SCHEME,         // Custom URL scheme (e.g., netflix://title/12345)
    WEB_FALLBACK          // Browser fallback if app not installed
}
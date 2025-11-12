package io.github.lauramiron.nextuptv.ui.deeplinktest

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import android.util.Log
import io.github.lauramiron.nextuptv.data.local.entity.StreamingService

object DeeplinkTester {
    private const val TAG = "DeeplinkTester"

    // Package names for TV apps
    private val PACKAGE_NAMES = mapOf(
        StreamingService.NETFLIX to "com.netflix.ninja",
        StreamingService.APPLE to "com.apple.atv.plus",
        StreamingService.PRIME to "com.amazon.avod.thirdpartyclient",
        StreamingService.DISNEY to "com.disney.disneyplus",
        StreamingService.HBO to "com.hbo.hbonow",
        StreamingService.HULU to "com.hulu.plus",
        StreamingService.PEACOCK to "com.peacocktv.peacockandroid"
    )

    /**
     * Launch a streaming service title using the specified method.
     */
    fun launch(context: Context, item: DeepLinkItem) {
        val intent = buildIntent(item) ?: run {
            Toast.makeText(context, "Unable to build intent for ${item.methodName}", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "Failed to build intent for: $item")
            return
        }

        val pm: PackageManager = context.packageManager

        // Check if intent can be resolved
        val canResolve = intent.resolveActivity(pm) != null
        Log.i(TAG, "Intent: ${intent.dataString}, Package: ${intent.`package`}, Can resolve: $canResolve")

        try {
            context.startActivity(intent)
            val desc = intent.dataString ?: intent.toString()
            Toast.makeText(context, "Launching: ${item.methodName}\n$desc", Toast.LENGTH_LONG).show()
            Log.i(TAG, "Launched: $desc")
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Launch failed: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Launch failed for ${intent.dataString}", e)
        }
    }

    /**
     * Build an Intent based on the launch method.
     */
    private fun buildIntent(item: DeepLinkItem): Intent? {
        val url = buildUrl(item.service, item.externalId) ?: return null
        val packageName = PACKAGE_NAMES[item.service]

        return when (item.launchMethod) {
            LaunchMethod.HTTPS_WITH_PACKAGE -> {
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    packageName?.let { setPackage(it) }
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            LaunchMethod.HTTPS_NO_PACKAGE -> {
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            LaunchMethod.CUSTOM_SCHEME -> {
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    packageName?.let { setPackage(it) }
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("source","30")
                }
//                val customUrl = buildCustomSchemeUrl(item.service, item.externalId) ?: return null
//                Intent(Intent.ACTION_VIEW, Uri.parse(customUrl)).apply {
//                    packageName?.let { setPackage(it) }
//                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                }
            }
            LaunchMethod.WEB_FALLBACK -> {
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    // No package specified - will open in browser if app can't handle it
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        }
    }

    /**
     * Build the HTTPS URL for a given service and external ID.
     */
    private fun buildUrl(service: StreamingService, externalId: String): String? {
        return when (service) {
            StreamingService.NETFLIX -> "https://www.netflix.com/watch/80077368"
            StreamingService.APPLE -> "https://tv.apple.com/us/show/severance/umc.cmc.1srk2goyh2q2zdxcx605w8vtx"
//            "https://tv.apple.com/us/episode/woes-hollow/umc.cmc.39o64vs9jfwkrt6zr653oe0dx?showId=umc.cmc.1srk2goyh2q2zdxcx605w8vtx"
            StreamingService.PRIME -> "https://www.primevideo.com/detail/$externalId"
            StreamingService.DISNEY -> "https://www.disneyplus.com/video/$externalId"
            StreamingService.HBO -> "https://play.hbomax.com/page/$externalId"
            StreamingService.HULU -> "https://www.hulu.com/watch/$externalId"
            StreamingService.PEACOCK -> "https://www.peacocktv.com/watch/playback/$externalId"
        }
    }

    /**
     * Build custom scheme URL (e.g., netflix://title/12345).
     * These may or may not work depending on the app.
     */
    private fun buildCustomSchemeUrl(service: StreamingService, externalId: String): String? {
        return when (service) {
            StreamingService.NETFLIX -> "netflix://title/$externalId"
            StreamingService.APPLE -> "videos://tv.apple.com/$externalId"
            // Other services may not support custom schemes
            else -> null
        }
    }
}

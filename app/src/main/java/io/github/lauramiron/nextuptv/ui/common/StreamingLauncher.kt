package io.github.lauramiron.nextuptv.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast

/**
 * Utility for launching streaming service apps via deep links.
 * Handles service-specific package names, intent flags, and extras.
 */
object StreamingLauncher {
    private const val TAG = "StreamingLauncher"

    // Package names for TV apps
    private const val NETFLIX_TV_PKG = "com.netflix.ninja"
    private const val PRIME_TV_PKG = "com.amazon.amazonvideo.livingroom"
    private const val DISNEY_TV_PKG = "com.disney.disneyplus"
    private const val APPLE_TV_PKG = "com.apple.atve.androidtv.appletv"
    private const val HBO_TV_PKG = "com.hbo.hbonow"
    private const val PEACOCK_TV_PKG = "com.peacocktv.peacockandroid"
    private const val HULU_TV_PKG = "com.hulu.plus"

    /**
     * Launch a streaming service app using the provided video URL.
     *
     * @param context Android context for launching the intent
     * @param videoUrl The deep link URL (e.g., https://www.netflix.com/watch/...)
     */
    fun launch(context: Context, videoUrl: String) {
        val uri = Uri.parse(videoUrl)
        val host = uri.host?.lowercase() ?: ""

        // Determine the appropriate package based on the URL host
        val packageName = when {
            host.contains("netflix.com") -> NETFLIX_TV_PKG
            host.contains("primevideo.com") || host.contains("amazon.com") -> PRIME_TV_PKG
            host.contains("disneyplus.com") -> DISNEY_TV_PKG
            host.contains("apple.com") -> APPLE_TV_PKG
            host.contains("hbomax.com") || host.contains("hbo.com") -> HBO_TV_PKG
            host.contains("peacocktv.com") -> PEACOCK_TV_PKG
            host.contains("hulu.com") -> HULU_TV_PKG
            else -> null
        }

        // Create intent candidates
        val candidates = mutableListOf<Intent>()

        // First candidate: with package pinned (TV app usually claims these)
        if (packageName != null) {
            val pinnedIntent = Intent(Intent.ACTION_VIEW, uri)
                .setPackage(packageName)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // Add service-specific extras
            when (packageName) {
                NETFLIX_TV_PKG -> pinnedIntent.putExtra("source", "30")
                // Add other service-specific extras as needed
            }

            candidates.add(pinnedIntent)
        }

        // Second candidate: without package (let user choose handler)
        val unpinnedIntent = Intent(Intent.ACTION_VIEW, uri)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        candidates.add(unpinnedIntent)

        // Try to find a resolvable intent
        val pm: PackageManager = context.packageManager
        val chosen = candidates.firstOrNull { it.resolveActivity(pm) != null }

        if (chosen == null) {
            Toast.makeText(context, "No app found to play this content", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "No handler found for URL: $videoUrl")
            return
        }

        // Launch the activity
        try {
            context.startActivity(chosen)
            val desc = chosen.dataString ?: chosen.toString()
            Toast.makeText(context, "Launching: ${chosen.`package` ?: "default app"}", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "Launched: $desc")
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Failed to launch: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Launch failed for ${chosen.dataString}", e)
        }
    }
}

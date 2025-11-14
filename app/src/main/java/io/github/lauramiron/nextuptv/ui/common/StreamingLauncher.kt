package io.github.lauramiron.nextuptv.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import io.github.lauramiron.nextuptv.util.StreamingService
import io.github.lauramiron.nextuptv.util.tvAppPackage

/**
 * Utility for launching streaming service apps via deep links.
 * Handles service-specific package names, intent flags, and extras.
 */
object StreamingLauncher {
    private const val TAG = "StreamingLauncher"

    /**
     * Launch a streaming service app using the provided video URL.
     * Does not specify a package, allowing the system to choose the handler.
     *
     * @param context Android context for launching the intent
     * @param videoUrl The deep link URL (e.g., https://www.netflix.com/watch/...)
     */
    fun launch(context: Context, videoUrl: String) {
        val uri = Uri.parse(videoUrl)
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
            Log.i(TAG, "Launched: $videoUrl")
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app found to play this content", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Launch failed for $videoUrl", e)
        }
    }

    /**
     * Launch a streaming service app using the provided video URL and service.
     * Specifies the service's package to ensure the correct app handles the link.
     *
     * @param context Android context for launching the intent
     * @param videoUrl The deep link URL (e.g., https://www.netflix.com/watch/...)
     * @param service The streaming service to launch
     */
    fun launchWithPackage(context: Context, videoUrl: String, service: StreamingService?) {
        val uri = Uri.parse(videoUrl)
        val packageName = service?.tvAppPackage

        // Create intent candidates
        val candidates = mutableListOf<Intent>()

        // First candidate: with package pinned (TV app usually claims these)
        if (packageName != null) {
            val pinnedIntent = Intent(Intent.ACTION_VIEW, uri)
                .setPackage(packageName)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // Add service-specific extras
            when (service) {
                StreamingService.NETFLIX -> pinnedIntent.putExtra("source", "30")
                // Add other service-specific extras as needed
                else -> {}
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

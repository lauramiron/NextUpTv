package io.github.lauramiron.nextuptv.ui.deeplinktest

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import android.util.Log

object DeeplinkTester {
    private const val TAG = "NetflixDeeplinkTester"
    private const val NETFLIX_TV_PKG = "com.netflix.ninja"

    /** Try a chain of candidates; start the first resolvable one. */
    fun launch(context: Context, netflixId: String) {
        val candidates: List<Intent> = listOf(
            // HTTPS with package pinned (TV app usually claims these)
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.netflix.com/watch/$netflixId?trackId=284616272")).setPackage(NETFLIX_TV_PKG),

            // Fallback: HTTPS without package (let user choose handler)
//            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.netflix.com/watch/$netflixId?trackId=284616272"), )
        ).map {
            it.addCategory(Intent.CATEGORY_BROWSABLE)
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            it.putExtra("source","30")
            it
        }

        val pm: PackageManager = context.packageManager
        val chosen = candidates.firstOrNull { it.resolveActivity(pm) != null }

        if (chosen == null) {
            Toast.makeText(context, "No handler for Netflix deeplink", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "No candidate resolved for ID=$netflixId")
            return
        }

        try {
            context.startActivity(chosen)
            val desc = chosen.dataString ?: chosen.toString()
            Toast.makeText(context, "Launching: $desc", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "Launched: $desc")
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Launch failed: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Launch failed for ${chosen.dataString}", e)
        }
    }
}

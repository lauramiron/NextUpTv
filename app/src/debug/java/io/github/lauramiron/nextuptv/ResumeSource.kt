package io.github.lauramiron.nextuptv

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources

class ResumeSource {
    fun load(context: Context): List<ResumeEntry> {
        // Debug-only icons/posters (put simple placeholders in src/debug/res/drawable/)
        fun d(resId: Int) = AppCompatResources.getDrawable(context, resId)

        return listOf(
            ResumeEntry(
                title = "Stranger Things",
                subtitle = "S2 • E3 • The Pollywog",
                progressPercent = 62,
                poster = d(R.drawable.ic_poster_netflix),      // debug poster placeholder
                appPackage = "com.netflix.ninja",
                appBadge = d(R.drawable.ic_netflix),            // app badge
                deepLink = null
            ),
            ResumeEntry(
                title = "The Mandalorian",
                subtitle = "S1 • E5 • The Gunslinger",
                progressPercent = 41,
                poster = d(R.drawable.ic_poster_disney),
                appPackage = "com.disney.disneyplus",
                appBadge = d(R.drawable.ic_disney_plus),
                deepLink = null
            ),
            ResumeEntry(
                title = "Loki",
                subtitle = "S2 • E1 • Ouroboros",
                progressPercent = 78,
                poster = d(R.drawable.ic_poster_disney),
                appPackage = "com.disney.disneyplus",
                appBadge = d(R.drawable.ic_disney_plus),
                deepLink = null
            ),
            ResumeEntry(
                title = "The Boys",
                subtitle = "S3 • E4 • Glorious Five Year Plan",
                progressPercent = 23,
                poster = d(R.drawable.ic_poster_prime),
                appPackage = "com.amazon.avod.thirdpartyclient",
                appBadge = d(R.drawable.ic_prime_video),
                deepLink = null
            )
        )
    }
}

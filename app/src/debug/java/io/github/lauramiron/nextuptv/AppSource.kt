package io.github.lauramiron.nextuptv

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources

class AppSource {
    fun loadApps(context: Context): List<AppEntry> {
        fun d(resId: Int) = AppCompatResources.getDrawable(context, resId)

        return listOf(
            AppEntry("YouTube",    "com.google.android.youtube.tv",     d(R.drawable.ic_youtube_tv),   launchIntent = null),
            AppEntry("Netflix",    "com.netflix.ninja",                 d(R.drawable.ic_netflix),       launchIntent = null),
            AppEntry("Prime Video","com.amazon.avod.thirdpartyclient", d(R.drawable.ic_prime_video),   launchIntent = null),
            AppEntry("Disney+",    "com.disney.disneyplus",             d(R.drawable.ic_disney_plus),   launchIntent = null),
            AppEntry("Hulu",       "com.hulu.plus",                     d(R.drawable.ic_hulu),          launchIntent = null),
            AppEntry("Max",        "com.hbo.hbomax",                    d(R.drawable.ic_max),           launchIntent = null),
            AppEntry("Settings",   "com.android.tv.settings",           d(android.R.drawable.ic_menu_manage),      launchIntent = null),
        )


//        val ph = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_help);
//        return listOf(
//            "com.google.android.youtube.tv" to "YouTube",
//            "com.netflix.ninja" to "Netflix",
//            "com.amazon.avod.thirdpartyclient" to "Prime Video",
//            "com.disney.disneyplus" to "Disney+",
//            "com.hulu.plus" to "Hulu",
//            "com.hbo.hbomax" to "Max",
//            "com.android.tv.settings" to "Settings"
//        ).map { (pkg, label) ->
//            AppEntry(label, pkg, ph, launchIntent = null)
//        }
    }
}
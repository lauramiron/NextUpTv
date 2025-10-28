package io.github.lauramiron.nextuptv

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import io.github.lauramiron.nextuptv.ui.app.AppItem

class AppSource {
    fun loadApps(context: Context): List<AppItem> {
        fun d(resId: Int) = AppCompatResources.getDrawable(context, resId)

        return listOf(
            AppItem("YouTube",    "com.google.android.youtube.tv",     d(R.drawable.ic_youtube_tv),   launchIntent = null),
            AppItem("Netflix",    "com.netflix.ninja",                 d(R.drawable.ic_netflix),       launchIntent = null),
            AppItem("Prime Video","com.amazon.avod.thirdpartyclient", d(R.drawable.ic_prime_video),   launchIntent = null),
            AppItem("Disney+",    "com.disney.disneyplus",             d(R.drawable.ic_disney_plus),   launchIntent = null),
            AppItem("Hulu",       "com.hulu.plus",                     d(R.drawable.ic_hulu),          launchIntent = null),
            AppItem("Max",        "com.hbo.hbomax",                    d(R.drawable.ic_max),           launchIntent = null),
            AppItem("Settings",   "com.android.tv.settings",           d(android.R.drawable.ic_menu_manage),      launchIntent = null),
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
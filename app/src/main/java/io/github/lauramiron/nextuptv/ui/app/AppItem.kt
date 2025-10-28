package io.github.lauramiron.nextuptv.ui.app

import android.content.Intent
import android.graphics.drawable.Drawable

data class AppItem (
    val label: String,
    val packageName: String,
    val icon: Drawable?,
    val launchIntent: Intent?
)
package io.github.lauramiron.nextuptv

import android.content.Intent
import android.graphics.drawable.Drawable

data class ResumeEntry(
    val title: String,                 // “Stranger Things”
    val subtitle: String,              // “S2 • E3 • The Pollywog”
    val progressPercent: Int,          // 0..100
    val poster: Drawable?,             // nullable in dev
    val appPackage: String,            // e.g., com.netflix.ninja
    val appBadge: Drawable?,           // app icon/logo
    val deepLink: Intent?              // null in dev; real in release
)

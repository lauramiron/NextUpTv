package io.github.lauramiron.nextuptv.ui.deeplinktest

data class DeepLinkItem(
    val title: String,
    val netflixId: String, // the numeric title/episode id (e.g., "80057281")
    val imageResId: Int? = null
)
package io.github.lauramiron.nextuptv.ui.app

import android.R
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter

class AppCardPresenter : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder? {
        val cardView = ImageCardView(parent?.context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setMainImageDimensions(240, 240)
        }
        return ViewHolder(cardView);
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val entry = item as AppItem
        val card = viewHolder.view as ImageCardView
        val pm = card.context.packageManager

        card.titleText = entry.label
        card.mainImage = entry.icon
            ?: runCatching { pm.getApplicationIcon(entry.packageName) }.getOrNull()
            ?: ContextCompat.getDrawable(card.context, R.drawable.ic_menu_help)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        (viewHolder.view as ImageCardView).mainImage = null;
    }
}
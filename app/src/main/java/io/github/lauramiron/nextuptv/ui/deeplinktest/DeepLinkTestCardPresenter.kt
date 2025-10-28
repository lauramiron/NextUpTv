package io.github.lauramiron.nextuptv.ui.deeplinktest

import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter

class DeepLinkTestCardPresenter : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val card = ImageCardView(parent.context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setMainImageDimensions(313, 176) // 16:9, tweak to your grid
        }
        return ViewHolder(card)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val card = viewHolder.view as ImageCardView
        val test = item as DeepLinkItem
        card.titleText = test.title
        card.contentText = "ID: ${test.netflixId}"
        val placeholder = test.imageResId?.let {
            ContextCompat.getDrawable(card.context, it)
        }
        card.mainImageView.setImageDrawable(placeholder)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) = Unit
}
package io.github.lauramiron.nextuptv.ui.details

import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import io.github.lauramiron.nextuptv.ui.details.MovieItem

class DetailsDescriptionPresenter : AbstractDetailsDescriptionPresenter() {

    override fun onBindDescription(
            viewHolder: ViewHolder,
            item: Any) {
        val movieItem = item as MovieItem

        viewHolder.title.text = movieItem.title
        viewHolder.subtitle.text = movieItem.studio
        viewHolder.body.text = movieItem.description
    }
}
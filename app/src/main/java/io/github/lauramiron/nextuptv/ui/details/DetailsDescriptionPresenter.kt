package io.github.lauramiron.nextuptv.ui.details

import androidx.leanback.widget.AbstractDetailsDescriptionPresenter

class DetailsDescriptionPresenter : AbstractDetailsDescriptionPresenter() {

    override fun onBindDescription(
            viewHolder: ViewHolder,
            item: Any) {
        val titleItem = item as TitleItem

        viewHolder.title.text = titleItem.title
        viewHolder.subtitle.text = titleItem.studio
        viewHolder.body.text = titleItem.description
    }
}
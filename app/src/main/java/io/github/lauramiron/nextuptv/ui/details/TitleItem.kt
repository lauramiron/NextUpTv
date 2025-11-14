package io.github.lauramiron.nextuptv.ui.details

import io.github.lauramiron.nextuptv.util.StreamingService
import java.io.Serializable

/**
 * Title class represents video entity with title, description, image thumbs and video url.
 */
data class TitleItem(
        var id: Long = 0,
        var title: String? = null,
        var description: String? = null,
        var backgroundImageUrl: String? = null,
        var cardImageUrl: String? = null,
        var videoUrl: String? = null,
        var studio: String? = null,
        var service: StreamingService? = null
) : Serializable {

    override fun toString(): String {
        return "TitleItem{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", videoUrl='" + videoUrl + '\'' +
                ", backgroundImageUrl='" + backgroundImageUrl + '\'' +
                ", cardImageUrl='" + cardImageUrl + '\'' +
                ", service=" + service +
                '}'
    }

    companion object {
        internal const val serialVersionUID = 727566175075960653L
    }
}

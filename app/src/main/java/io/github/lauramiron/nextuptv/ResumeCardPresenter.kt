package io.github.lauramiron.nextuptv

import android.R.attr.indeterminate
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter

class ResumeCardPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        // Wrap ImageCardView so we can overlay a ProgressBar and a badge
        val container = FrameLayout(parent.context)

        val card = ImageCardView(parent.context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            // 16:9 card for shows/movies (adjust to taste)
            setMainImageDimensions(320, 180)
        }
        container.addView(card, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        // Progress bar overlay
        val progress = ProgressBar(parent.context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            // style it lightly; or use a custom drawable
//            indeterminate = 1
            progressDrawable?.setColorFilter(0xFF00BFA5.toInt(), PorterDuff.Mode.SRC_IN) // teal-ish
            visibility = View.VISIBLE
        }
        val progressLp = FrameLayout.LayoutParams(card.mainImageView.layoutParams.width, 8).apply {
            // anchor to bottom of the image area
            topMargin = 180 - 8
        }
        container.addView(progress, progressLp)

        // Badge (app icon) in the corner
        val badge = androidx.appcompat.widget.AppCompatImageView(parent.context).apply {
            adjustViewBounds = true
            visibility = View.VISIBLE
        }
        val badgeSize = 48
        val badgeLp = FrameLayout.LayoutParams(badgeSize, badgeSize).apply {
            // bottom-left over the poster
            topMargin = 180 - badgeSize - 8
            marginStart = 8
        }
        container.addView(badge, badgeLp)

        container.tag = Holder(card, progress, badge)
        return ViewHolder(container)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        val entry = item as ResumeEntry
        val container = viewHolder.view as FrameLayout
        val holder = container.tag as Holder
        val card = holder.card
        val pm = card.context.packageManager

        // Title/subtitle
        card.titleText = entry.title
        card.contentText = entry.subtitle

        // Poster (fallback to a neutral placeholder)
        val poster: Drawable = entry.poster
            ?: runCatching { pm.getApplicationIcon(entry.appPackage) }.getOrNull()
            ?: ContextCompat.getDrawable(card.context, R.drawable.ic_placeholder_poster)
            ?: ContextCompat.getDrawable(card.context, android.R.drawable.ic_menu_report_image)!!

        card.mainImage = poster

        // Progress
        holder.progress.progress = entry.progressPercent.coerceIn(0, 100)

        // App badge
        val badgeDrawable: Drawable? = entry.appBadge
            ?: runCatching { pm.getApplicationIcon(entry.appPackage) }.getOrNull()
        holder.badge.setImageDrawable(badgeDrawable)
        holder.badge.visibility = if (badgeDrawable != null) View.VISIBLE else View.GONE

        // Optional visual hint if not launchable (dev)
        card.alpha = if (entry.deepLink == null) 0.95f else 1f
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val holder = (viewHolder.view.tag as Holder)
        holder.card.mainImage = null
        holder.badge.setImageDrawable(null)
    }

    private data class Holder(
        val card: ImageCardView,
        val progress: ProgressBar,
        val badge: androidx.appcompat.widget.AppCompatImageView
    )
}

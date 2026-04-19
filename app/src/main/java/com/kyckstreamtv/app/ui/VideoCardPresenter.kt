package com.kyckstreamtv.app.ui

import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.kyckstreamtv.app.R
import com.kyckstreamtv.app.model.VideoItem

class VideoCardPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = ImageCardView(parent.context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        }
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        val video = item as? VideoItem ?: return
        val cardView = viewHolder.view as ImageCardView

        cardView.titleText = video.sessionTitle?.takeIf { it.isNotBlank() } ?: "Video"
        cardView.contentText = buildContentText(video)
        cardView.setMainImageScaleType(ImageView.ScaleType.CENTER_CROP)

        val imageUrl = video.thumbnail?.src?.takeIf { it.isNotBlank() }
        val imageTarget = cardView.mainImageView ?: return

        if (imageUrl != null) {
            Glide.with(cardView.context)
                .load(imageUrl)
                .centerCrop()
                .placeholder(ContextCompat.getDrawable(cardView.context, R.drawable.app_banner))
                .into(imageTarget)
        } else {
            cardView.mainImage = ContextCompat.getDrawable(cardView.context, R.drawable.app_banner)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        Glide.with(cardView.context).clear(cardView.mainImageView)
        cardView.badgeImage = null
        cardView.mainImage = null
    }

    private fun buildContentText(video: VideoItem): String {
        val durationStr = formatDuration(video.duration)
        val viewsStr = if (video.views > 0) "${formatViews(video.views)} zhlédnutí" else ""
        val dateStr = formatStartTime(video.startTime)
        return listOf(dateStr, durationStr, viewsStr).filter { it.isNotBlank() }.joinToString(" • ")
    }

    private fun formatStartTime(startTime: String?): String {
        if (startTime.isNullOrBlank()) return ""
        return try {
            // Input: "2026-04-18 21:54:14"
            val parts = startTime.split(" ")
            val dateParts = parts[0].split("-")
            val timePart = parts.getOrNull(1)?.substring(0, 5) ?: ""
            val day = dateParts[2].toInt()
            val month = dateParts[1].toInt()
            val year = dateParts[0]
            "$day.$month.$year $timePart"
        } catch (e: Exception) {
            startTime
        }
    }

    private fun formatDuration(ms: Long): String {
        if (ms <= 0) return ""
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    private fun formatViews(count: Int) = when {
        count >= 1000 -> "${count / 1000}.${(count % 1000) / 100}K"
        else -> "$count"
    }

    companion object {
        private const val CARD_WIDTH = 320
        private const val CARD_HEIGHT = 180
    }
}

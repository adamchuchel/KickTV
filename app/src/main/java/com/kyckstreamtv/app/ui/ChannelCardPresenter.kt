package com.kyckstreamtv.app.ui

import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.kyckstreamtv.app.R
import com.kyckstreamtv.app.model.ChannelCard

class ChannelCardPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = ImageCardView(parent.context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        }
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        val card = item as? ChannelCard ?: return
        val cardView = viewHolder.view as ImageCardView
        cardView.titleText = card.username
        cardView.contentText = formatViewers(card.viewerCount)
        cardView.setMainImageScaleType(ImageView.ScaleType.CENTER_CROP)

        val imageTarget = cardView.mainImageView ?: return
        val imageUrl = card.thumbnailUrl?.takeIf { it.isNotBlank() }
            ?: card.profilePicUrl?.takeIf { it.isNotBlank() }

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

    private fun formatViewers(count: Int) = when {
        count >= 1000 -> "${count / 1000}.${(count % 1000) / 100}K"
        else -> "$count"
    }

    companion object {
        private const val CARD_WIDTH = 320
        private const val CARD_HEIGHT = 180
    }
}

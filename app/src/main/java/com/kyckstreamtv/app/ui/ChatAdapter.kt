package com.kyckstreamtv.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.kyckstreamtv.app.R
import com.kyckstreamtv.app.model.ChatMessage
import java.lang.ref.WeakReference

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private val items = mutableListOf<ChatMessage>()
    var fontSizeSp: Float = 13f
        private set

    fun setMessages(messages: List<ChatMessage>) {
        items.clear()
        items.addAll(messages)
        notifyDataSetChanged()
    }

    fun setFontSize(sp: Float) {
        if (fontSizeSp != sp) {
            fontSizeSp = sp
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(items[position], fontSizeSp)
    }

    override fun getItemCount(): Int = items.size

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
        private val tvMessage: TextView = itemView.findViewById(R.id.tv_message)

        fun bind(message: ChatMessage, fontSizeSp: Float) {
            val emotePx = (fontSizeSp * itemView.resources.displayMetrics.density)
                .toInt().coerceIn(24, 64)

            tvUsername.textSize = fontSizeSp
            tvUsername.text = "${message.sender.username}: "

            val colorStr = message.sender.identity?.color
            tvUsername.setTextColor(
                if (!colorStr.isNullOrBlank()) {
                    try { Color.parseColor(colorStr) } catch (_: Exception) { DEFAULT_COLOR }
                } else { DEFAULT_COLOR }
            )

            tvMessage.textSize = fontSizeSp
            tvMessage.text = buildEmoteSpannable(tvMessage, message.content, emotePx)
        }

        private fun buildEmoteSpannable(tv: TextView, content: String, sizePx: Int): CharSequence {
            val matches = EMOTE_REGEX.findAll(content).toList()
            if (matches.isEmpty()) return content

            val sb = SpannableStringBuilder()
            var lastEnd = 0

            for (match in matches) {
                sb.append(content.substring(lastEnd, match.range.first))

                val emoteId = match.groupValues[1]
                val emoteDrawable = EmoteDrawable(WeakReference(tv), sizePx)
                emoteDrawable.setBounds(0, 0, sizePx, sizePx)

                val spanStart = sb.length
                sb.append("\u200B")
                sb.setSpan(
                    ImageSpan(emoteDrawable, ImageSpan.ALIGN_BOTTOM),
                    spanStart, spanStart + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                Glide.with(tv.context)
                    .asBitmap()
                    .load("https://files.kick.com/emotes/$emoteId/fullsize")
                    .into(object : CustomTarget<Bitmap>(sizePx, sizePx) {
                        override fun onResourceReady(resource: Bitmap, t: Transition<in Bitmap>?) {
                            val bd = BitmapDrawable(tv.resources, resource).also {
                                it.setBounds(0, 0, sizePx, sizePx)
                            }
                            emoteDrawable.set(bd)
                        }
                        override fun onLoadCleared(p: Drawable?) {}
                    })

                lastEnd = match.range.last + 1
            }
            sb.append(content.substring(lastEnd))
            return sb
        }

        companion object {
            private val DEFAULT_COLOR = Color.parseColor("#53FC18")
            private val EMOTE_REGEX = Regex("""\[emote:(\d+):[^\]]+\]""")
        }
    }
}

private class EmoteDrawable(private val ref: WeakReference<TextView>, private val size: Int) : Drawable() {
    private var inner: Drawable? = null

    fun set(drawable: Drawable) {
        inner = drawable
        ref.get()?.post { ref.get()?.invalidate() }
    }

    override fun draw(canvas: Canvas) { inner?.draw(canvas) }
    override fun setAlpha(alpha: Int) { inner?.alpha = alpha }
    override fun setColorFilter(cf: ColorFilter?) { inner?.colorFilter = cf }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
    override fun getIntrinsicWidth() = size
    override fun getIntrinsicHeight() = size
}

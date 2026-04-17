package com.kyckstreamtv.app.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kyckstreamtv.app.R
import com.kyckstreamtv.app.model.ChatMessage

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private val items = mutableListOf<ChatMessage>()

    fun setMessages(messages: List<ChatMessage>) {
        val prevSize = items.size
        items.clear()
        items.addAll(messages)
        if (prevSize == 0) {
            notifyDataSetChanged()
        } else {
            notifyItemRangeInserted(prevSize, items.size - prevSize)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
        private val tvMessage: TextView = itemView.findViewById(R.id.tv_message)

        fun bind(message: ChatMessage) {
            tvUsername.text = message.sender.username
            tvMessage.text = message.content

            val colorStr = message.sender.identity?.color
            tvUsername.setTextColor(
                if (!colorStr.isNullOrBlank()) {
                    try { Color.parseColor(colorStr) } catch (e: Exception) { DEFAULT_USERNAME_COLOR }
                } else {
                    DEFAULT_USERNAME_COLOR
                }
            )
        }

        companion object {
            private val DEFAULT_USERNAME_COLOR = Color.parseColor("#53FC18")
        }
    }
}

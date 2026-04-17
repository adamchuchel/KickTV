package com.kyckstreamtv.app.model

import com.google.gson.annotations.SerializedName

data class ChatMessage(
    val id: String,
    @SerializedName("chatroom_id") val chatroomId: Int,
    val content: String,
    val type: String,
    @SerializedName("created_at") val createdAt: String,
    val sender: ChatSender
)

data class ChatSender(
    val id: Int,
    val username: String,
    val slug: String,
    val identity: ChatIdentity?
)

data class ChatIdentity(
    val color: String?,
    val badges: List<ChatBadge>?
)

data class ChatBadge(
    val type: String,
    val text: String?,
    val count: Int?
)

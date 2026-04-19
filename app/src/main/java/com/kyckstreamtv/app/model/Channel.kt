package com.kyckstreamtv.app.model

import com.google.gson.annotations.SerializedName

data class ChannelResponse(
    val id: Int,
    val slug: String,
    @SerializedName("user_id") val userId: Int,
    val user: ChannelUser,
    val chatroom: Chatroom?,
    val livestream: LivestreamSummary?,
    @SerializedName("playback_url") val playbackUrl: String?
)

data class ChannelUser(
    val id: Int,
    val username: String,
    @SerializedName("profile_pic") val profilePic: String?
)

data class Chatroom(
    val id: Int,
    @SerializedName("channel_id") val channelId: Int
)

data class PlaybackUrlResponse(val data: String?)

data class LivestreamSummary(
    val id: Int,
    @SerializedName("session_title") val sessionTitle: String?,
    @SerializedName("is_live") val isLive: Boolean,
    @SerializedName("viewer_count") val viewerCount: Int = 0,
    val thumbnail: StreamThumbnail? = null
)

package com.kyckstreamtv.app.model

import com.google.gson.annotations.SerializedName

// ChannelResponse is in Channel.kt (same package — no import needed)

data class ChannelCard(
    val slug: String,
    val username: String,
    val title: String,
    val viewerCount: Int,
    val thumbnailUrl: String?,
    val profilePicUrl: String?
)

data class FollowedChannelsResponse(
    val channels: List<FollowedChannelItem>? = null,
    val data: List<ChannelResponse>? = null  // fallback for older endpoint shape
)

data class FollowedChannelItem(
    @SerializedName("channel_slug") val channelSlug: String,
    @SerializedName("user_username") val userUsername: String,
    @SerializedName("session_title") val sessionTitle: String?,
    @SerializedName("is_live") val isLive: Boolean = false,
    @SerializedName("viewer_count") val viewerCount: Int = 0,
    @SerializedName("profile_picture") val profilePicture: String?,
    @SerializedName("banner_picture") val bannerPicture: String?
)

data class LivestreamsResponse(
    val data: List<LivestreamItem>
)

data class LivestreamItem(
    val id: Int,
    val slug: String,
    @SerializedName("session_title") val sessionTitle: String?,
    @SerializedName("is_live") val isLive: Boolean = true,
    @SerializedName("viewer_count") val viewerCount: Int = 0,
    val thumbnail: StreamThumbnail?,
    val channel: LivestreamChannel?
)

data class LivestreamChannel(
    val id: Int,
    val slug: String,
    val user: ChannelUser?
)

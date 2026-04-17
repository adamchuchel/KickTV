package com.kyckstreamtv.app.model

import com.google.gson.annotations.SerializedName

data class LivestreamResponse(
    val id: Int,
    @SerializedName("channel_id") val channelId: Int,
    @SerializedName("session_title") val sessionTitle: String?,
    @SerializedName("is_live") val isLive: Boolean,
    @SerializedName("viewer_count") val viewerCount: Int,
    @SerializedName("playback_url") val playbackUrl: String?,
    val thumbnail: StreamThumbnail?
)

data class StreamThumbnail(
    val url: String?
)

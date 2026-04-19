package com.kyckstreamtv.app.model

import com.google.gson.annotations.SerializedName

data class VideoItem(
    val id: Long,
    @SerializedName("channel_id") val channelId: Int,
    @SerializedName("session_title") val sessionTitle: String?,
    @SerializedName("start_time") val startTime: String?,
    val source: String?,
    val duration: Long = 0,
    val thumbnail: VideoThumbnail?,
    val views: Int = 0,
    val categories: List<VideoCategory>?
)

data class VideoThumbnail(
    val src: String?
)

data class VideoCategory(
    val id: Int,
    val name: String?,
    val slug: String?
)

data class VideosResponse(
    val data: List<VideoItem>
)

data class VodMessagesData(
    val messages: List<ChatMessage>?
)

data class VodMessagesResponse(
    val data: VodMessagesData?,
    val messages: List<ChatMessage>?
)

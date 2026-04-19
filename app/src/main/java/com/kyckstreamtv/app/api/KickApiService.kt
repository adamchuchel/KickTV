package com.kyckstreamtv.app.api

import com.kyckstreamtv.app.model.ChannelResponse
import com.kyckstreamtv.app.model.FollowedChannelsResponse
import com.kyckstreamtv.app.model.LivestreamResponse
import com.kyckstreamtv.app.model.LivestreamsResponse
import com.kyckstreamtv.app.model.PlaybackUrlResponse
import com.kyckstreamtv.app.model.VideoItem
import com.kyckstreamtv.app.model.VideosResponse
import com.kyckstreamtv.app.model.VodMessagesResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface KickApiService {

    @GET("v2/channels/{username}")
    suspend fun getChannel(@Path("username") username: String): ChannelResponse

    @GET("v2/channels/{slug}/playback-url")
    suspend fun getPlaybackUrl(@Path("slug") slug: String): PlaybackUrlResponse

    @GET("v2/channels/{username}/livestream")
    suspend fun getLivestream(@Path("username") username: String): LivestreamResponse

    @GET("v2/channels/followed")
    suspend fun getFollowedChannels(): FollowedChannelsResponse

    @GET("/api/v1/user/livestreams")
    suspend fun getUserLivestreams(
        @Query("sort") sort: String = "desc"
    ): LivestreamsResponse

    @GET("/stream/livestreams/en")
    suspend fun getLivestreams(
        @Query("limit") limit: Int = 20,
        @Query("sort") sort: String = "desc"
    ): LivestreamsResponse

    @GET("v2/channels/{slug}/videos")
    suspend fun getChannelVideos(
        @Path("slug") slug: String,
        @Query("page") page: Int = 1
    ): List<VideoItem>

    @GET("v2/channels/{chatroomId}/messages")
    suspend fun getVodMessages(
        @Path("chatroomId") chatroomId: Int,
        @Query("start_time") startTime: String
    ): VodMessagesResponse
}

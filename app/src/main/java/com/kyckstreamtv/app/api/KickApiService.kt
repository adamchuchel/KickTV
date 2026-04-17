package com.kyckstreamtv.app.api

import com.kyckstreamtv.app.model.ChannelResponse
import com.kyckstreamtv.app.model.LivestreamResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface KickApiService {

    @GET("v2/channels/{username}")
    suspend fun getChannel(@Path("username") username: String): ChannelResponse

    @GET("v2/channels/{username}/livestream")
    suspend fun getLivestream(@Path("username") username: String): LivestreamResponse
}

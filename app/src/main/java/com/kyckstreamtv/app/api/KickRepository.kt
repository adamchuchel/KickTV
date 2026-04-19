package com.kyckstreamtv.app.api

import com.google.gson.GsonBuilder
import com.kyckstreamtv.app.model.LivestreamsResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface KickPublicApiService {
    @GET("public/v1/livestreams")
    suspend fun getLivestreams(
        @Query("limit") limit: Int = 20,
        @Query("sort") sort: String = "viewer_count"
    ): LivestreamsResponse
}

object KickRepository {

    // Set by KyckStreamApp on startup
    var tokenProvider: (() -> String?)? = null

    private const val BASE_URL = "https://kick.com/api/"
    private const val PUBLIC_BASE_URL = "https://api.kick.com/"

    private val gson = GsonBuilder().setLenient().create()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val token = tokenProvider?.invoke()
            val req = chain.request().newBuilder()
                .header("Accept", "application/json")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Referer", "https://kick.com")
                .header("Origin", "https://kick.com")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 9; Android TV) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36")
                .apply { if (token != null) header("Authorization", "Bearer $token") }
                .build()
            chain.proceed(req)
        }
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    private val publicHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("Accept", "application/json")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 9; Android TV) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36")
                .build()
            chain.proceed(req)
        }
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private val publicRetrofit = Retrofit.Builder()
        .baseUrl(PUBLIC_BASE_URL)
        .client(publicHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val api: KickApiService = retrofit.create(KickApiService::class.java)
    val publicApi: KickPublicApiService = publicRetrofit.create(KickPublicApiService::class.java)
}

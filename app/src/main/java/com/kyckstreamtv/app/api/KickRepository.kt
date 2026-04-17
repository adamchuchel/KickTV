package com.kyckstreamtv.app.api

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object KickRepository {

    // Set by KyckStreamApp on startup
    var tokenProvider: (() -> String?)? = null

    private const val BASE_URL = "https://kick.com/api/"

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

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
        .build()

    val api: KickApiService = retrofit.create(KickApiService::class.java)
}

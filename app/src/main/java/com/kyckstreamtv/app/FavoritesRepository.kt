package com.kyckstreamtv.app

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class FavoriteChannel(
    val slug: String,
    val username: String,
    val profilePicUrl: String?
)

object FavoritesRepository {

    private const val PREFS_NAME = "favorites"
    private const val KEY_CHANNELS = "channels"

    private val gson = Gson()
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun getAll(): List<FavoriteChannel> {
        val json = prefs().getString(KEY_CHANNELS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<FavoriteChannel>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(channel: FavoriteChannel) {
        val list = getAll().toMutableList()
        if (list.none { it.slug == channel.slug }) {
            list.add(0, channel) // newest first
            save(list)
        }
    }

    fun remove(slug: String) {
        save(getAll().filter { it.slug != slug })
    }

    fun isFavorite(slug: String): Boolean = getAll().any { it.slug == slug }

    private fun save(list: List<FavoriteChannel>) {
        prefs().edit().putString(KEY_CHANNELS, gson.toJson(list)).apply()
    }

    private fun prefs() = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

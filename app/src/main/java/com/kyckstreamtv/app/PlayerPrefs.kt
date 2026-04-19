package com.kyckstreamtv.app

import android.content.Context
import android.content.SharedPreferences

class PlayerPrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("player_prefs", Context.MODE_PRIVATE)

    // alpha: 0–100% stored directly as 0–100
    var chatOpacityProgress: Int
        get() = prefs.getInt("chat_opacity2", 75)
        set(v) { prefs.edit().putInt("chat_opacity2", v.coerceIn(0, 100)).apply() }

    // width: 15–50% of screen (seeker 0-35, pct = 15+progress)
    var chatWidthProgress: Int
        get() = prefs.getInt("chat_width", 13)
        set(v) { prefs.edit().putInt("chat_width", v.coerceIn(0, 35)).apply() }

    // height: 10–100% of screen (seeker 0-90, pct = 10+progress)
    var chatHeightProgress: Int
        get() = prefs.getInt("chat_height2", 90)
        set(v) { prefs.edit().putInt("chat_height2", v.coerceIn(0, 90)).apply() }

    // font: 10–20sp (seeker 0-10, sp = 10+progress)
    var chatFontProgress: Int
        get() = prefs.getInt("chat_font", 3)
        set(v) { prefs.edit().putInt("chat_font", v.coerceIn(0, 10)).apply() }

    // Horizontal position: 0=left, 1=center, 2=right
    var chatHPos: Int
        get() = prefs.getInt("chat_h_pos", 2)
        set(v) { prefs.edit().putInt("chat_h_pos", v.coerceIn(0, 2)).apply() }

    // Vertical position: 0=top, 1=middle, 2=bottom
    var chatVPos: Int
        get() = prefs.getInt("chat_v_pos", 2)
        set(v) { prefs.edit().putInt("chat_v_pos", v.coerceIn(0, 2)).apply() }

    val chatAlpha: Float get() = chatOpacityProgress / 100f
    val chatWidthPercent: Int get() = 15 + chatWidthProgress
    val chatHeightPercent: Int get() = 10 + chatHeightProgress
    val chatFontSizeSp: Float get() = (10 + chatFontProgress).toFloat()
}

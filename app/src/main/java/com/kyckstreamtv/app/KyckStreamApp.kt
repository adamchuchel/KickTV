package com.kyckstreamtv.app

import android.app.Application
import com.kyckstreamtv.app.api.KickRepository

class KyckStreamApp : Application() {

    lateinit var authManager: KickAuthManager
        private set

    override fun onCreate() {
        super.onCreate()
        authManager = KickAuthManager(this)
        KickRepository.tokenProvider = { authManager.getAccessToken() }
        FavoritesRepository.init(this)
    }
}

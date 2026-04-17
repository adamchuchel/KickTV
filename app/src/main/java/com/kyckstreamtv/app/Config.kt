package com.kyckstreamtv.app

object Config {
    // 1. Go to kick.com/settings/developer
    // 2. Create app, set Redirect URI to:
    //    https://adamchuchel.github.io/KickTV/callback.html
    // 3. Paste your Client ID below
    const val KICK_CLIENT_ID = "YOUR_CLIENT_ID_HERE"

    const val KICK_REDIRECT_URI = "https://adamchuchel.github.io/KickTV/callback.html"
    const val KICK_REDIRECT_SCHEME = "kyckstreamtv://auth/callback"
    const val KICK_OAUTH_SCOPES = "channel:read events:subscribe chat:write"
    const val KICK_AUTH_URL = "https://id.kick.com/oauth/authorize"
    const val KICK_TOKEN_URL = "https://id.kick.com/oauth/token"
}

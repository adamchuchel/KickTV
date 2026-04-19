package com.kyckstreamtv.app

object Config {
    const val KICK_CLIENT_ID = "01KKPM9H64E50HRVWXSH55X6NM"
    // WARNING: do not commit client_secret to a public repository
    const val KICK_CLIENT_SECRET = "d6919d36a5266f052b9ce86d8c12fda7043c2703e14f1de0c7aeeac773523900"

    const val KICK_REDIRECT_URI = "https://localhost/oauth/callback"
    const val KICK_OAUTH_SCOPES = "user:read channel:read chat:write"
    const val KICK_AUTH_URL = "https://id.kick.com/oauth/authorize"
    const val KICK_TOKEN_URL = "https://id.kick.com/oauth/token"
}

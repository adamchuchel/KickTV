package com.kyckstreamtv.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

class KickAuthManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("kyck_auth", Context.MODE_PRIVATE)
    private val client = OkHttpClient()

    private var pendingVerifier: String? = null
    private var pendingState: String? = null

    fun isAuthenticated(): Boolean {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return false
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        return token.isNotBlank() && System.currentTimeMillis() < expiresAt - 60_000
    }

    fun getAccessToken(): String? =
        prefs.getString(KEY_ACCESS_TOKEN, null).takeIf { isAuthenticated() }

    fun buildAuthUrl(): String {
        val verifier = generateCodeVerifier()
        pendingVerifier = verifier
        val state = generateState()
        pendingState = state
        val challenge = generateCodeChallenge(verifier)
        return "${Config.KICK_AUTH_URL}" +
            "?client_id=${Config.KICK_CLIENT_ID}" +
            "&redirect_uri=${android.net.Uri.encode(Config.KICK_REDIRECT_URI)}" +
            "&response_type=code" +
            "&scope=${Config.KICK_OAUTH_SCOPES.replace(" ", "%20")}" +
            "&code_challenge=$challenge" +
            "&code_challenge_method=S256" +
            "&state=$state"
    }

    suspend fun exchangeCode(code: String): Boolean = withContext(Dispatchers.IO) {
        val verifier = pendingVerifier ?: return@withContext false
        try {
            val body = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("client_id", Config.KICK_CLIENT_ID)
                .add("client_secret", Config.KICK_CLIENT_SECRET)
                .add("code", code)
                .add("redirect_uri", Config.KICK_REDIRECT_URI)
                .add("code_verifier", verifier)
                .build()

            val request = Request.Builder()
                .url(Config.KICK_TOKEN_URL)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val bodyStr = response.body?.string() ?: return@withContext false

            if (!response.isSuccessful) {
                Log.e(TAG, "Token exchange failed ${response.code}: $bodyStr")
                return@withContext false
            }

            val json = JSONObject(bodyStr)
            saveTokens(
                accessToken = json.getString("access_token"),
                refreshToken = json.optString("refresh_token"),
                expiresIn = json.optLong("expires_in", 3600)
            )
            Log.d(TAG, "OAuth token saved")
            true
        } catch (e: Exception) {
            Log.e(TAG, "exchangeCode error: ${e.message}")
            false
        }
    }

    suspend fun refreshIfNeeded(): Boolean {
        if (isAuthenticated()) return true
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return false
        return withContext(Dispatchers.IO) {
            try {
                val body = FormBody.Builder()
                    .add("grant_type", "refresh_token")
                    .add("client_id", Config.KICK_CLIENT_ID)
                    .add("client_secret", Config.KICK_CLIENT_SECRET)
                    .add("refresh_token", refreshToken)
                    .build()
                val request = Request.Builder().url(Config.KICK_TOKEN_URL).post(body).build()
                val response = client.newCall(request).execute()
                val bodyStr = response.body?.string() ?: return@withContext false
                if (!response.isSuccessful) return@withContext false
                val json = JSONObject(bodyStr)
                saveTokens(
                    accessToken = json.getString("access_token"),
                    refreshToken = json.optString("refresh_token", refreshToken),
                    expiresIn = json.optLong("expires_in", 3600)
                )
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    fun logout() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Logged out")
    }

    private fun saveTokens(accessToken: String, refreshToken: String, expiresIn: Long) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken.ifBlank { null })
            .putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + expiresIn * 1000)
            .apply()
    }

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(96)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun generateState(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    companion object {
        private const val TAG = "KyckAuth"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
    }
}

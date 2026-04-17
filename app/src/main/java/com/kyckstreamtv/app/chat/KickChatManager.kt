package com.kyckstreamtv.app.chat

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.kyckstreamtv.app.model.ChatMessage
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit

class KickChatManager(
    private val chatroomId: Int,
    private val onMessage: (ChatMessage) -> Unit,
    private val onConnectionStateChange: (Boolean) -> Unit
) {
    private var webSocket: WebSocket? = null
    private val gson = Gson()
    private var pingTimer: Timer? = null

    private val client = OkHttpClient.Builder()
        .pingInterval(0, TimeUnit.SECONDS)
        .build()

    fun connect() {
        val url = "wss://ws-us2.pusher.com/app/eb1d5f283081a78b932c" +
            "?protocol=7&client=js&version=7.6.0&flash=false"

        val request = Request.Builder()
            .url(url)
            .header("Origin", "https://kick.com")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                onConnectionStateChange(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $reason")
                onConnectionStateChange(false)
                stopPing()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error: ${t.message}")
                onConnectionStateChange(false)
                stopPing()
            }
        })
    }

    private fun handleMessage(text: String) {
        try {
            val json = JsonParser.parseString(text).asJsonObject
            val event = json.get("event")?.asString ?: return

            when (event) {
                "pusher:connection_established" -> {
                    subscribe()
                    startPing()
                }
                "pusher:ping" -> {
                    webSocket?.send("""{"event":"pusher:pong","data":{}}""")
                }
                "App\\Events\\ChatMessageEvent" -> {
                    val dataStr = json.get("data")?.asString ?: return
                    val message = gson.fromJson(dataStr, ChatMessage::class.java)
                    if (message.type == "message") {
                        onMessage(message)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse message: ${e.message}")
        }
    }

    private fun subscribe() {
        val msg = """{"event":"pusher:subscribe","data":{"auth":"","channel":"chatrooms.$chatroomId.v2"}}"""
        webSocket?.send(msg)
        Log.d(TAG, "Subscribed to chatroom $chatroomId")
    }

    private fun startPing() {
        pingTimer?.cancel()
        pingTimer = Timer()
        pingTimer?.schedule(object : TimerTask() {
            override fun run() {
                webSocket?.send("""{"event":"pusher:ping","data":{}}""")
            }
        }, PING_INTERVAL_MS, PING_INTERVAL_MS)
    }

    private fun stopPing() {
        pingTimer?.cancel()
        pingTimer = null
    }

    fun disconnect() {
        stopPing()
        webSocket?.close(1000, "Closed by client")
        webSocket = null
        Log.d(TAG, "WebSocket disconnected")
    }

    companion object {
        private const val TAG = "KyckStreamTV"
        private const val PING_INTERVAL_MS = 30_000L
    }
}

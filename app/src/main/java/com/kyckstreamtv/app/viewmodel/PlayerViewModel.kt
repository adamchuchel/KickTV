package com.kyckstreamtv.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyckstreamtv.app.api.KickRepository
import com.kyckstreamtv.app.chat.KickChatManager
import com.kyckstreamtv.app.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

sealed class PlayerState {
    object Loading : PlayerState()
    data class Ready(val title: String, val streamUrl: String, val viewerCount: Int) : PlayerState()
    object Offline : PlayerState()
    data class Error(val message: String) : PlayerState()
}

class PlayerViewModel : ViewModel() {

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Loading)
    val playerState: StateFlow<PlayerState> = _playerState

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _chatConnected = MutableStateFlow(false)
    val chatConnected: StateFlow<Boolean> = _chatConnected

    private var chatManager: KickChatManager? = null
    private var vodPollingJob: Job? = null
    var chatDelayMs: Long = 0L

    // VOD state
    private var isVodMode = false
    private var vodChatroomId: Int = -1
    private var vodStartEpochMs: Long = 0L
    private var vodCurrentOffsetMs: Long = 0L

    fun loadChannel(username: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _playerState.value = PlayerState.Loading
            try {
                Log.d(TAG, "Fetching channel: $username")
                val channel = KickRepository.api.getChannel(username)
                Log.d(TAG, "Channel OK — chatroomId=${channel.chatroom?.id} isLive=${channel.livestream?.isLive} channelPlaybackUrl=${channel.playbackUrl}")

                val chatroomId = channel.chatroom?.id

                val livestream = try {
                    KickRepository.api.getLivestream(username).also {
                        Log.d(TAG, "Livestream OK — isLive=${it.isLive} playbackUrl=${it.playbackUrl}")
                    }
                } catch (e: HttpException) {
                    Log.w(TAG, "Livestream endpoint ${e.code()}: ${e.message()}")
                    null
                } catch (e: Exception) {
                    Log.w(TAG, "Livestream endpoint error: ${e.message}")
                    null
                }

                val streamUrl = livestream?.playbackUrl?.takeIf { it.isNotBlank() }
                    ?: channel.playbackUrl?.takeIf { it.isNotBlank() }
                    ?: try {
                        KickRepository.api.getPlaybackUrl(username).data?.takeIf { it.isNotBlank() }
                            .also { Log.d(TAG, "playback-url endpoint: $it") }
                    } catch (e: Exception) {
                        Log.w(TAG, "getPlaybackUrl failed: ${e.message}")
                        null
                    }

                val isLive = channel.livestream?.isLive ?: (livestream?.isLive ?: false)

                if (!isLive || streamUrl.isNullOrBlank()) {
                    Log.d(TAG, "Channel offline — isLive=$isLive streamUrl=$streamUrl")
                    _playerState.value = PlayerState.Offline
                    return@launch
                }

                val title = livestream?.sessionTitle?.takeIf { it.isNotBlank() }
                    ?: channel.livestream?.sessionTitle?.takeIf { it.isNotBlank() }
                    ?: channel.user.username
                val viewers = livestream?.viewerCount ?: 0

                Log.d(TAG, "Ready — title=$title url=$streamUrl viewers=$viewers")
                _playerState.value = PlayerState.Ready(title, streamUrl, viewers)

                if (chatroomId != null) {
                    connectChat(chatroomId)
                }

            } catch (e: HttpException) {
                val body = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                val msg = when (e.code()) {
                    404 -> "Channel \"$username\" not found"
                    403 -> "Access denied by Kick API (403)"
                    429 -> "Too many requests — try again later"
                    else -> "API error ${e.code()}"
                }
                Log.e(TAG, "HTTP ${e.code()} — body: $body")
                _playerState.value = PlayerState.Error(msg)
            } catch (e: Exception) {
                Log.e(TAG, "Exception: ${e.javaClass.simpleName}: ${e.message}")
                _playerState.value = PlayerState.Error("${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    fun loadVod(vodUrl: String, startTime: String, chatroomId: Int, title: String) {
        isVodMode = true
        vodChatroomId = chatroomId
        vodStartEpochMs = parseStartTime(startTime)
        vodCurrentOffsetMs = 0L

        Log.d(TAG, "VOD mode: url=$vodUrl startTime=$startTime chatroomId=$chatroomId")
        _playerState.value = PlayerState.Ready(title, vodUrl, 0)
    }

    fun updateVodPosition(playerPositionMs: Long) {
        if (!isVodMode) return
        vodCurrentOffsetMs = playerPositionMs
    }

    fun startVodChatPolling() {
        if (!isVodMode || vodChatroomId < 0) return
        vodPollingJob?.cancel()
        vodPollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val queryTimeMs = vodStartEpochMs + vodCurrentOffsetMs
                    val queryTimeStr = formatEpochAsIso(queryTimeMs)
                    Log.d(TAG, "VOD chat poll: chatroomId=$vodChatroomId time=$queryTimeStr")

                    val response = KickRepository.api.getVodMessages(vodChatroomId, queryTimeStr)

                    // Flexible parsing: try data.messages first, then root messages
                    val messages = response.data?.messages
                        ?: response.messages
                        ?: emptyList()

                    Log.d(TAG, "VOD chat: got ${messages.size} msgs (data=${response.data}, rootMsgs=${response.messages?.size})")

                    if (messages.isNotEmpty()) {
                        val updated = (_chatMessages.value + messages).takeLast(MAX_CHAT_MESSAGES)
                        _chatMessages.value = updated
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "VOD chat poll error: ${e.message}")
                }
                delay(VOD_POLL_INTERVAL_MS)
            }
        }
    }

    fun stopVodChatPolling() {
        vodPollingJob?.cancel()
        vodPollingJob = null
    }

    private fun connectChat(chatroomId: Int) {
        chatManager?.disconnect()
        chatManager = KickChatManager(
            chatroomId = chatroomId,
            onMessage = { message ->
                val delayMs = chatDelayMs
                if (delayMs <= 0L) {
                    val updated = (_chatMessages.value + message).takeLast(MAX_CHAT_MESSAGES)
                    _chatMessages.value = updated
                } else {
                    viewModelScope.launch {
                        delay(delayMs)
                        val updated = (_chatMessages.value + message).takeLast(MAX_CHAT_MESSAGES)
                        _chatMessages.value = updated
                    }
                }
            },
            onConnectionStateChange = { connected ->
                _chatConnected.value = connected
            }
        )
        chatManager?.connect()
    }

    private fun parseStartTime(startTime: String): Long {
        // Input format: "2026-04-18 21:54:14"
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            sdf.parse(startTime)?.time ?: 0L
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse startTime: $startTime — ${e.message}")
            0L
        }
    }

    private fun formatEpochAsIso(epochMs: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return sdf.format(Date(epochMs))
    }

    override fun onCleared() {
        super.onCleared()
        chatManager?.disconnect()
        vodPollingJob?.cancel()
    }

    companion object {
        private const val TAG = "KyckStreamTV"
        private const val MAX_CHAT_MESSAGES = 200
        private const val VOD_POLL_INTERVAL_MS = 5000L
    }
}

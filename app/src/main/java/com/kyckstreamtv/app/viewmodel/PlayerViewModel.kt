package com.kyckstreamtv.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyckstreamtv.app.api.KickRepository
import com.kyckstreamtv.app.chat.KickChatManager
import com.kyckstreamtv.app.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

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

                val isLive = livestream?.isLive ?: (channel.livestream != null)

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

    private fun connectChat(chatroomId: Int) {
        chatManager?.disconnect()
        chatManager = KickChatManager(
            chatroomId = chatroomId,
            onMessage = { message ->
                val updated = (_chatMessages.value + message).takeLast(MAX_CHAT_MESSAGES)
                _chatMessages.value = updated
            },
            onConnectionStateChange = { connected ->
                _chatConnected.value = connected
            }
        )
        chatManager?.connect()
    }

    override fun onCleared() {
        super.onCleared()
        chatManager?.disconnect()
    }

    companion object {
        private const val TAG = "KyckStreamTV"
        private const val MAX_CHAT_MESSAGES = 200
    }
}

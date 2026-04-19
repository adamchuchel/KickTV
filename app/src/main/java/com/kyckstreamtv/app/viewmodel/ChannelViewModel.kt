package com.kyckstreamtv.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyckstreamtv.app.api.KickRepository
import com.kyckstreamtv.app.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChannelDetails(
    val slug: String,
    val username: String,
    val profilePicUrl: String?,
    val isLive: Boolean,
    val viewerCount: Int,
    val sessionTitle: String?,
    val chatroomId: Int?,
    val playbackUrl: String?
)

sealed class ChannelState {
    object Loading : ChannelState()
    data class Ready(val details: ChannelDetails) : ChannelState()
    data class Error(val message: String) : ChannelState()
}

class ChannelViewModel : ViewModel() {

    private val _channelState = MutableStateFlow<ChannelState>(ChannelState.Loading)
    val channelState: StateFlow<ChannelState> = _channelState

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos

    fun load(slug: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _channelState.value = ChannelState.Loading
            try {
                val channelDeferred = async { KickRepository.api.getChannel(slug) }
                val videosDeferred = async {
                    try {
                        KickRepository.api.getChannelVideos(slug)
                    } catch (e: Exception) {
                        Log.w(TAG, "getChannelVideos failed: ${e.message}")
                        emptyList()
                    }
                }

                val channel = channelDeferred.await()
                val videoList = videosDeferred.await()

                val isLive = channel.livestream?.isLive ?: false
                val details = ChannelDetails(
                    slug = channel.slug,
                    username = channel.user.username,
                    profilePicUrl = channel.user.profilePic,
                    isLive = isLive,
                    viewerCount = channel.livestream?.viewerCount ?: 0,
                    sessionTitle = channel.livestream?.sessionTitle,
                    chatroomId = channel.chatroom?.channelId ?: channel.chatroom?.id,
                    playbackUrl = channel.playbackUrl
                )

                _channelState.value = ChannelState.Ready(details)
                _videos.value = videoList

                Log.d(TAG, "Channel loaded: ${channel.user.username} isLive=$isLive videos=${videoList.size} channelId=${channel.id} chatroomId=${channel.chatroom?.id} chatroomChannelId=${channel.chatroom?.channelId}")
            } catch (e: Exception) {
                Log.e(TAG, "load($slug) failed: ${e.message}")
                _channelState.value = ChannelState.Error(e.message ?: "Unknown error")
            }
        }
    }

    companion object {
        private const val TAG = "KyckStreamTV"
    }
}

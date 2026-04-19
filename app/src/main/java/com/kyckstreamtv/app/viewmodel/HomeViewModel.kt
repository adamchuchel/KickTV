package com.kyckstreamtv.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyckstreamtv.app.api.KickRepository
import com.kyckstreamtv.app.model.ChannelCard
import com.kyckstreamtv.app.model.ChannelResponse
import com.kyckstreamtv.app.model.FollowedChannelItem
import com.kyckstreamtv.app.model.FollowedChannelsResponse
import com.kyckstreamtv.app.model.LivestreamItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _liveFollowed = MutableStateFlow<List<ChannelCard>>(emptyList())
    val liveFollowed: StateFlow<List<ChannelCard>> = _liveFollowed

    private val _offlineFollowed = MutableStateFlow<List<ChannelCard>>(emptyList())
    val offlineFollowed: StateFlow<List<ChannelCard>> = _offlineFollowed

    private val _popular = MutableStateFlow<List<ChannelCard>>(emptyList())
    val popular: StateFlow<List<ChannelCard>> = _popular

    // Keep for backwards compatibility
    val followed: StateFlow<List<ChannelCard>> = _liveFollowed

    private var loaded = false

    fun load() {
        if (loaded) return
        loaded = true
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            val followedDeferred = async {
                // Try /api/v2/channels/followed first (returns live+offline)
                try {
                    val r = KickRepository.api.getFollowedChannels()
                    if ((r.channels?.isNotEmpty() == true) || (r.data?.isNotEmpty() == true)) return@async r
                    throw Exception("empty response")
                } catch (e: Exception) {
                    Log.w(TAG, "getFollowedChannels failed: ${e.message}, trying user/livestreams")
                }
                // Fallback: /api/v1/user/livestreams (only live channels)
                try {
                    val items = KickRepository.api.getUserLivestreams().data
                    FollowedChannelsResponse(
                        channels = items.map { it.toFollowedChannelItem() }
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "getUserLivestreams also failed: ${e.message}")
                    null
                }
            }

            val popularDeferred = async {
                try {
                    KickRepository.api.getLivestreams(limit = 20, sort = "desc").data
                } catch (e: Exception) {
                    Log.w(TAG, "getLivestreams failed: ${e.message}")
                    emptyList()
                }
            }

            val followedResponse = followedDeferred.await()
            val popularStreams = popularDeferred.await()

            val followedChannels: List<FollowedChannelItem> = followedResponse?.channels
                ?: followedResponse?.data?.map { it.toFollowedChannelItem() }
                ?: emptyList()

            val liveCards = mutableListOf<ChannelCard>()
            val offlineCards = mutableListOf<ChannelCard>()
            for (ch in followedChannels) {
                val card = followedChannelToCard(ch)
                if (ch.isLive) liveCards.add(card) else offlineCards.add(card)
            }

            _liveFollowed.value = liveCards
            _offlineFollowed.value = offlineCards

            // Popular streams
            val popularCards = popularStreams.map { livestreamItemToCard(it) }
            _popular.value = popularCards

            Log.d(TAG, "Loaded: live=${liveCards.size} offline=${offlineCards.size} popular=${popularCards.size}")
        }
    }

    private fun followedChannelToCard(ch: FollowedChannelItem) = ChannelCard(
        slug = ch.channelSlug,
        username = ch.userUsername,
        title = ch.sessionTitle ?: ch.userUsername,
        viewerCount = ch.viewerCount,
        thumbnailUrl = ch.bannerPicture,
        profilePicUrl = ch.profilePicture
    )

    private fun LivestreamItem.toFollowedChannelItem() = FollowedChannelItem(
        channelSlug = channel?.slug ?: slug,
        userUsername = channel?.user?.username ?: slug,
        sessionTitle = sessionTitle,
        isLive = true,
        viewerCount = viewerCount,
        profilePicture = channel?.user?.profilePic,
        bannerPicture = thumbnail?.url
    )

    private fun ChannelResponse.toFollowedChannelItem() = FollowedChannelItem(
        channelSlug = slug,
        userUsername = user.username,
        sessionTitle = livestream?.sessionTitle,
        isLive = livestream?.isLive ?: false,
        viewerCount = livestream?.viewerCount ?: 0,
        profilePicture = user.profilePic,
        bannerPicture = livestream?.thumbnail?.url
    )

    private fun livestreamItemToCard(item: LivestreamItem): ChannelCard {
        val slug = item.channel?.slug ?: item.slug
        val username = item.channel?.user?.username ?: slug
        return ChannelCard(
            slug = slug,
            username = username,
            title = item.sessionTitle ?: username,
            viewerCount = item.viewerCount,
            thumbnailUrl = item.thumbnail?.url,
            profilePicUrl = item.channel?.user?.profilePic
        )
    }

    companion object {
        private const val TAG = "KyckStreamTV"
    }
}

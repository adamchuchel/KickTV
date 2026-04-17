package com.kyckstreamtv.app

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kyckstreamtv.app.ui.ChatAdapter
import com.kyckstreamtv.app.viewmodel.PlayerState
import com.kyckstreamtv.app.viewmodel.PlayerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CHANNEL = "extra_channel"
    }

    private val viewModel: PlayerViewModel by viewModels()

    private lateinit var playerView: PlayerView
    private lateinit var chatContainer: LinearLayout
    private lateinit var rvChat: RecyclerView
    private lateinit var tvTitle: TextView
    private lateinit var tvStatus: TextView
    private lateinit var player: ExoPlayer
    private lateinit var chatAdapter: ChatAdapter

    private var chatVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        val channelName = intent.getStringExtra(EXTRA_CHANNEL) ?: run {
            finish()
            return
        }

        playerView = findViewById(R.id.player_view)
        chatContainer = findViewById(R.id.chat_container)
        rvChat = findViewById(R.id.rv_chat)
        tvTitle = findViewById(R.id.tv_stream_title)
        tvStatus = findViewById(R.id.tv_status)

        setupPlayer()
        setupChat()
        observeViewModel()

        viewModel.loadChannel(channelName)
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        playerView.useController = false

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                tvStatus.text = "Playback error: ${error.message}"
                tvStatus.visibility = View.VISIBLE
            }
        })
    }

    private fun setupChat() {
        chatAdapter = ChatAdapter()
        rvChat.adapter = chatAdapter
        rvChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.playerState.collectLatest { state ->
                when (state) {
                    is PlayerState.Loading -> {
                        tvStatus.text = getString(R.string.msg_loading)
                        tvStatus.visibility = View.VISIBLE
                    }
                    is PlayerState.Ready -> {
                        tvStatus.visibility = View.GONE
                        tvTitle.text = buildString {
                            append(state.title)
                            if (state.viewerCount > 0) append("  •  ${formatViewers(state.viewerCount)}")
                        }
                        playStream(state.streamUrl)
                    }
                    is PlayerState.Offline -> {
                        tvStatus.text = getString(R.string.msg_channel_offline)
                        tvStatus.visibility = View.VISIBLE
                    }
                    is PlayerState.Error -> {
                        tvStatus.text = state.message
                        tvStatus.visibility = View.VISIBLE
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.chatMessages.collectLatest { messages ->
                val prevCount = chatAdapter.itemCount
                chatAdapter.setMessages(messages)
                if (messages.isNotEmpty() && messages.size > prevCount) {
                    rvChat.scrollToPosition(messages.size - 1)
                }
            }
        }
    }

    private fun playStream(url: String) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }

    private fun formatViewers(count: Int): String {
        return when {
            count >= 1000 -> "${count / 1000}.${(count % 1000) / 100}K viewers"
            else -> "$count viewers"
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_PROG_YELLOW -> {
                toggleChat()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun toggleChat() {
        chatVisible = !chatVisible
        chatContainer.visibility = if (chatVisible) View.VISIBLE else View.GONE
    }

    override fun onPause() {
        super.onPause()
        player.pause()
    }

    override fun onResume() {
        super.onResume()
        if (player.playbackState == Player.STATE_READY) {
            player.play()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}

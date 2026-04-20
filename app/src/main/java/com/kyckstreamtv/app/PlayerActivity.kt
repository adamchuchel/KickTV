package com.kyckstreamtv.app

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kyckstreamtv.app.ui.ChatAdapter
import com.kyckstreamtv.app.viewmodel.PlayerState
import com.kyckstreamtv.app.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CHANNEL = "extra_channel"
        const val EXTRA_VOD_URL = "extra_vod_url"
        const val EXTRA_VOD_START_TIME = "extra_vod_start_time"
        const val EXTRA_CHATROOM_ID = "extra_chatroom_id"
        const val EXTRA_TITLE = "extra_title"
    }

    private val viewModel: PlayerViewModel by viewModels()
    private lateinit var prefs: PlayerPrefs
    private var isVodMode = false
    private var vodPositionJob: kotlinx.coroutines.Job? = null

    private lateinit var playerView: PlayerView
    private lateinit var chatContainer: View
    private lateinit var rvChat: RecyclerView
    private lateinit var progressBar: View
    private lateinit var tvStatus: TextView
    private lateinit var bottomBar: View
    private lateinit var tvChannelName: TextView
    private lateinit var tvGameName: TextView
    private lateinit var tvViewers: TextView

    // Settings chips
    private lateinit var chipChat: LinearLayout
    private lateinit var chipOpacity: LinearLayout
    private lateinit var chipWidth: LinearLayout
    private lateinit var chipHeight: LinearLayout
    private lateinit var chipFont: LinearLayout
    private lateinit var chipHPos: LinearLayout
    private lateinit var chipVPos: LinearLayout
    private lateinit var chipQuality: LinearLayout
    private lateinit var chipDelay: LinearLayout

    private lateinit var valChat: TextView
    private lateinit var valOpacity: TextView
    private lateinit var valWidth: TextView
    private lateinit var valHeight: TextView
    private lateinit var valFont: TextView
    private lateinit var valHPos: TextView
    private lateinit var valVPos: TextView
    private lateinit var valQuality: TextView
    private lateinit var valDelay: TextView

    private lateinit var player: ExoPlayer
    private lateinit var chatAdapter: ChatAdapter
    private var chatVisible = true
    private var bottomBarVisible = false

    // VOD seek overlay
    private lateinit var seekOverlay: View
    private lateinit var tvSeekDirection: TextView
    private lateinit var tvSeekPosition: TextView
    private val seekHideHandler = Handler(Looper.getMainLooper())
    private val seekHideRunnable = Runnable { seekOverlay.visibility = View.GONE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        setContentView(R.layout.activity_player)

        val vodUrl = intent.getStringExtra(EXTRA_VOD_URL)
        isVodMode = vodUrl != null

        val channelName = if (!isVodMode) {
            intent.getStringExtra(EXTRA_CHANNEL) ?: run { finish(); return }
        } else ""

        prefs = PlayerPrefs(this)

        playerView     = findViewById(R.id.player_view)
        chatContainer  = findViewById(R.id.chat_container)
        rvChat         = findViewById(R.id.rv_chat)
        progressBar    = findViewById(R.id.progress_bar)
        tvStatus       = findViewById(R.id.tv_status)
        bottomBar      = findViewById(R.id.bottom_bar)
        tvChannelName  = findViewById(R.id.tv_channel_name)
        tvGameName     = findViewById(R.id.tv_game_name)
        tvViewers      = findViewById(R.id.tv_viewers)
        seekOverlay    = findViewById(R.id.seek_overlay)
        tvSeekDirection= findViewById(R.id.tv_seek_direction)
        tvSeekPosition = findViewById(R.id.tv_seek_position)

        chipChat    = findViewById(R.id.chip_chat)
        chipOpacity = findViewById(R.id.chip_opacity)
        chipWidth   = findViewById(R.id.chip_width)
        chipHeight  = findViewById(R.id.chip_height)
        chipFont    = findViewById(R.id.chip_font)
        chipHPos    = findViewById(R.id.chip_hpos)
        chipVPos    = findViewById(R.id.chip_vpos)
        chipQuality = findViewById(R.id.chip_quality)
        chipDelay   = findViewById(R.id.chip_delay)

        valChat    = findViewById(R.id.val_chat)
        valOpacity = findViewById(R.id.val_opacity)
        valWidth   = findViewById(R.id.val_width)
        valHeight  = findViewById(R.id.val_height)
        valFont    = findViewById(R.id.val_font)
        valHPos    = findViewById(R.id.val_hpos)
        valVPos    = findViewById(R.id.val_vpos)
        valQuality = findViewById(R.id.val_quality)
        valDelay   = findViewById(R.id.val_delay)

        setupPlayer()
        setupChat()
        setupChips()
        applyPrefs()
        observeViewModel()

        if (isVodMode) {
            val url = intent.getStringExtra(EXTRA_VOD_URL)!!
            val startTime = intent.getStringExtra(EXTRA_VOD_START_TIME) ?: ""
            val chatroomId = intent.getIntExtra(EXTRA_CHATROOM_ID, -1)
            val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
            viewModel.loadVod(url, startTime, chatroomId, title)
        } else {
            // Show delay chip only for live
            chipDelay.visibility = View.VISIBLE
            viewModel.chatDelayMs = prefs.chatDelaySeconds * 1000L
            viewModel.loadChannel(channelName)
        }
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        playerView.useController = false

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                showStatusText("Chyba přehrávání: ${error.message}")
            }
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> progressBar.visibility = View.VISIBLE
                    Player.STATE_READY     -> progressBar.visibility = View.GONE
                    else -> {}
                }
            }
        })
    }

    private fun setupChat() {
        chatAdapter = ChatAdapter()
        rvChat.adapter = chatAdapter
        rvChat.itemAnimator = null
        rvChat.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
    }

    // ── Settings chips ──────────────────────────────────────────────────────

    private fun setupChips() {
        attachChipKeys(chipChat,
            onUp   = { toggleChat() },
            onDown = { toggleChat() },
            onSelect = { toggleChat() }
        )
        attachChipKeys(chipOpacity,
            onUp   = { prefs.chatOpacityProgress = (prefs.chatOpacityProgress + 5).coerceAtMost(100); applyChatBackground(); refreshChipValues() },
            onDown = { prefs.chatOpacityProgress = (prefs.chatOpacityProgress - 5).coerceAtLeast(0);  applyChatBackground(); refreshChipValues() }
        )
        attachChipKeys(chipWidth,
            onUp   = { prefs.chatWidthProgress = (prefs.chatWidthProgress + 1).coerceAtMost(35); applyChatLayout(); refreshChipValues() },
            onDown = { prefs.chatWidthProgress = (prefs.chatWidthProgress - 1).coerceAtLeast(0); applyChatLayout(); refreshChipValues() }
        )
        attachChipKeys(chipHeight,
            onUp   = { prefs.chatHeightProgress = (prefs.chatHeightProgress + 2).coerceAtMost(90); applyChatLayout(); refreshChipValues() },
            onDown = { prefs.chatHeightProgress = (prefs.chatHeightProgress - 2).coerceAtLeast(0); applyChatLayout(); refreshChipValues() }
        )
        attachChipKeys(chipFont,
            onUp   = { prefs.chatFontProgress = (prefs.chatFontProgress + 1).coerceAtMost(10); chatAdapter.setFontSize(prefs.chatFontSizeSp); refreshChipValues() },
            onDown = { prefs.chatFontProgress = (prefs.chatFontProgress - 1).coerceAtLeast(0); chatAdapter.setFontSize(prefs.chatFontSizeSp); refreshChipValues() }
        )
        attachChipKeys(chipHPos,
            onUp   = { prefs.chatHPos = (prefs.chatHPos + 1) % 3; applyChatLayout(); refreshChipValues() },
            onDown = { prefs.chatHPos = (prefs.chatHPos + 2) % 3; applyChatLayout(); refreshChipValues() }
        )
        attachChipKeys(chipVPos,
            onUp   = { prefs.chatVPos = (prefs.chatVPos + 2) % 3; applyChatLayout(); refreshChipValues() },
            onDown = { prefs.chatVPos = (prefs.chatVPos + 1) % 3; applyChatLayout(); refreshChipValues() }
        )
        attachChipKeys(chipQuality,
            onUp = {}, onDown = {},
            onSelect = { showQualityDialog() }
        )
        attachChipKeys(chipDelay,
            onUp   = { prefs.chatDelaySeconds = (prefs.chatDelaySeconds + 1).coerceAtMost(30); viewModel.chatDelayMs = prefs.chatDelaySeconds * 1000L; refreshChipValues() },
            onDown = { prefs.chatDelaySeconds = (prefs.chatDelaySeconds - 1).coerceAtLeast(0); viewModel.chatDelayMs = prefs.chatDelaySeconds * 1000L; refreshChipValues() }
        )
    }

    private fun attachChipKeys(chip: LinearLayout, onUp: () -> Unit, onDown: () -> Unit, onSelect: (() -> Unit)? = null) {
        chip.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP     -> { onUp(); true }
                KeyEvent.KEYCODE_DPAD_DOWN   -> { onDown(); true }
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER       -> { onSelect?.invoke(); true }
                else -> false
            }
        }
        chip.setOnClickListener { onSelect?.invoke() }
    }

    private fun applyPrefs() {
        viewModel.chatDelayMs = prefs.chatDelaySeconds * 1000L
        refreshChipValues()
        applyChatBackground()
        chatAdapter.setFontSize(prefs.chatFontSizeSp)
        chatContainer.post { applyChatLayout() }
    }

    private fun refreshChipValues() {
        valChat.text    = if (chatVisible) "ON" else "OFF"
        valChat.setTextColor(if (chatVisible) getColor(R.color.kick_green) else getColor(R.color.text_secondary))
        valOpacity.text = "${prefs.chatAlpha.times(100).toInt()}%"
        valWidth.text   = "${prefs.chatWidthPercent}%"
        valHeight.text  = "${prefs.chatHeightPercent}%"
        valFont.text    = "${prefs.chatFontSizeSp.toInt()}sp"
        valHPos.text    = when (prefs.chatHPos) { 0 -> "← Vlevo"; 1 -> "— Střed"; else -> "→ Vpravo" }
        valVPos.text    = when (prefs.chatVPos) { 0 -> "↑ Nahoře"; 1 -> "↕ Střed"; else -> "↓ Dole" }
        valDelay.text   = if (prefs.chatDelaySeconds == 0) "0s (vypnuto)" else "${prefs.chatDelaySeconds}s"
    }

    private fun applyChatBackground() {
        val bgAlpha = (prefs.chatAlpha * 255).toInt()
        chatContainer.setBackgroundColor(Color.argb(bgAlpha, 0, 0, 0))
    }

    private fun applyChatLayout() {
        val screenWidth  = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val params = chatContainer.layoutParams as RelativeLayout.LayoutParams
        params.width  = screenWidth  * prefs.chatWidthPercent  / 100
        params.height = screenHeight * prefs.chatHeightPercent / 100
        params.removeRule(RelativeLayout.ALIGN_PARENT_START)
        params.removeRule(RelativeLayout.ALIGN_PARENT_END)
        params.removeRule(RelativeLayout.CENTER_HORIZONTAL)
        params.removeRule(RelativeLayout.ALIGN_PARENT_TOP)
        params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        params.removeRule(RelativeLayout.CENTER_VERTICAL)
        when (prefs.chatHPos) {
            0    -> params.addRule(RelativeLayout.ALIGN_PARENT_START)
            1    -> params.addRule(RelativeLayout.CENTER_HORIZONTAL)
            else -> params.addRule(RelativeLayout.ALIGN_PARENT_END)
        }
        when (prefs.chatVPos) {
            0    -> params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
            1    -> params.addRule(RelativeLayout.CENTER_VERTICAL)
            else -> params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        }
        chatContainer.layoutParams = params
    }

    // ── ViewModel observation ────────────────────────────────────────────────

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.playerState.collect { state ->
                when (state) {
                    is PlayerState.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        tvStatus.visibility = View.GONE
                    }
                    is PlayerState.Ready -> {
                        progressBar.visibility = View.GONE
                        tvStatus.visibility = View.GONE
                        tvChannelName.text = state.title.substringBefore("  •")
                        tvViewers.text = if (isVodMode) "" else formatViewers(state.viewerCount)
                        playStream(state.streamUrl)
                        if (isVodMode) {
                            viewModel.startVodChatPolling()
                            startVodPositionTracking()
                        }
                    }
                    is PlayerState.Offline -> showStatusText(getString(R.string.msg_channel_offline))
                    is PlayerState.Error   -> showStatusText(state.message)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.chatMessages.collect { messages ->
                chatAdapter.setMessages(messages)
                if (messages.isNotEmpty()) rvChat.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun playStream(url: String) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }

    private fun startVodPositionTracking() {
        vodPositionJob?.cancel()
        vodPositionJob = lifecycleScope.launch {
            while (true) {
                delay(1000L)
                viewModel.updateVodPosition(player.currentPosition)
            }
        }
    }

    private fun showStatusText(msg: String) {
        progressBar.visibility = View.GONE
        tvStatus.text = msg
        tvStatus.visibility = View.VISIBLE
    }

    private fun toggleChat() {
        chatVisible = !chatVisible
        chatContainer.visibility = if (chatVisible) View.VISIBLE else View.GONE
        refreshChipValues()
    }

    private fun showQualityDialog() {
        val tracks = player.currentTracks
        val videoGroup = tracks.groups.firstOrNull {
            it.type == C.TRACK_TYPE_VIDEO && it.isSupported
        } ?: run {
            AlertDialog.Builder(this).setTitle("Kvalita")
                .setMessage("Žádné stopy k dispozici").setPositiveButton("OK", null).show()
            return
        }

        val labels = mutableListOf("Auto (adaptivní)")
        val overrides = mutableListOf<TrackSelectionOverride?>(null)
        for (i in 0 until videoGroup.length) {
            val fmt = videoGroup.getTrackFormat(i)
            labels.add(if (fmt.height > 0) "${fmt.height}p" else if (fmt.bitrate > 0) "${fmt.bitrate / 1000} kbps" else "Stopa $i")
            overrides.add(TrackSelectionOverride(videoGroup.mediaTrackGroup, i))
        }

        AlertDialog.Builder(this).setTitle("Kvalita streamu")
            .setItems(labels.toTypedArray()) { _, which ->
                val override = overrides[which]
                player.trackSelectionParameters = if (override == null) {
                    player.trackSelectionParameters.buildUpon()
                        .clearVideoSizeConstraints().clearOverridesOfType(C.TRACK_TYPE_VIDEO).build()
                } else {
                    player.trackSelectionParameters.buildUpon().setOverrideForType(override).build()
                }
                valQuality.text = labels[which].substringBefore(" (")
            }.show()
    }

    // ── Key handling ─────────────────────────────────────────────────────────

    private fun seekStepMs(repeatCount: Int): Long = when {
        repeatCount == 0  -> 10_000L
        repeatCount <= 8  -> 30_000L
        repeatCount <= 20 -> 120_000L
        else              -> 600_000L
    }

    private fun seekVideo(deltaMs: Long) {
        val newPos = (player.currentPosition + deltaMs).coerceIn(0, player.duration)
        player.seekTo(newPos)
        tvSeekDirection.text = if (deltaMs > 0) "⏩" else "⏪"
        tvSeekPosition.text = formatMs(newPos)
        seekOverlay.visibility = View.VISIBLE
        seekHideHandler.removeCallbacks(seekHideRunnable)
        seekHideHandler.postDelayed(seekHideRunnable, 1000)
    }

    private fun formatMs(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600; val m = (totalSec % 3600) / 60; val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (isVodMode && !bottomBarVisible) { seekVideo(seekStepMs(event?.repeatCount ?: 0)); true }
                else super.onKeyDown(keyCode, event)
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (isVodMode && !bottomBarVisible) { seekVideo(-seekStepMs(event?.repeatCount ?: 0)); true }
                else super.onKeyDown(keyCode, event)
            }
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_PROG_YELLOW,
            KeyEvent.KEYCODE_SETTINGS, KeyEvent.KEYCODE_INFO, 176 -> {
                toggleBottomBar(); true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (!bottomBarVisible) { showBottomBar(); true }
                else super.onKeyDown(keyCode, event)
            }
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                if (bottomBarVisible) { hideBottomBar(); true }
                else super.onKeyDown(keyCode, event)
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun toggleBottomBar() { if (bottomBarVisible) hideBottomBar() else showBottomBar() }

    private fun showBottomBar() {
        bottomBarVisible = true
        bottomBar.visibility = View.VISIBLE
        chipChat.requestFocus()
    }

    private fun hideBottomBar() {
        bottomBarVisible = false
        bottomBar.visibility = View.GONE
        playerView.requestFocus()
    }

    private fun formatViewers(count: Int): String = when {
        count >= 1000 -> "${count / 1000}.${(count % 1000) / 100}K diváků"
        else -> "$count diváků"
    }

    override fun onPause()   { super.onPause(); player.pause() }
    override fun onResume()  { super.onResume(); if (player.playbackState == Player.STATE_READY) player.play() }
    override fun onDestroy() {
        super.onDestroy()
        vodPositionJob?.cancel()
        seekHideHandler.removeCallbacks(seekHideRunnable)
        player.release()
    }
}

package com.kyckstreamtv.app

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.kyckstreamtv.app.model.VideoItem
import com.kyckstreamtv.app.ui.VideoCardPresenter
import com.kyckstreamtv.app.viewmodel.ChannelDetails
import com.kyckstreamtv.app.viewmodel.ChannelState
import com.kyckstreamtv.app.viewmodel.ChannelViewModel
import kotlinx.coroutines.launch

class ChannelFragment : Fragment() {

    companion object {
        private const val ARG_SLUG = "arg_slug"

        fun newInstance(slug: String) = ChannelFragment().apply {
            arguments = Bundle().also { it.putString(ARG_SLUG, slug) }
        }
    }

    private val viewModel: ChannelViewModel by viewModels()
    private lateinit var slug: String

    private lateinit var tvChannelName: TextView
    private lateinit var tvLiveDot: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvSessionTitle: TextView
    private lateinit var ivProfile: ImageView
    private lateinit var btnWatchLive: LinearLayout
    private lateinit var btnFavorite: LinearLayout
    private lateinit var tvFavoriteIcon: TextView

    private lateinit var rowsAdapter: ArrayObjectAdapter
    private var videosRowAdapter: ArrayObjectAdapter? = null
    private var rowsFragment: RowsSupportFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        slug = arguments?.getString(ARG_SLUG) ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_channel, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvChannelName = view.findViewById(R.id.tv_channel_name)
        tvLiveDot = view.findViewById(R.id.tv_live_dot)
        tvStatus = view.findViewById(R.id.tv_status)
        tvSessionTitle = view.findViewById(R.id.tv_session_title)
        ivProfile = view.findViewById(R.id.iv_profile)
        btnWatchLive = view.findViewById(R.id.btn_watch_live)
        btnFavorite = view.findViewById(R.id.btn_favorite)
        tvFavoriteIcon = view.findViewById(R.id.tv_favorite_icon)

        btnWatchLive.setOnClickListener { openPlayer() }
        btnFavorite.setOnClickListener { toggleFavorite() }
        btnWatchLive.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event.action == KeyEvent.ACTION_DOWN) {
                focusRows(); true
            } else false
        }

        setupRowsFragment()
        observeViewModel()

        if (slug.isNotBlank()) {
            viewModel.load(slug)
        }
    }

    private fun setupRowsFragment() {
        rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

        val fragment = RowsSupportFragment()
        fragment.adapter = rowsAdapter
        fragment.onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            if (item is VideoItem) openVodPlayer(item)
        }
        rowsFragment = fragment

        childFragmentManager.beginTransaction()
            .replace(R.id.rows_container, fragment)
            .commitNow()

    }

    private fun focusRows() {
        val target = rowsFragment?.view ?: view?.findViewById(R.id.rows_container) ?: return
        // Find first focusable descendant inside RowsSupportFragment
        target.post {
            val focusable = target.focusSearch(View.FOCUS_DOWN) ?: target
            focusable.requestFocus()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.channelState.collect { state ->
                when (state) {
                    is ChannelState.Loading -> { /* nothing yet */ }
                    is ChannelState.Ready -> updateHeader(state.details)
                    is ChannelState.Error -> tvStatus.text = "Chyba načítání"
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.videos.collect { videos ->
                if (videos.isNotEmpty()) showVideos(videos)
            }
        }
    }

    private fun updateFavoriteButton() {
        val isFav = FavoritesRepository.isFavorite(slug)
        tvFavoriteIcon.text = if (isFav) "★" else "☆"
        tvFavoriteIcon.setTextColor(
            if (isFav) requireContext().getColor(R.color.kick_green)
            else requireContext().getColor(R.color.text_primary)
        )
    }

    private fun toggleFavorite() {
        val details = (viewModel.channelState.value as? ChannelState.Ready)?.details ?: return
        if (FavoritesRepository.isFavorite(slug)) {
            FavoritesRepository.remove(slug)
        } else {
            FavoritesRepository.add(FavoriteChannel(
                slug = details.slug,
                username = details.username,
                profilePicUrl = details.profilePicUrl
            ))
        }
        updateFavoriteButton()
    }

    private fun updateHeader(details: ChannelDetails) {
        tvChannelName.text = details.username
        updateFavoriteButton()

        if (details.isLive) {
            tvLiveDot.visibility = View.VISIBLE
            val viewersText = formatViewers(details.viewerCount)
            tvStatus.text = "ŽIVĚ • $viewersText diváků"
            if (!details.sessionTitle.isNullOrBlank()) {
                tvSessionTitle.text = details.sessionTitle
                tvSessionTitle.visibility = View.VISIBLE
            }
            btnWatchLive.visibility = View.VISIBLE
        } else {
            tvLiveDot.visibility = View.GONE
            tvStatus.text = "Offline"
            btnWatchLive.visibility = View.GONE
        }

        if (!details.profilePicUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(details.profilePicUrl)
                .transform(CircleCrop())
                .into(object : CustomTarget<Drawable>() {
                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                        ivProfile.setImageDrawable(resource)
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        }
    }

    private fun showVideos(videos: List<VideoItem>) {
        val adapter = videosRowAdapter ?: ArrayObjectAdapter(VideoCardPresenter()).also {
            videosRowAdapter = it
            val header = HeaderItem(0L, getString(R.string.row_videos))
            rowsAdapter.add(ListRow(header, it))
        }
        adapter.clear()
        adapter.addAll(0, videos)

        // Auto-focus rows if header has no focusable button (offline channel)
        if (btnWatchLive.visibility != View.VISIBLE) {
            view?.post { focusRows() }
        }
    }

    private fun openPlayer() {
        val details = (viewModel.channelState.value as? ChannelState.Ready)?.details ?: return
        startActivity(Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_CHANNEL, details.slug)
        })
    }

    private fun openVodPlayer(video: VideoItem) {
        val url = video.source ?: return
        val details = (viewModel.channelState.value as? ChannelState.Ready)?.details
        startActivity(Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_VOD_URL, url)
            putExtra(PlayerActivity.EXTRA_VOD_START_TIME, video.startTime ?: "")
            putExtra(PlayerActivity.EXTRA_TITLE, video.sessionTitle ?: slug)
            if (details?.chatroomId != null) {
                putExtra(PlayerActivity.EXTRA_CHATROOM_ID, details.chatroomId)
            }
        })
    }

    private fun formatViewers(count: Int) = when {
        count >= 1000 -> "${count / 1000}.${(count % 1000) / 100}K"
        else -> "$count"
    }
}

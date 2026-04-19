package com.kyckstreamtv.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.lifecycle.lifecycleScope
import com.kyckstreamtv.app.model.ChannelCard
import com.kyckstreamtv.app.ui.ChannelCardPresenter
import com.kyckstreamtv.app.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

class HomeFragment : BrowseSupportFragment() {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var rowsAdapter: ArrayObjectAdapter

    // Track rows by section index
    private val rowIndexMap = mutableMapOf<Int, Int>() // sectionIndex -> rowsAdapter index

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        adapter = rowsAdapter
        title = getString(R.string.app_name)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        brandColor = requireContext().getColor(R.color.surface)
        searchAffordanceColor = requireContext().getColor(R.color.kick_green)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setOnSearchClickedListener {
            startActivity(Intent(requireContext(), MainActivity::class.java))
        }

        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            if (item is ChannelCard) {
                startActivity(Intent(requireContext(), ChannelActivity::class.java).apply {
                    putExtra(ChannelActivity.EXTRA_SLUG, item.slug)
                })
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.liveFollowed.collect { cards ->
                updateRow(0, getString(R.string.row_live_following), cards)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.offlineFollowed.collect { cards ->
                updateRow(1, getString(R.string.row_offline_following), cards)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.popular.collect { cards ->
                updateRow(2, getString(R.string.row_popular), cards)
            }
        }

        viewModel.load()
    }

    private fun updateRow(sectionIndex: Int, title: String, cards: List<ChannelCard>) {
        if (cards.isEmpty()) return
        val cardAdapter = ArrayObjectAdapter(ChannelCardPresenter())
        cardAdapter.addAll(0, cards)
        val row = ListRow(HeaderItem(sectionIndex.toLong(), title), cardAdapter)

        val existingIndex = rowIndexMap[sectionIndex]
        if (existingIndex != null && existingIndex < rowsAdapter.size()) {
            rowsAdapter.replace(existingIndex, row)
        } else {
            // Find correct insertion position (keep rows in order 0,1,2)
            var insertAt = 0
            for ((sec, idx) in rowIndexMap) {
                if (sec < sectionIndex) insertAt = idx + 1
            }
            // Shift existing indices
            for (sec in rowIndexMap.keys) {
                val idx = rowIndexMap[sec]!!
                if (idx >= insertAt) rowIndexMap[sec] = idx + 1
            }
            rowIndexMap[sectionIndex] = insertAt
            rowsAdapter.add(insertAt, row)
        }
    }
}

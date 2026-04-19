package com.kyckstreamtv.app

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

class ChannelActivity : FragmentActivity() {

    companion object {
        const val EXTRA_SLUG = "extra_slug"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channel)

        val slug = intent.getStringExtra(EXTRA_SLUG) ?: run { finish(); return }

        if (savedInstanceState == null) {
            val fragment = ChannelFragment.newInstance(slug)
            supportFragmentManager.beginTransaction()
                .replace(R.id.channel_fragment_container, fragment)
                .commit()
        }
    }
}

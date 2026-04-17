package com.kyckstreamtv.app

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var authManager: KickAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = (application as KyckStreamApp).authManager

        // Redirect to login if not authenticated
        if (!authManager.isAuthenticated()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val etChannel = findViewById<EditText>(R.id.et_channel_name)
        val btnWatch = findViewById<Button>(R.id.btn_watch)

        btnWatch.setOnClickListener {
            openChannel(etChannel.text.toString().trim().lowercase())
        }

        etChannel.setOnEditorActionListener { _, _, _ ->
            openChannel(etChannel.text.toString().trim().lowercase())
            true
        }

        etChannel.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                openChannel(etChannel.text.toString().trim().lowercase())
                true
            } else {
                false
            }
        }

        etChannel.requestFocus()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, MENU_LOGOUT, 0, "Log out")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == MENU_LOGOUT) {
            authManager.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openChannel(channelName: String) {
        if (channelName.isBlank()) {
            Toast.makeText(this, "Enter a channel name", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_CHANNEL, channelName)
        })
    }

    companion object {
        private const val MENU_LOGOUT = 1
    }
}

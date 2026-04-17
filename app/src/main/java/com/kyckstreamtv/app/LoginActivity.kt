package com.kyckstreamtv.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var authManager: KickAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        authManager = (application as KyckStreamApp).authManager
        webView = findViewById(R.id.webview_login)
        progressBar = findViewById(R.id.progress_login)

        // If already authenticated skip login
        if (authManager.isAuthenticated()) {
            goToMain()
            return
        }

        setupWebView()
        webView.loadUrl(authManager.buildAuthUrl())
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                // Intercept the custom-scheme redirect from the GitHub Pages callback
                if (url.startsWith(Config.KICK_REDIRECT_SCHEME)) {
                    val code = request.url.getQueryParameter("code")
                    val error = request.url.getQueryParameter("error")
                    when {
                        code != null -> handleAuthCode(code)
                        error != null -> showError("Login error: $error")
                    }
                    return true
                }
                return false
            }

            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun handleAuthCode(code: String) {
        progressBar.visibility = View.VISIBLE
        webView.visibility = View.GONE
        lifecycleScope.launch {
            val ok = authManager.exchangeCode(code)
            if (ok) {
                goToMain()
            } else {
                showError("Token exchange failed. Check your Client ID in Config.kt.")
                webView.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                webView.loadUrl(authManager.buildAuthUrl())
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}

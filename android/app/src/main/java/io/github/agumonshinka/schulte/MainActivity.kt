package io.github.agumonshinka.schulte

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewAssetLoader

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var store: StatsStore
    private var pendingStartGame = false
    private var pageReady = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = Color.parseColor("#0F766E")
        window.navigationBarColor = Color.parseColor("#0F172A")
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        store = StatsStore(this)
        pendingStartGame = intent.getBooleanExtra(EXTRA_START_GAME, false) ||
            intent.action == ACTION_START_GAME

        webView = findViewById(R.id.webview)
        setupWebView()
        webView.loadUrl(ASSET_URL)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingStartGame = intent.getBooleanExtra(EXTRA_START_GAME, false) ||
            intent.action == ACTION_START_GAME
        if (pendingStartGame && pageReady) {
            triggerStartGame()
        }
    }

    fun onStatsUpdated(@Suppress("UNUSED_PARAMETER") snap: StatsStore.Snapshot) {
        // Hook for future in-app UI; widget already refreshed by bridge.
    }

    private fun setupWebView() {
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.setBackgroundColor(Color.parseColor("#0F172A"))
        webView.overScrollMode = View.OVER_SCROLL_NEVER

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

        webView.addJavascriptInterface(AndroidBridge(this, store), "AndroidBridge")

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                pageReady = true
                // Ask page to push current stats; also auto-start if launched from widget.
                view?.evaluateJavascript(
                    "(function(){ try { if (window.SchulteApp && window.SchulteApp.pushStatsToAndroid) window.SchulteApp.pushStatsToAndroid(); } catch(e){} })();",
                    null
                )
                if (pendingStartGame) {
                    triggerStartGame()
                }
            }
        }
    }

    private fun triggerStartGame() {
        pendingStartGame = false
        webView.evaluateJavascript(
            "(function(){ try { if (window.SchulteApp && window.SchulteApp.startNewGame) { window.SchulteApp.startNewGame(); } } catch(e){} })();",
            null
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (this::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    companion object {
        const val EXTRA_START_GAME = "start_game"
        const val ACTION_START_GAME = "io.github.agumonshinka.schulte.START_GAME"
        private const val ASSET_URL =
            "https://appassets.androidplatform.net/assets/www/index.html"
    }
}

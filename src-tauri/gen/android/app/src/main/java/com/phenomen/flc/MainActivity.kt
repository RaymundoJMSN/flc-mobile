package com.phenomen.flc

import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : TauriActivity() {
  private var wv: WebView? = null
  private val updater by lazy { Updater(this) }

  override fun onWebViewCreate(webView: WebView) {
    wv = webView
    webView.settings.apply {
      // app shell is https://tauri.localhost, most Foundry servers on a LAN are plain http
      mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
      // Foundry ambient audio/video should not wait for a tap
      mediaPlaybackRequiresUserGesture = false
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    WindowCompat.setDecorFitsSystemWindows(window, false)
    hideSystemBars()

    // back leaves the Foundry page and returns to the server list; at the list it exits
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        val webView = wv
        if (webView != null && webView.canGoBack()) webView.goBack() else finish()
      }
    })

    updater.checkInBackground()
  }

  override fun onResume() {
    super.onResume()
    // picks up an update that was waiting for the "install unknown apps" grant
    updater.retryPendingInstall()
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) hideSystemBars()
  }

  private fun hideSystemBars() {
    WindowInsetsControllerCompat(window, window.decorView).apply {
      systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      hide(WindowInsetsCompat.Type.systemBars())
    }
  }
}

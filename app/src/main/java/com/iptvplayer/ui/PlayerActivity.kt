package com.iptvplayer.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.iptvplayer.R

class PlayerActivity : Activity() {

    private lateinit var player: ExoPlayer
    private lateinit var primaryUrl: String
    private var fallbackUrl: String? = null
    private var isLive = true
    private var triedFallback = false
    private var wasPlaying = false

    private lateinit var loadingBox: View
    private lateinit var errorBox: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_player)

        primaryUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        fallbackUrl = intent.getStringExtra(EXTRA_FALLBACK)
        isLive = intent.getBooleanExtra(EXTRA_LIVE, true)

        loadingBox = findViewById(R.id.loadingBox)
        errorBox = findViewById(R.id.errorBox)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        findViewById<TextView>(R.id.playerTitle).text = title
        findViewById<TextView>(R.id.playerSubtitle).setText(
            if (isLive) R.string.player_live else R.string.player_movie
        )

        findViewById<View>(R.id.retryButton).setOnClickListener {
            startPlayback(primaryUrl, resetFallback = true)
        }
        findViewById<View>(R.id.quitButton).setOnClickListener { finish() }

        player = ExoPlayer.Builder(this).build()
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            true,
        )
        player.setVideoSurfaceView(findViewById<SurfaceView>(R.id.surface))
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        if (errorBox.visibility != View.VISIBLE) {
                            loadingBox.visibility = View.VISIBLE
                        }
                    }
                    Player.STATE_READY -> {
                        loadingBox.visibility = View.GONE
                        errorBox.visibility = View.GONE
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Lecture impossible (${error.errorCodeName})", error)
                val fallback = fallbackUrl
                if (!triedFallback && !fallback.isNullOrBlank()) {
                    triedFallback = true
                    startPlayback(fallback, resetFallback = false)
                } else {
                    showPlaybackError()
                }
            }
        })

        hideSystemUi()
        startPlayback(primaryUrl, resetFallback = true)
    }

    private fun startPlayback(url: String, resetFallback: Boolean) {
        if (url.isBlank()) {
            showPlaybackError()
            return
        }
        if (resetFallback) triedFallback = false
        errorBox.visibility = View.GONE
        loadingBox.visibility = View.VISIBLE
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }

    private fun showPlaybackError() {
        loadingBox.visibility = View.GONE
        errorBox.visibility = View.VISIBLE
        findViewById<View>(R.id.retryButton).requestFocus()
    }

    override fun onPause() {
        super.onPause()
        if (::player.isInitialized) {
            wasPlaying = player.playWhenReady
            player.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::player.isInitialized && wasPlaying) player.play()
    }

    override fun onDestroy() {
        if (::player.isInitialized) player.release()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUi() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
    }

    companion object {
        private const val TAG = "IPTVPlayer"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_URL = "url"
        private const val EXTRA_FALLBACK = "fallback"
        private const val EXTRA_LIVE = "live"

        fun start(
            context: Context,
            title: String,
            url: String,
            fallbackUrl: String?,
            live: Boolean,
        ) {
            context.startActivity(
                Intent(context, PlayerActivity::class.java)
                    .putExtra(EXTRA_TITLE, title)
                    .putExtra(EXTRA_URL, url)
                    .putExtra(EXTRA_FALLBACK, fallbackUrl)
                    .putExtra(EXTRA_LIVE, live)
            )
        }
    }
}

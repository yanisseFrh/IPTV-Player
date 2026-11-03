package com.iptvplayer.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.iptvplayer.R
import com.iptvplayer.data.Prefs
import com.iptvplayer.util.Expiry

class HomeActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var accountLabel: TextView
    private lateinit var expiryLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        if (!prefs.isConfigured()) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_home)
        accountLabel = findViewById(R.id.accountLabel)
        expiryLabel = findViewById(R.id.expiryLabel)

        findViewById<Button>(R.id.liveButton).setOnClickListener {
            BrowseActivity.start(this, BrowseActivity.MODE_LIVE)
        }
        findViewById<Button>(R.id.moviesButton).setOnClickListener {
            BrowseActivity.start(this, BrowseActivity.MODE_VOD)
        }
        findViewById<Button>(R.id.accountButton).setOnClickListener {
            SetupActivity.startEdit(this)
        }

        refreshAccount()
        findViewById<Button>(R.id.liveButton).requestFocus()
    }

    override fun onResume() {
        super.onResume()
        if (::prefs.isInitialized) refreshAccount()
    }

    private fun refreshAccount() {
        accountLabel.text = getString(R.string.home_connected, prefs.username())
        val expiresAt = prefs.expiresAt()
        if (expiresAt > 0L) {
            expiryLabel.visibility = android.view.View.VISIBLE
            expiryLabel.text = Expiry.label(this, expiresAt)
            expiryLabel.setTextColor(
                resources.getColor(
                    if (Expiry.isExpired(expiresAt)) R.color.error else R.color.blue_100
                )
            )
        } else {
            expiryLabel.visibility = android.view.View.GONE
        }
    }
}

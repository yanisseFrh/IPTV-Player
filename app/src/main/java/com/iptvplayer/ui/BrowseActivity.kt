package com.iptvplayer.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iptvplayer.R
import com.iptvplayer.data.Prefs
import com.iptvplayer.data.XtreamClient
import com.iptvplayer.data.XtreamUrl
import com.iptvplayer.model.Category
import com.iptvplayer.util.Ui

class BrowseActivity : Activity() {

    private lateinit var client: XtreamClient
    private lateinit var server: String
    private lateinit var username: String
    private lateinit var password: String
    private var mode = MODE_LIVE

    private lateinit var categoryList: RecyclerView
    private lateinit var mediaList: RecyclerView
    private lateinit var sectionTitle: TextView
    private lateinit var countLabel: TextView
    private lateinit var statusBox: View
    private lateinit var progress: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var retryButton: Button

    private val categoryAdapter = CategoryAdapter { selectCategory(it, focusItems = true) }
    private val mediaAdapter = MediaAdapter { play(it) }
    private val itemCache = HashMap<String, List<MediaRow>>()

    private var selectedCategory: Category? = null
    private var categoriesRequest = 0
    private var itemsRequest = 0
    private var retryAction: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        if (!prefs.isConfigured()) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }
        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_LIVE
        server = prefs.server()
        username = prefs.username()
        password = prefs.password()
        client = XtreamClient(server, username, password)

        setContentView(R.layout.activity_browse)
        categoryList = findViewById(R.id.categoryList)
        mediaList = findViewById(R.id.mediaList)
        sectionTitle = findViewById(R.id.sectionTitle)
        countLabel = findViewById(R.id.countLabel)
        statusBox = findViewById(R.id.statusBox)
        progress = findViewById(R.id.progress)
        statusText = findViewById(R.id.statusText)
        retryButton = findViewById(R.id.retryButton)

        findViewById<TextView>(R.id.screenTitle).setText(
            if (mode == MODE_LIVE) R.string.live_title else R.string.movies_title
        )
        findViewById<TextView>(R.id.backButton).setOnClickListener { finish() }

        categoryList.layoutManager = LinearLayoutManager(this)
        categoryList.adapter = categoryAdapter
        mediaList.layoutManager = LinearLayoutManager(this)
        mediaList.adapter = mediaAdapter

        retryButton.setOnClickListener { retryAction?.invoke() }

        loadCategories()
    }

    private fun loadCategories() {
        val request = ++categoriesRequest
        selectedCategory = null
        categoryAdapter.submit(emptyList())
        mediaAdapter.submit(emptyList())
        sectionTitle.text = ""
        countLabel.text = ""
        showLoading(R.string.loading_categories)

        Ui.background({
            if (mode == MODE_LIVE) client.liveCategories() else client.vodCategories()
        }) { result ->
            if (isFinishing || isDestroyed || request != categoriesRequest) return@background
            result.fold(
                onSuccess = { categories ->
                    if (categories.isEmpty()) {
                        showEmpty(R.string.empty_categories)
                    } else {
                        categoryAdapter.submit(categories)
                        selectCategory(categories.first(), focusItems = false)
                        focusCategory(0)
                    }
                },
                onFailure = { error ->
                    showError(error.message ?: getString(R.string.err_network)) { loadCategories() }
                },
            )
        }
    }

    private fun selectCategory(category: Category, focusItems: Boolean) {
        if (selectedCategory?.id == category.id && mediaAdapter.itemCount > 0) {
            if (focusItems) focusFirstMedia()
            return
        }
        selectedCategory = category
        categoryAdapter.selectedId = category.id
        sectionTitle.text = category.name

        val cached = itemCache[category.id]
        if (cached != null) {
            if (cached.isEmpty()) {
                showEmpty(emptyMessage())
            } else {
                showRows(cached)
                if (focusItems) focusFirstMedia()
            }
            return
        }

        mediaAdapter.submit(emptyList())
        countLabel.text = ""
        showLoading(if (mode == MODE_LIVE) R.string.loading_items else R.string.loading_movies)
        val request = ++itemsRequest

        Ui.background({
            fetchRows(category.id)
        }) { result ->
            if (isFinishing || isDestroyed || request != itemsRequest) return@background
            result.fold(
                onSuccess = { rows ->
                    itemCache[category.id] = rows
                    if (rows.isEmpty()) {
                        showEmpty(emptyMessage())
                    } else {
                        showRows(rows)
                        if (focusItems) focusFirstMedia()
                    }
                },
                onFailure = { error ->
                    showError(error.message ?: getString(R.string.err_network)) {
                        selectCategory(category, focusItems = true)
                    }
                },
            )
        }
    }

    private fun fetchRows(categoryId: String): List<MediaRow> =
        if (mode == MODE_LIVE) {
            client.liveStreams(categoryId).map { channel ->
                MediaRow(
                    id = channel.id,
                    leading = if (channel.number > 0) channel.number.toString() else "",
                    title = channel.name,
                    subtitle = null,
                    url = XtreamUrl.live(server, username, password, channel.id, hls = true),
                    fallbackUrl = XtreamUrl.live(server, username, password, channel.id, hls = false),
                )
            }
        } else {
            client.vodStreams(categoryId).map { movie ->
                MediaRow(
                    id = movie.id,
                    leading = "",
                    title = movie.name,
                    subtitle = movie.year.ifBlank { null },
                    url = XtreamUrl.vod(server, username, password, movie.id, movie.extension),
                    fallbackUrl = null,
                )
            }
        }

    private fun play(row: MediaRow) {
        PlayerActivity.start(this, row.title, row.url, row.fallbackUrl, mode == MODE_LIVE)
    }

    private fun showLoading(message: Int) {
        retryAction = null
        statusBox.visibility = View.VISIBLE
        progress.visibility = View.VISIBLE
        retryButton.visibility = View.GONE
        statusText.setText(message)
        statusText.setTextColor(resources.getColor(R.color.text))
    }

    private fun showEmpty(message: Int) {
        retryAction = null
        mediaAdapter.submit(emptyList())
        updateCount(0)
        statusBox.visibility = View.VISIBLE
        progress.visibility = View.GONE
        retryButton.visibility = View.GONE
        statusText.setText(message)
        statusText.setTextColor(resources.getColor(R.color.text_muted))
    }

    private fun showError(message: String, retry: () -> Unit) {
        retryAction = retry
        statusBox.visibility = View.VISIBLE
        progress.visibility = View.GONE
        retryButton.visibility = View.VISIBLE
        statusText.text = message
        statusText.setTextColor(resources.getColor(R.color.error))
        retryButton.post { retryButton.requestFocus() }
    }

    private fun showRows(rows: List<MediaRow>) {
        statusBox.visibility = View.GONE
        mediaAdapter.submit(rows)
        updateCount(rows.size)
    }

    private fun updateCount(count: Int) {
        countLabel.text = if (mode == MODE_LIVE) {
            getString(if (count == 1) R.string.count_channel else R.string.count_channels, count)
        } else {
            getString(if (count == 1) R.string.count_movie else R.string.count_movies, count)
        }
    }

    private fun emptyMessage(): Int =
        if (mode == MODE_LIVE) R.string.empty_items else R.string.empty_movies

    private fun focusCategory(position: Int) {
        categoryList.post {
            categoryList.scrollToPosition(position)
            categoryList.post {
                categoryList.findViewHolderForAdapterPosition(position)?.itemView?.requestFocus()
            }
        }
    }

    private fun focusFirstMedia() {
        mediaList.post {
            mediaList.scrollToPosition(0)
            mediaList.post {
                mediaList.getChildAt(0)?.requestFocus()
            }
        }
    }

    companion object {
        const val MODE_LIVE = "live"
        const val MODE_VOD = "vod"
        private const val EXTRA_MODE = "mode"

        fun start(context: Context, mode: String) {
            context.startActivity(
                Intent(context, BrowseActivity::class.java).putExtra(EXTRA_MODE, mode)
            )
        }
    }
}

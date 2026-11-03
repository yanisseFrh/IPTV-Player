package com.iptvplayer.util

import android.os.Handler
import android.os.Looper

object Ui {

    private val main = Handler(Looper.getMainLooper())

    /** Exécute un appel réseau hors du thread principal, puis rend le résultat sur le thread UI. */
    fun <T> background(task: () -> T, onDone: (Result<T>) -> Unit) {
        Thread {
            val result = runCatching(task)
            main.post { onDone(result) }
        }.apply { isDaemon = true }.start()
    }
}

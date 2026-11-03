package com.iptvplayer.data

import java.net.URLEncoder

/**
 * URLs de l'API Xtream. Fonctions pures, testées sans Android.
 */
object XtreamUrl {

    /** Nettoie une adresse collée par l'utilisateur et garde la racine du panneau. */
    fun normalizeServer(raw: String): String {
        var server = raw.trim()
        if (server.isEmpty()) return ""
        if (!server.startsWith("http://", ignoreCase = true) &&
            !server.startsWith("https://", ignoreCase = true)
        ) {
            server = "http://$server"
        }
        val query = server.indexOf('?')
        if (query >= 0) server = server.substring(0, query)
        server = server.trimEnd('/')
        for (suffix in arrayOf("/player_api.php", "/get.php")) {
            if (server.endsWith(suffix, ignoreCase = true)) {
                server = server.substring(0, server.length - suffix.length).trimEnd('/')
            }
        }
        return server
    }

    fun api(
        server: String,
        user: String,
        password: String,
        action: String? = null,
        categoryId: String? = null,
    ): String {
        val url = StringBuilder(server)
            .append("/player_api.php?username=").append(encode(user))
            .append("&password=").append(encode(password))
        if (action != null) url.append("&action=").append(encode(action))
        if (categoryId != null) url.append("&category_id=").append(encode(categoryId))
        return url.toString()
    }

    fun live(server: String, user: String, password: String, streamId: String, hls: Boolean): String =
        "$server/live/${encode(user)}/${encode(password)}/${encode(streamId)}.${if (hls) "m3u8" else "ts"}"

    fun vod(
        server: String,
        user: String,
        password: String,
        streamId: String,
        extension: String,
    ): String {
        val ext = extension.trim().removePrefix(".").ifBlank { "mp4" }
        return "$server/movie/${encode(user)}/${encode(password)}/${encode(streamId)}.$ext"
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}

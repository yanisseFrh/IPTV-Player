package com.iptvplayer.data

import android.content.Context

class Prefs(context: Context) {

    private val prefs = context.getSharedPreferences("iptvplayer", Context.MODE_PRIVATE)

    fun server(): String = prefs.getString(KEY_SERVER, "").orEmpty()
    fun username(): String = prefs.getString(KEY_USER, "").orEmpty()
    fun password(): String = prefs.getString(KEY_PASSWORD, "").orEmpty()
    fun expiresAt(): Long = prefs.getLong(KEY_EXPIRES_AT, 0L)

    fun isConfigured(): Boolean =
        server().isNotBlank() && username().isNotBlank() && password().isNotBlank()

    fun save(server: String, username: String, password: String, expiresAt: Long?) {
        prefs.edit()
            .putString(KEY_SERVER, server)
            .putString(KEY_USER, username)
            .putString(KEY_PASSWORD, password)
            .putLong(KEY_EXPIRES_AT, expiresAt ?: 0L)
            .apply()
    }

    private companion object {
        const val KEY_SERVER = "server"
        const val KEY_USER = "user"
        const val KEY_PASSWORD = "password"
        const val KEY_EXPIRES_AT = "expires_at"
    }
}

package com.iptvplayer.util

import android.content.Context
import com.iptvplayer.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Expiry {

    /** Libellé lisible de la date d'expiration Xtream (secondes Unix). */
    fun label(context: Context, expiresAt: Long): String {
        if (expiresAt <= 0L) return context.getString(R.string.expiry_none)
        val date = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(expiresAt * 1000L))
        return if (expiresAt * 1000L < System.currentTimeMillis()) {
            context.getString(R.string.expiry_expired, date)
        } else {
            context.getString(R.string.expiry_until, date)
        }
    }

    fun isExpired(expiresAt: Long): Boolean =
        expiresAt > 0L && expiresAt * 1000L < System.currentTimeMillis()
}

package com.iptvplayer.model

data class Account(
    val status: String,
    val username: String,
    /** Date d'expiration Xtream en secondes Unix, null si le serveur ne la fournit pas. */
    val expiresAt: Long?,
)

data class Category(
    val id: String,
    val name: String,
)

data class Channel(
    val id: String,
    val number: Int,
    val name: String,
)

data class Movie(
    val id: String,
    val name: String,
    val extension: String,
    val year: String,
)

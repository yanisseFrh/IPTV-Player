package com.iptvplayer.data

import com.iptvplayer.model.Account
import com.iptvplayer.model.Category
import com.iptvplayer.model.Channel
import com.iptvplayer.model.Movie
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class XtreamException(message: String) : Exception(message)

/**
 * Lecture des réponses JSON Xtream. Fonctions pures, testées sur la JVM.
 */
object XtreamParser {

    fun login(body: String, fallbackUser: String): Account {
        val json = try {
            JSONObject(body.trim())
        } catch (e: JSONException) {
            throw XtreamException("Réponse inattendue du serveur Xtream.")
        }
        val info = json.optJSONObject("user_info")
            ?: throw XtreamException("Réponse inattendue du serveur Xtream.")
        if (json.optInt("auth", 1) == 0 || info.optInt("auth", 1) == 0) {
            throw XtreamException("Identifiants refusés. Vérifiez le serveur, l'utilisateur et le mot de passe.")
        }
        val status = info.optString("status").trim()
        if (status.isNotEmpty() && !status.equals("Active", true) && !status.equals("Actif", true)) {
            throw XtreamException("Ce compte Xtream n'est pas actif.")
        }
        val expiry = when (val raw = info.opt("exp_date")) {
            is Number -> raw.toLong()
            is String -> raw.toLongOrNull()
            else -> null
        }?.takeIf { it > 0 }
        return Account(status, info.optString("username").ifBlank { fallbackUser }, expiry)
    }

    fun categories(body: String): List<Category> =
        objects(body).mapNotNull { obj ->
            val id = obj.optString("category_id").trim()
            if (id.isEmpty()) return@mapNotNull null
            Category(id, obj.optString("category_name").trim().ifBlank { "Sans nom" })
        }

    fun liveStreams(body: String): List<Channel> =
        objects(body).mapNotNull { obj ->
            val id = obj.optString("stream_id").trim()
            if (id.isEmpty()) return@mapNotNull null
            Channel(
                id = id,
                number = obj.optString("num").trim().toIntOrNull() ?: 0,
                name = obj.optString("name").trim().ifBlank { "Chaîne sans nom" },
            )
        }.sortedWith(compareBy({ it.number == 0 }, { it.number }, { it.name.lowercase() }))

    fun vodStreams(body: String): List<Movie> =
        objects(body).mapNotNull { obj ->
            val id = obj.optString("stream_id").trim()
            if (id.isEmpty()) return@mapNotNull null
            Movie(
                id = id,
                name = obj.optString("name").trim().ifBlank { "Film sans nom" },
                extension = obj.optString("container_extension").trim().ifBlank { "mp4" },
                year = obj.optString("year").trim().takeUnless { it == "0" }.orEmpty(),
            )
        }.sortedBy { it.name.lowercase() }

    private fun objects(body: String): List<JSONObject> {
        val arr = array(body)
        val result = ArrayList<JSONObject>(arr.length())
        for (index in 0 until arr.length()) {
            arr.optJSONObject(index)?.let { result.add(it) }
        }
        return result
    }

    private fun array(body: String): JSONArray {
        val text = body.trim()
        if (!text.startsWith("[")) {
            throw XtreamException("Réponse inattendue du serveur Xtream.")
        }
        return try {
            JSONArray(text)
        } catch (e: JSONException) {
            throw XtreamException("Réponse inattendue du serveur Xtream.")
        }
    }
}

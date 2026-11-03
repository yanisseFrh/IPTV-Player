package com.iptvplayer.data

import com.iptvplayer.model.Account
import com.iptvplayer.model.Category
import com.iptvplayer.model.Channel
import com.iptvplayer.model.Movie
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class XtreamClient(
    private val server: String,
    private val user: String,
    private val password: String,
) {

    fun login(): Account =
        XtreamParser.login(get(XtreamUrl.api(server, user, password)), user)

    fun liveCategories(): List<Category> =
        XtreamParser.categories(get(XtreamUrl.api(server, user, password, "get_live_categories")))

    fun liveStreams(categoryId: String?): List<Channel> =
        XtreamParser.liveStreams(get(XtreamUrl.api(server, user, password, "get_live_streams", categoryId)))

    fun vodCategories(): List<Category> =
        XtreamParser.categories(get(XtreamUrl.api(server, user, password, "get_vod_categories")))

    fun vodStreams(categoryId: String?): List<Movie> =
        XtreamParser.vodStreams(get(XtreamUrl.api(server, user, password, "get_vod_streams", categoryId)))

    private fun get(url: String): String {
        var connection: HttpURLConnection? = null
        try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 25_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "IPTVPlayer/1.0 (Android TV)")
            connection.setRequestProperty("Accept", "application/json")
            connection.useCaches = false

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw XtreamException("Le serveur a répondu une erreur ($code).")
            }
            if (body.isBlank()) throw XtreamException("Le serveur a renvoyé une réponse vide.")
            return body
        } catch (e: XtreamException) {
            throw e
        } catch (e: UnknownHostException) {
            throw XtreamException("Adresse du serveur introuvable.")
        } catch (e: SocketTimeoutException) {
            throw XtreamException("Le serveur ne répond pas. Vérifiez l'adresse et votre connexion.")
        } catch (e: SSLException) {
            throw XtreamException("Connexion sécurisée impossible avec ce serveur.")
        } catch (e: IOException) {
            throw XtreamException("Connexion au serveur impossible.")
        } catch (e: Exception) {
            throw XtreamException("Adresse du serveur invalide.")
        } finally {
            connection?.disconnect()
        }
    }
}

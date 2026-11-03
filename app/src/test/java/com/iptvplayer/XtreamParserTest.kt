package com.iptvplayer

import com.iptvplayer.data.XtreamException
import com.iptvplayer.data.XtreamParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class XtreamParserTest {

    @Test
    fun `login reads username and expiry date`() {
        val body = """
            {"user_info":{"auth":1,"status":"Active","username":"demo","exp_date":"1800000000"},"server_info":{}}
        """.trimIndent()
        val account = XtreamParser.login(body, "fallback")
        assertEquals("demo", account.username)
        assertEquals(1_800_000_000L, account.expiresAt)
    }

    @Test
    fun `login keeps working when expiry is missing`() {
        val account = XtreamParser.login(
            """{"user_info":{"auth":1,"status":"Active","username":"demo","exp_date":null}}""",
            "fallback",
        )
        assertNull(account.expiresAt)
    }

    @Test
    fun `login refuses invalid credentials`() {
        assertThrows(XtreamException::class.java) {
            XtreamParser.login("""{"user_info":{"auth":0}}""", "fallback")
        }
    }

    @Test
    fun `login refuses inactive account`() {
        assertThrows(XtreamException::class.java) {
            XtreamParser.login("""{"user_info":{"auth":1,"status":"Expired"}}""", "fallback")
        }
    }

    @Test
    fun `categories skip entries without id`() {
        val categories = XtreamParser.categories(
            """[{"category_id":"1","category_name":"Info"},{"category_id":"","category_name":"X"}]"""
        )
        assertEquals(1, categories.size)
        assertEquals("Info", categories[0].name)
    }

    @Test
    fun `live streams sort by channel number then name`() {
        val channels = XtreamParser.liveStreams(
            """
            [{"stream_id":3,"num":"2","name":"Zèbre"},
             {"stream_id":1,"num":1,"name":"Arte"},
             {"stream_id":2,"num":0,"name":"Autre"}]
            """.trimIndent()
        )
        assertEquals(listOf("Arte", "Zèbre", "Autre"), channels.map { it.name })
    }

    @Test
    fun `vod streams read year and container extension`() {
        val movies = XtreamParser.vodStreams(
            """[{"stream_id":9,"name":"Océans","container_extension":"mkv","year":"2010"}]"""
        )
        assertEquals("mkv", movies[0].extension)
        assertEquals("2010", movies[0].year)
    }

    @Test
    fun `malformed responses are rejected`() {
        assertThrows(XtreamException::class.java) {
            XtreamParser.categories("""{"error":"bad"}""")
        }
    }
}

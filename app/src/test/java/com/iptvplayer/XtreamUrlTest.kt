package com.iptvplayer

import com.iptvplayer.data.XtreamUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class XtreamUrlTest {

    @Test
    fun `server normalization keeps scheme and strips trailing slash`() {
        assertEquals("http://demo.tv:8080", XtreamUrl.normalizeServer(" http://demo.tv:8080/ "))
        assertEquals("https://demo.tv", XtreamUrl.normalizeServer("https://demo.tv"))
    }

    @Test
    fun `server normalization adds http when missing`() {
        assertEquals("http://demo.tv:8080", XtreamUrl.normalizeServer("demo.tv:8080"))
    }

    @Test
    fun `server normalization accepts a pasted player_api url`() {
        assertEquals("http://demo.tv:8080", XtreamUrl.normalizeServer("http://demo.tv:8080/player_api.php?username=x"))
        assertEquals("http://demo.tv:8080/panel", XtreamUrl.normalizeServer("http://demo.tv:8080/panel/get.php"))
    }

    @Test
    fun `api url encodes credentials and category`() {
        assertEquals(
            "http://demo.tv/player_api.php?username=jean%20dupont&password=p%26ss&action=get_live_streams&category_id=12",
            XtreamUrl.api("http://demo.tv", "jean dupont", "p&ss", "get_live_streams", "12"),
        )
    }

    @Test
    fun `live urls use hls and ts`() {
        assertEquals(
            "http://demo.tv/live/user/pass/42.m3u8",
            XtreamUrl.live("http://demo.tv", "user", "pass", "42", hls = true),
        )
        assertEquals(
            "http://demo.tv/live/user/pass/42.ts",
            XtreamUrl.live("http://demo.tv", "user", "pass", "42", hls = false),
        )
    }

    @Test
    fun `vod url uses the movie extension`() {
        assertEquals(
            "http://demo.tv/movie/user/pass/7.mkv",
            XtreamUrl.vod("http://demo.tv", "user", "pass", "7", "mkv"),
        )
        assertEquals(
            "http://demo.tv/movie/user/pass/7.mp4",
            XtreamUrl.vod("http://demo.tv", "user", "pass", "7", ""),
        )
    }
}

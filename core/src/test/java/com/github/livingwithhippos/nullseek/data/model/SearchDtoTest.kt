package com.github.livingwithhippos.nullseek.data.model

import com.github.livingwithhippos.nullseek.data.remote.parseJackettResults
import com.github.livingwithhippos.nullseek.data.remote.parseProwlarrResults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchDtoTest {
    @Test
    fun jackettMapping() {
        val body = """{"Results":[{"Title":"A","Size":100,"Seeders":5,"Peers":7,
            "PublishDate":"2024-01-01T00:00:00","MagnetUri":"magnet:?x","Link":"http://l",
            "Details":"http://d","Tracker":"T","Unknown":1},{"Title":""}],"Indexers":[]}"""
        val r = parseJackettResults(body, "fallback")
        assertEquals(1, r.size)
        assertEquals("T", r[0].indexerName)
        assertEquals(100L, r[0].sizeBytes)
        assertEquals(7, r[0].peers)
        assertEquals(1704067200000L, r[0].publishedAt)
        assertEquals("magnet:?x", r[0].magnetLink)
    }

    @Test
    fun prowlarrMapping() {
        val body = """[{"title":"B","size":50,"seeders":2,"leechers":3,"publishDate":"2024-01-01T00:00:00Z",
            "magnetUrl":"","downloadUrl":"http://dl","infoUrl":"http://i","categories":[{"name":"Movies"}]}]"""
        val r = parseProwlarrResults(body, "fallback")
        assertEquals("fallback", r[0].indexerName)
        assertEquals(3, r[0].peers)
        assertNull(r[0].magnetLink)
        assertEquals("http://dl", r[0].downloadLink)
        assertEquals(listOf("Movies"), r[0].categories)
        assertEquals(1704067200000L, r[0].publishedAt)
    }
}

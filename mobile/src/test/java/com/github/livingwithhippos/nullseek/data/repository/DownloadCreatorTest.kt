package com.github.livingwithhippos.nullseek.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadCreatorTest {
    @Test
    fun magnetLinksAreTorrents() {
        assertEquals(LinkType.MAGNET, linkType("magnet:?xt=urn:btih:abc"))
        assertEquals(LinkType.MAGNET, linkType("MAGNET:?xt=urn:btih:abc"))
    }

    @Test
    fun nzbLinksAreUsenet() {
        assertEquals(LinkType.NZB, linkType("https://example.com/file.NZB"))
    }

    @Test
    fun otherLinksAreWebDownloads() {
        assertEquals(LinkType.WEB, linkType("https://example.com/file.zip"))
    }
}

package com.github.livingwithhippos.nullseek.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HashUtilsTest {
    private val hex = "c9e15763f722f23e98a29decdfae341b98d53056"

    @Test
    fun `hex hashes are lowercased`() {
        assertEquals(hex, normalizeInfoHash(hex.uppercase()))
    }

    @Test
    fun `base32 hashes are converted to hex`() {
        assertEquals(hex, normalizeInfoHash("ZHQVOY7XELZD5GFCTXWN7LRUDOMNKMCW"))
        assertEquals(hex, normalizeInfoHash("zhqvoy7xelzd5gfctxwn7lrudomnkmcw"))
    }

    @Test
    fun `invalid hashes are rejected`() {
        assertNull(normalizeInfoHash(null))
        assertNull(normalizeInfoHash("abc"))
        assertNull(normalizeInfoHash("z".repeat(40)))
    }

    @Test
    fun `info hash is read from magnet links`() {
        assertEquals(hex, extractInfoHash("magnet:?xt=urn:btih:${hex.uppercase()}&dn=Big%20Buck%20Bunny&tr=udp%3A%2F%2Ftracker"))
        assertEquals(hex, extractInfoHash("magnet:?dn=name&xt=urn:btih:ZHQVOY7XELZD5GFCTXWN7LRUDOMNKMCW"))
        assertNull(extractInfoHash("https://example.com/file.torrent"))
        assertNull(extractInfoHash(null))
    }
}

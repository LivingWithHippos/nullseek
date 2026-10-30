package com.github.livingwithhippos.nullseek.ui.newdownload

import org.junit.Assert.assertEquals
import org.junit.Test

class AppendLinkLineTest {
    @Test
    fun `pasting into an empty field fills it`() {
        assertEquals("magnet:?xt=a", appendLinkLine("", "magnet:?xt=a"))
        assertEquals("magnet:?xt=a", appendLinkLine("  \n", " magnet:?xt=a\n"))
    }

    @Test
    fun `pasting into existing text adds a new line`() {
        assertEquals("magnet:?xt=a\nhttps://b", appendLinkLine("magnet:?xt=a", "https://b"))
        assertEquals("magnet:?xt=a\nhttps://b", appendLinkLine("magnet:?xt=a\n ", "https://b"))
    }

    @Test
    fun `blank clipboard leaves the text unchanged`() {
        assertEquals("magnet:?xt=a\n", appendLinkLine("magnet:?xt=a\n", null))
        assertEquals("magnet:?xt=a", appendLinkLine("magnet:?xt=a", "   "))
    }
}

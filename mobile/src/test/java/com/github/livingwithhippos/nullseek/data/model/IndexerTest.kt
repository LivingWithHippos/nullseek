package com.github.livingwithhippos.nullseek.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class IndexerTest {
    @Test
    fun entityRoundTrip() {
        val indexer = Indexer(3, "Home", IndexerType.PROWLARR, "host:9696", "key", false)
        val entity = indexer.toEntity()
        assertEquals("PROWLARR", entity.type)
        assertEquals("key", entity.apiToken)
        assertEquals(indexer, Indexer.fromEntity(entity))
    }

    @Test
    fun unknownTypeFallsBackToJackett() {
        assertEquals(IndexerType.JACKETT, IndexerType.fromString("nope"))
    }
}

package com.github.livingwithhippos.nullseek.core.domain

import com.github.livingwithhippos.nullseek.core.domain.realdebrid.hosterLinkFor
import com.github.livingwithhippos.nullseek.core.domain.realdebrid.realDebridLimit
import com.github.livingwithhippos.nullseek.core.domain.realdebrid.realDebridPage
import com.github.livingwithhippos.nullseek.core.domain.realdebrid.realDebridStatus
import com.github.livingwithhippos.nullseek.core.domain.realdebrid.realDebridStreamingPage
import com.github.livingwithhippos.nullseek.core.domain.realdebrid.toDebridError
import com.github.livingwithhippos.nullseek.core.domain.realdebrid.toDomain
import com.github.livingwithhippos.realdebrid.model.RealDebridErrorDto
import com.github.livingwithhippos.realdebrid.model.TorrentDto
import com.github.livingwithhippos.realdebrid.model.TorrentFileDto
import com.github.livingwithhippos.realdebrid.network.ApiResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RealDebridMappingTest {
    private fun torrent(
        status: String = "downloaded",
        files: List<TorrentFileDto> = emptyList(),
        links: List<String> = emptyList(),
    ) = TorrentDto(
        id = "ABC",
        filename = "Some.Show",
        hash = "hash",
        bytes = 100,
        host = "real-debrid.com",
        split = 2000,
        progress = 50f,
        status = status,
        added = "2026-11-01T10:00:00.000Z",
        files = files,
        links = links,
    )

    @Test
    fun sizeIsTheSelectedBytes() {
        val partial = torrent().copy(bytes = 40, originalBytes = 100)
        assertEquals(40L, partial.toDomain(filesLoaded = true).size)
    }

    @Test
    fun listPagesStartAtOne() {
        assertEquals(1, realDebridPage(offset = 0, limit = 50))
        assertEquals(2, realDebridPage(offset = 50, limit = 50))
        assertEquals(4, realDebridPage(offset = 300, limit = 100))
        assertEquals(2, realDebridPage(offset = 500, limit = 500))
        // the page size is capped at 5000, the page follows the capped size
        assertEquals(5000, realDebridLimit(8000))
        assertEquals(3, realDebridPage(offset = 10_000, limit = 8000))
    }

    @Test
    fun streamingPageUsesTheDownloadId() {
        assertEquals("https://real-debrid.com/streaming-ABC", realDebridStreamingPage("ABC"))
    }

    @Test
    fun statusesAreMapped() {
        assertEquals(DownloadStatus.CONVERTING, realDebridStatus("magnet_conversion"))
        assertEquals(DownloadStatus.AWAITING_FILE_SELECTION, realDebridStatus("waiting_files_selection"))
        assertEquals(DownloadStatus.QUEUED, realDebridStatus("queued"))
        assertEquals(DownloadStatus.DOWNLOADING, realDebridStatus("downloading"))
        assertEquals(DownloadStatus.DOWNLOADING, realDebridStatus("compressing"))
        assertEquals(DownloadStatus.UPLOADING, realDebridStatus("uploading"))
        assertEquals(DownloadStatus.READY, realDebridStatus("downloaded"))
        assertEquals(DownloadStatus.FAILED, realDebridStatus("magnet_error"))
        assertEquals(DownloadStatus.FAILED, realDebridStatus("virus"))
        assertEquals(DownloadStatus.EXPIRED, realDebridStatus("dead"))
        assertEquals(DownloadStatus.UNKNOWN, realDebridStatus("something_new"))
    }

    @Test
    fun torrentIsMappedWithProgressAndReadiness() {
        val mapped = torrent(status = "downloading").toDomain(filesLoaded = false)
        assertEquals(0.5, mapped.progress, 0.0001)
        assertFalse(mapped.cached)
        assertFalse(mapped.filesLoaded)
        assertEquals("downloading", mapped.downloadState)
        assertTrue(torrent().toDomain(filesLoaded = true).cached)
    }

    @Test
    fun selectedFilesGetLinkIndexesInIdOrder() {
        val mapped =
            torrent(
                files =
                    listOf(
                        TorrentFileDto(id = 3, path = "/folder/c.mkv", bytes = 3, selected = 1),
                        TorrentFileDto(id = 1, path = "/folder/a.mkv", bytes = 1, selected = 1),
                        TorrentFileDto(id = 2, path = "/folder/b.nfo", bytes = 2, selected = 0),
                    ),
                links = listOf("link-a", "link-c"),
            ).toDomain(filesLoaded = true)

        val byId = mapped.files.associateBy { it.id }
        assertEquals(0, byId.getValue(1).linkIndex)
        assertNull(byId.getValue(2).linkIndex)
        assertEquals(1, byId.getValue(3).linkIndex)
        assertEquals("folder/c.mkv", byId.getValue(3).name)
        assertEquals("c.mkv", byId.getValue(3).shortName)

        assertEquals("link-a", hosterLinkFor(mapped, 1))
        assertEquals("link-c", hosterLinkFor(mapped, 3))
        assertNull(hosterLinkFor(mapped, 2))
        // several links can't be downloaded as a whole
        assertNull(hosterLinkFor(mapped, null))
    }

    @Test
    fun singleArchiveLinkIsUsedForEveryFile() {
        val mapped =
            torrent(
                files =
                    listOf(
                        TorrentFileDto(id = 1, path = "/a.mkv", bytes = 1, selected = 1),
                        TorrentFileDto(id = 2, path = "/b.mkv", bytes = 2, selected = 1),
                    ),
                links = listOf("archive"),
            ).toDomain(filesLoaded = true)

        assertEquals("archive", hosterLinkFor(mapped, 2))
        assertEquals("archive", hosterLinkFor(mapped, null))
    }

    @Test
    fun mismatchedLinksGiveNoLink() {
        val mapped =
            torrent(
                files = (1..3).map { TorrentFileDto(id = it, path = "/$it.mkv", bytes = 1, selected = 1) },
                links = listOf("one", "two"),
            ).toDomain(filesLoaded = true)

        assertNull(hosterLinkFor(mapped, 1))
    }

    @Test
    fun errorsAreMapped() {
        assertEquals(
            DebridError.InvalidToken,
            ApiResult.Failure.Api(401, RealDebridErrorDto("bad_token", errorCode = 8)).toDebridError(),
        )
        assertEquals(
            DebridError.RateLimited,
            ApiResult.Failure.Api(429, RealDebridErrorDto("too_many_requests", errorCode = 34)).toDebridError(),
        )
        assertEquals(
            DebridError.PermissionDenied,
            ApiResult.Failure.Api(403, RealDebridErrorDto("permission_denied", errorCode = 9)).toDebridError(),
        )
        assertTrue(
            ApiResult.Failure.Api(503, RealDebridErrorDto("hoster_unavailable", errorCode = 19)).toDebridError()
                is DebridError.HosterProblem,
        )
        assertEquals(DebridError.InvalidToken, ApiResult.Failure.Http(401, null).toDebridError())
        assertEquals(DebridError.NotFound, ApiResult.Failure.Http(404, null).toDebridError())
    }
}

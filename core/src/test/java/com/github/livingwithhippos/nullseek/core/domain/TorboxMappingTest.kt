package com.github.livingwithhippos.nullseek.core.domain

import com.github.livingwithhippos.nullseek.core.domain.torbox.toDebridError
import com.github.livingwithhippos.nullseek.core.domain.torbox.torboxStatus
import com.github.livingwithhippos.nullseek.core.utils.mockTorrents
import com.github.livingwithhippos.torbox.model.ApiResponseDto
import com.github.livingwithhippos.torbox.network.ApiResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TorboxMappingTest {
    @Test
    fun statusesAreMapped() {
        assertEquals(DownloadStatus.READY, torboxStatus("completed", cached = false, downloadFinished = true))
        assertEquals(DownloadStatus.READY, torboxStatus("downloading", cached = true, downloadFinished = false))
        assertEquals(DownloadStatus.DOWNLOADING, torboxStatus("downloading", cached = false, downloadFinished = false))
        assertEquals(DownloadStatus.CONVERTING, torboxStatus("metaDL", cached = false, downloadFinished = false))
        assertEquals(DownloadStatus.SEEDING, torboxStatus("uploading", cached = true, downloadFinished = true, seeding = true))
        assertEquals(DownloadStatus.STALLED, torboxStatus("stalled (no seeds)", cached = false, downloadFinished = false))
        assertEquals(DownloadStatus.FAILED, torboxStatus("failed (whatever)", cached = false, downloadFinished = false))
        assertEquals(DownloadStatus.EXPIRED, torboxStatus("expired", cached = false, downloadFinished = false))
        assertEquals(DownloadStatus.QUEUED, torboxStatus("queuedDL", cached = false, downloadFinished = false))
    }

    @Test
    fun mockTorrentsKeepTheirFields() {
        val torrent = mockTorrents.first()
        assertEquals(ProviderKind.TORBOX, torrent.provider)
        assertTrue(torrent.filesLoaded)
        assertTrue(torrent.files.all { it.selected })
        assertTrue(torrent.id.toIntOrNull() != null)
    }

    @Test
    fun errorsAreMapped() {
        assertEquals(DebridError.InvalidToken, ApiResult.Failure.Http(401, null).toDebridError())
        assertEquals(DebridError.RateLimited, ApiResult.Failure.Http(429, null).toDebridError())
        assertEquals(
            DebridError.InvalidToken,
            ApiResult.Failure.NoSuccess(ApiResponseDto<String>(success = false, error = "BAD_TOKEN", detail = "")).toDebridError(),
        )
        assertTrue(
            ApiResult.Failure
                .NoSuccess(ApiResponseDto<String>(success = false, error = "DOWNLOAD_TOO_LARGE", detail = "x"))
                .toDebridError() is DebridError.Server,
        )
    }
}

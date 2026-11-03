package com.github.livingwithhippos.nullseek.core.domain

import com.github.livingwithhippos.nullseek.data.repository.isEndOfList
import com.github.livingwithhippos.nullseek.ui.newdownload.FileSelectionQueue
import com.github.livingwithhippos.nullseek.ui.newdownload.PendingSelection
import com.github.livingwithhippos.nullseek.ui.newdownload.SelectionDecision
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun file(id: Int) = DebridFile(id = id, name = "f$id.mkv", shortName = "f$id.mkv", size = 1, mimetype = null)

private fun pending(
    id: String,
    vararg fileIds: Int,
) = PendingSelection(id, "torrent $id", fileIds.map { file(it) })

private fun torrentWith(status: DownloadStatus) =
    DebridTorrent(
        id = "T",
        provider = ProviderKind.REAL_DEBRID,
        name = "T",
        hash = "h",
        size = 1,
        progress = 0.0,
        status = status,
        downloadState = status.name,
        cached = false,
        downloadFinished = false,
        active = true,
        createdAt = "",
        files = listOf(file(1), file(2)),
    )

class FileSelectionTest {
    private class Recorder {
        val selected = mutableListOf<Pair<String, List<Int>?>>()
        val deleted = mutableListOf<String>()
    }

    private fun queue(
        recorder: Recorder,
        decisions: ArrayDeque<SelectionDecision>,
        asked: MutableList<Pair<String, Int>> = mutableListOf(),
    ) = FileSelectionQueue(
        select = { id, ids ->
            recorder.selected.add(id to ids)
            DebridResult.Ok(Unit)
        },
        delete = { id ->
            recorder.deleted.add(id)
            DebridResult.Ok(Unit)
        },
        ask = { item, remaining ->
            asked.add(item.torrentId to remaining)
            decisions.removeFirst()
        },
    )

    @Test
    fun requeueKeepsThePreviousSelection() {
        val old = torrentWith(DownloadStatus.READY).copy(files = listOf(file(1), file(2).copy(selected = false), file(3)))
        val new = torrentWith(DownloadStatus.AWAITING_FILE_SELECTION).copy(files = listOf(file(1), file(2), file(3)))
        assertEquals(setOf(1, 3), requeuePreselection(old, new))
        // nothing in common: check everything
        val other = new.copy(files = listOf(file(7), file(8)))
        assertEquals(setOf(7, 8), requeuePreselection(old, other))
        assertEquals("magnet:?xt=urn:btih:abc", magnetFromHash("abc"))
    }

    @Test
    fun selectionRequestUsesAllShortcut() {
        assertNull(selectionToRequest(listOf(1, 2, 3), setOf(3, 2, 1)))
        assertEquals(listOf(1, 3), selectionToRequest(listOf(1, 2, 3), setOf(3, 1)))
    }

    @Test
    fun singleFileTorrentsAreNotAsked() =
        runBlocking {
            val recorder = Recorder()
            val asked = mutableListOf<Pair<String, Int>>()
            val summary = queue(recorder, ArrayDeque(), asked).process(listOf(pending("A", 1)))
            assertTrue(asked.isEmpty())
            assertEquals(listOf("A" to null), recorder.selected)
            assertEquals(1, summary.selected)
        }

    @Test
    fun decisionsAreApplied() =
        runBlocking {
            val recorder = Recorder()
            val asked = mutableListOf<Pair<String, Int>>()
            val summary =
                queue(
                    recorder,
                    ArrayDeque(
                        listOf(
                            SelectionDecision.Confirm(setOf(2)),
                            SelectionDecision.Skip,
                            SelectionDecision.Delete,
                        ),
                    ),
                    asked,
                ).process(listOf(pending("A", 1, 2), pending("B", 1, 2), pending("C", 1, 2)))

            assertEquals(listOf("A" to 2, "B" to 1, "C" to 0), asked)
            assertEquals(listOf("A" to listOf(2)), recorder.selected)
            assertEquals(listOf("C"), recorder.deleted)
            assertEquals(1, summary.selected)
            assertEquals(1, summary.skipped)
            assertEquals(1, summary.deleted)
        }

    @Test
    fun selectAllRemainingStopsAsking() =
        runBlocking {
            val recorder = Recorder()
            val asked = mutableListOf<Pair<String, Int>>()
            val summary =
                queue(recorder, ArrayDeque(listOf(SelectionDecision.SelectAllRemaining)), asked)
                    .process(listOf(pending("A", 1, 2), pending("B", 1, 2), pending("C", 1, 2)))

            assertEquals(1, asked.size)
            assertEquals(listOf("A" to null, "B" to null, "C" to null), recorder.selected)
            assertEquals(3, summary.selected)
        }

    @Test
    fun conversionWaitsForFileSelection() =
        runBlocking {
            val statuses =
                ArrayDeque(listOf(DownloadStatus.CONVERTING, DownloadStatus.CONVERTING, DownloadStatus.AWAITING_FILE_SELECTION))
            val service = FakeTorrentService { torrentWith(statuses.removeFirst()) }
            val outcome = service.awaitConversion("T", initialDelayMs = 1, maxDelayMs = 1)
            assertTrue(outcome is ConversionOutcome.NeedsSelection)
            assertTrue(statuses.isEmpty())
        }

    @Test
    fun conversionReportsFailuresAndStarts() =
        runBlocking {
            assertTrue(
                FakeTorrentService { torrentWith(DownloadStatus.FAILED) }.awaitConversion("T", initialDelayMs = 1)
                    is ConversionOutcome.Failed,
            )
            assertTrue(
                FakeTorrentService { torrentWith(DownloadStatus.DOWNLOADING) }.awaitConversion("T", initialDelayMs = 1)
                    is ConversionOutcome.Started,
            )
        }

    @Test
    fun conversionTimesOut() =
        runBlocking {
            var time = 0L
            val service = FakeTorrentService { torrentWith(DownloadStatus.CONVERTING) }
            val outcome =
                service.awaitConversion(
                    "T",
                    timeoutMs = 10,
                    initialDelayMs = 1,
                    maxDelayMs = 1,
                    clock = { time.also { time += 5 } },
                )
            assertEquals(ConversionOutcome.TimedOut, outcome)
        }

    @Test
    fun endOfListUsesTotalCount() {
        assertFalse(isEndOfList(0, Page(List(10) { it }, totalCount = 25), 10))
        assertTrue(isEndOfList(20, Page(List(5) { it }, totalCount = 25), 10))
        assertTrue(isEndOfList(0, Page(List(3) { it }), 10))
        assertFalse(isEndOfList(0, Page(List(10) { it }), 10))
        assertTrue(isEndOfList(10, Page(emptyList<Int>(), totalCount = 50), 10))
    }
}

/** Only answers [torrent], everything else fails. */
private class FakeTorrentService(
    private val next: () -> DebridTorrent,
) : DebridService {
    override val kind = ProviderKind.REAL_DEBRID

    override fun setToken(token: String) = Unit

    private fun <T> unsupported(): DebridResult<T> = DebridResult.Err(DebridError.Unsupported)

    override suspend fun user(): DebridResult<UserProfile> = unsupported()

    override suspend fun listTorrents(
        offset: Int,
        limit: Int,
        bypassCache: Boolean,
    ): DebridResult<Page<DebridTorrent>> = unsupported()

    override suspend fun torrent(id: String): DebridResult<DebridTorrent> = DebridResult.Ok(next())

    override suspend fun addMagnet(
        magnet: String,
        options: AddOptions,
    ): DebridResult<AddedItem> = unsupported()

    override suspend fun addTorrentFile(
        fileName: String,
        bytes: ByteArray,
        options: AddOptions,
    ): DebridResult<AddedItem> = unsupported()

    override suspend fun deleteTorrent(id: String): DebridResult<Unit> = unsupported()

    override suspend fun torrentDownloadLink(
        torrent: DebridTorrent,
        fileId: Int?,
        zip: Boolean,
    ): DebridResult<DebridLink> = unsupported()

    override suspend fun listWeb(
        offset: Int,
        limit: Int,
        bypassCache: Boolean,
    ): DebridResult<Page<DebridDownload>> = unsupported()

    override suspend fun web(id: String): DebridResult<DebridDownload> = unsupported()

    override suspend fun addWeb(
        link: String,
        password: String?,
    ): DebridResult<AddedItem> = unsupported()

    override suspend fun deleteWeb(id: String): DebridResult<Unit> = unsupported()

    override suspend fun webDownloadLink(
        download: DebridDownload,
        fileId: Int?,
        zip: Boolean,
    ): DebridResult<DebridLink> = unsupported()
}

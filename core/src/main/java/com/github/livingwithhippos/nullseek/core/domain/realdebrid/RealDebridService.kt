package com.github.livingwithhippos.nullseek.core.domain.realdebrid

import com.github.livingwithhippos.nullseek.core.domain.AddOptions
import com.github.livingwithhippos.nullseek.core.domain.AddedItem
import com.github.livingwithhippos.nullseek.core.domain.DebridDownload
import com.github.livingwithhippos.nullseek.core.domain.DebridError
import com.github.livingwithhippos.nullseek.core.domain.DebridFile
import com.github.livingwithhippos.nullseek.core.domain.DebridLink
import com.github.livingwithhippos.nullseek.core.domain.DebridResult
import com.github.livingwithhippos.nullseek.core.domain.DebridService
import com.github.livingwithhippos.nullseek.core.domain.DebridTorrent
import com.github.livingwithhippos.nullseek.core.domain.DownloadStatus
import com.github.livingwithhippos.nullseek.core.domain.ItemKind
import com.github.livingwithhippos.nullseek.core.domain.Page
import com.github.livingwithhippos.nullseek.core.domain.ProviderKind
import com.github.livingwithhippos.nullseek.core.domain.ProviderUserExtras
import com.github.livingwithhippos.nullseek.core.domain.UserProfile
import com.github.livingwithhippos.realdebrid.client.RealDebridClient
import com.github.livingwithhippos.realdebrid.model.DownloadDto
import com.github.livingwithhippos.realdebrid.model.RealDebridErrorCode
import com.github.livingwithhippos.realdebrid.model.UserDto
import logcat.logcat
import com.github.livingwithhippos.realdebrid.model.TorrentDto as RdTorrentDto
import com.github.livingwithhippos.realdebrid.network.ApiResult as RdApiResult

// Real-Debrid accepts up to 5000 entries per page
internal const val MAX_PAGE_SIZE = 5000

/** The Real-Debrid page that streams a generated download in the browser. */
internal fun realDebridStreamingPage(downloadId: String): String = "https://real-debrid.com/streaming-$downloadId"

/** The page size sent to Real-Debrid for the requested [limit]. */
internal fun realDebridLimit(limit: Int): Int = limit.coerceIn(1, MAX_PAGE_SIZE)

/**
 * The Real-Debrid page (starting at 1) holding a 0-based app [offset].
 * The app pages in steps of [limit], so the offset is always a multiple of it.
 */
internal fun realDebridPage(
    offset: Int,
    limit: Int,
): Int = offset.coerceAtLeast(0) / realDebridLimit(limit) + 1

class RealDebridService(
    private val client: RealDebridClient,
    private val limiter: RateLimiter = RateLimiter(),
) : DebridService {
    override val kind = ProviderKind.REAL_DEBRID

    // Real-Debrid has no endpoint for a single download, keep the ones we saw
    private val webCache =
        object : LinkedHashMap<String, DebridDownload>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, DebridDownload>?) = size > 300
        }

    override fun setToken(token: String) {
        client.updateBearerToken(token)
    }

    private suspend fun <T> call(block: suspend () -> RdApiResult<T>): DebridResult<T> {
        limiter.acquire()
        return when (val result = block()) {
            is RdApiResult.Success -> DebridResult.Ok(result.value)
            is RdApiResult.Failure -> DebridResult.Err(result.toDebridError())
        }
    }

    override suspend fun user(): DebridResult<UserProfile> = call { client.user.get() }.mapOk { it.toProfile() }

    override suspend fun listTorrents(
        offset: Int,
        limit: Int,
        bypassCache: Boolean,
    ): DebridResult<Page<DebridTorrent>> =
        call { client.torrents.list(page = realDebridPage(offset, limit), limit = realDebridLimit(limit)) }
            .mapOk { page -> Page(page.items.map { it.toDomain(filesLoaded = false) }, page.totalCount) }

    override suspend fun torrent(id: String): DebridResult<DebridTorrent> =
        call { client.torrents.info(id) }.mapOk { it.toDomain(filesLoaded = true) }

    override suspend fun addMagnet(
        magnet: String,
        options: AddOptions,
    ): DebridResult<AddedItem> = call { client.torrents.addMagnet(magnet) }.mapOk { AddedItem(it.id, ItemKind.TORRENT) }

    override suspend fun addTorrentFile(
        fileName: String,
        bytes: ByteArray,
        options: AddOptions,
    ): DebridResult<AddedItem> = call { client.torrents.addTorrent(bytes) }.mapOk { AddedItem(it.id, ItemKind.TORRENT) }

    override suspend fun selectFiles(
        torrentId: String,
        fileIds: List<Int>?,
    ): DebridResult<Unit> {
        if (fileIds != null && fileIds.isEmpty()) return DebridResult.Err(DebridError.Server(null, "No files selected"))
        logcat { "Selecting files of torrent $torrentId: ${fileIds?.joinToString(",") ?: "all"}" }
        return call { client.torrents.selectFiles(torrentId, fileIds) }.also { logcat { "Select files of $torrentId: $it" } }
    }

    override suspend fun deleteTorrent(id: String): DebridResult<Unit> = call { client.torrents.delete(id) }

    override suspend fun torrentDownloadLink(
        torrent: DebridTorrent,
        fileId: Int?,
        zip: Boolean,
    ): DebridResult<DebridLink> {
        // the list endpoint has no files, links may also be missing until the download ends
        val loaded =
            if (torrent.filesLoaded && torrent.hosterLinks.isNotEmpty()) {
                torrent
            } else {
                when (val refreshed = torrent(torrent.id)) {
                    is DebridResult.Ok -> refreshed.value
                    is DebridResult.Err -> return refreshed
                }
            }
        val link =
            hosterLinkFor(loaded, fileId)
                ?: return DebridResult.Err(
                    if (fileId == null) DebridError.Unsupported else DebridError.Server(null, "No link for this file"),
                )
        return unrestrict(link)
    }

    private suspend fun unrestrict(
        link: String,
        password: String? = null,
    ): DebridResult<DebridLink> =
        call { client.unrestrict.link(link, password) }.mapOk { dto ->
            DebridLink(
                url = dto.download,
                streamingPageUrl = dto.id.takeIf { dto.streamable == 1 }?.let(::realDebridStreamingPage),
                fileName = dto.filename,
                size = dto.fileSize,
                mimeType = dto.mimeType,
            )
        }

    override suspend fun unlockLink(link: String): DebridResult<DebridLink> = unrestrict(link)

    override suspend fun listWeb(
        offset: Int,
        limit: Int,
        bypassCache: Boolean,
    ): DebridResult<Page<DebridDownload>> =
        call { client.downloads.list(page = realDebridPage(offset, limit), limit = realDebridLimit(limit)) }
            .mapOk { page ->
                val items = page.items.map { it.toDomain() }
                synchronized(webCache) { items.forEach { webCache[it.id] = it } }
                Page(items, page.totalCount)
            }

    override suspend fun web(id: String): DebridResult<DebridDownload> =
        synchronized(webCache) { webCache[id] }?.let { DebridResult.Ok(it) }
            ?: DebridResult.Err(DebridError.NotFound)

    override suspend fun addWeb(
        link: String,
        password: String?,
    ): DebridResult<AddedItem> =
        call { client.unrestrict.link(link, password) }.mapOk { dto ->
            val item = dto.toDomain()
            synchronized(webCache) { webCache[item.id] = item }
            AddedItem(item.id, ItemKind.WEB)
        }

    override suspend fun deleteWeb(id: String): DebridResult<Unit> =
        call { client.downloads.delete(id) }.also {
            if (it is DebridResult.Ok) synchronized(webCache) { webCache.remove(id) }
        }

    override suspend fun webDownloadLink(
        download: DebridDownload,
        fileId: Int?,
        zip: Boolean,
    ): DebridResult<DebridLink> {
        download.directUrl?.let { url ->
            val streamingPage = download.id.takeIf { download.streamable == true }?.let(::realDebridStreamingPage)
            return DebridResult.Ok(DebridLink(url, streamingPage))
        }
        return unrestrict(download.originalUrl)
    }
}

/**
 * The hoster link for a file. Real-Debrid returns one link per selected file, in file id order,
 * but archives packs into a single link, so with one link every file maps to it.
 */
internal fun hosterLinkFor(
    torrent: DebridTorrent,
    fileId: Int?,
): String? {
    val links = torrent.hosterLinks
    if (links.isEmpty()) return null
    if (fileId == null) return links.singleOrNull()
    val selected = torrent.files.filter { it.selected }
    if (links.size == 1) return links.first()
    if (links.size != selected.size) return null
    val index = selected.firstOrNull { it.id == fileId }?.linkIndex ?: return null
    return links.getOrNull(index)
}

private inline fun <T, R> DebridResult<T>.mapOk(transform: (T) -> R): DebridResult<R> =
    when (this) {
        is DebridResult.Ok -> DebridResult.Ok(transform(value))
        is DebridResult.Err -> this
    }

internal fun RdApiResult.Failure.toDebridError(): DebridError =
    when (this) {
        is RdApiResult.Failure.Api -> {
            when (error.code) {
                RealDebridErrorCode.BAD_TOKEN -> {
                    DebridError.InvalidToken
                }

                RealDebridErrorCode.PERMISSION_DENIED -> {
                    DebridError.PermissionDenied
                }

                RealDebridErrorCode.ACCOUNT_LOCKED -> {
                    DebridError.AccountLocked
                }

                RealDebridErrorCode.TOO_MANY_REQUESTS, RealDebridErrorCode.SLOW_DOWN -> {
                    DebridError.RateLimited
                }

                RealDebridErrorCode.FAIR_USAGE_LIMIT -> {
                    DebridError.FairUsage
                }

                RealDebridErrorCode.RESOURCE_NOT_FOUND, RealDebridErrorCode.UNKNOWN_METHOD -> {
                    DebridError.NotFound
                }

                RealDebridErrorCode.UNSUPPORTED_HOSTER,
                RealDebridErrorCode.HOSTER_IN_MAINTENANCE,
                RealDebridErrorCode.HOSTER_LIMIT_REACHED,
                RealDebridErrorCode.HOSTER_TEMPORARILY_UNAVAILABLE,
                RealDebridErrorCode.HOSTER_NOT_AVAILABLE_FOR_FREE_USERS,
                -> {
                    DebridError.HosterProblem(error.error)
                }

                else -> {
                    when (httpCode) {
                        401 -> DebridError.InvalidToken
                        429 -> DebridError.RateLimited
                        else -> DebridError.Server(httpCode, error.errorDetails ?: error.error)
                    }
                }
            }
        }

        is RdApiResult.Failure.Http -> {
            when (code) {
                401 -> DebridError.InvalidToken
                403 -> DebridError.PermissionDenied
                404 -> DebridError.NotFound
                429 -> DebridError.RateLimited
                else -> DebridError.Server(code, body)
            }
        }

        is RdApiResult.Failure.BadRequest -> {
            DebridError.Server(null, cause.message)
        }

        is RdApiResult.Failure.Network -> {
            DebridError.Network(cause)
        }

        is RdApiResult.Failure.Serialization -> {
            DebridError.Serialization(cause)
        }
    }

internal fun realDebridStatus(status: String): DownloadStatus =
    when (status) {
        "magnet_conversion" -> DownloadStatus.CONVERTING
        "waiting_files_selection" -> DownloadStatus.AWAITING_FILE_SELECTION
        "queued" -> DownloadStatus.QUEUED
        "downloading", "compressing" -> DownloadStatus.DOWNLOADING
        "uploading" -> DownloadStatus.UPLOADING
        "downloaded" -> DownloadStatus.READY
        "magnet_error", "error", "virus" -> DownloadStatus.FAILED
        "dead" -> DownloadStatus.EXPIRED
        else -> DownloadStatus.UNKNOWN
    }

internal fun RdTorrentDto.toDomain(filesLoaded: Boolean): DebridTorrent {
    val domainStatus = realDebridStatus(status)
    // links follow the selected files in id order
    val selectedIds =
        files
            .filter { it.selected == 1 }
            .map { it.id }
            .sorted()
    val domainFiles =
        files.map { file ->
            val index = selectedIds.indexOf(file.id).takeIf { it >= 0 }
            DebridFile(
                id = file.id,
                name = file.path.removePrefix("/"),
                shortName = file.path.substringAfterLast('/'),
                size = file.bytes,
                mimetype = null,
                selected = file.selected == 1,
                linkIndex = index,
            )
        }
    val ready = domainStatus == DownloadStatus.READY
    return DebridTorrent(
        id = id,
        provider = ProviderKind.REAL_DEBRID,
        name = filename.ifBlank { originalFilename.orEmpty() },
        hash = hash,
        // the size of the selected files only
        size = bytes,
        progress = (progress / 100.0).coerceIn(0.0, 1.0),
        status = domainStatus,
        downloadState = status.replace('_', ' '),
        cached = ready,
        downloadFinished = ready,
        active =
            domainStatus in
                setOf(DownloadStatus.DOWNLOADING, DownloadStatus.UPLOADING, DownloadStatus.QUEUED, DownloadStatus.CONVERTING),
        downloadSpeed = speed ?: 0,
        seeds = seeders,
        createdAt = added,
        updatedAt = ended,
        files = domainFiles,
        filesLoaded = filesLoaded,
        hosterLinks = links,
    )
}

internal fun DownloadDto.toDomain(): DebridDownload =
    DebridDownload(
        id = id,
        provider = ProviderKind.REAL_DEBRID,
        name = filename,
        size = fileSize,
        progress = 1.0,
        status = DownloadStatus.READY,
        downloadState = if (streamable == 1) "streamable" else "ready",
        cached = true,
        downloadFinished = true,
        active = false,
        originalUrl = link,
        host = host,
        files =
            listOf(
                DebridFile(id = 0, name = filename, shortName = filename, size = fileSize, mimetype = mimeType),
            ),
        directUrl = download,
        streamable = streamable?.let { it == 1 },
        createdAt = generated.orEmpty(),
    )

internal fun UserDto.toProfile(): UserProfile =
    UserProfile(
        provider = ProviderKind.REAL_DEBRID,
        id = id.toString(),
        username = username,
        email = email,
        isPremium = type == "premium",
        premiumExpiresAt = expiration,
        avatarUrl = avatar,
        points = points,
        extras = ProviderUserExtras.RealDebrid(type = type, premiumSecondsLeft = premium, locale = locale),
    )

package com.github.livingwithhippos.nullseek.core.domain.torbox

import com.github.livingwithhippos.nullseek.core.domain.AddOptions
import com.github.livingwithhippos.nullseek.core.domain.AddedItem
import com.github.livingwithhippos.nullseek.core.domain.DebridDownload
import com.github.livingwithhippos.nullseek.core.domain.DebridError
import com.github.livingwithhippos.nullseek.core.domain.DebridFile
import com.github.livingwithhippos.nullseek.core.domain.DebridResult
import com.github.livingwithhippos.nullseek.core.domain.DebridService
import com.github.livingwithhippos.nullseek.core.domain.DebridTorrent
import com.github.livingwithhippos.nullseek.core.domain.DownloadStatus
import com.github.livingwithhippos.nullseek.core.domain.ExportPayload
import com.github.livingwithhippos.nullseek.core.domain.ItemKind
import com.github.livingwithhippos.nullseek.core.domain.Page
import com.github.livingwithhippos.nullseek.core.domain.ProviderKind
import com.github.livingwithhippos.nullseek.core.domain.ProviderUserExtras
import com.github.livingwithhippos.nullseek.core.domain.TorrentFullInfo
import com.github.livingwithhippos.nullseek.core.domain.TorrentFullInfoFile
import com.github.livingwithhippos.nullseek.core.domain.UserProfile
import com.github.livingwithhippos.torbox.client.TorboxClient
import com.github.livingwithhippos.torbox.model.ApiResponseDto
import com.github.livingwithhippos.torbox.model.ApiResponseListDto
import com.github.livingwithhippos.torbox.model.ExportResultDto
import com.github.livingwithhippos.torbox.model.TorrentDto
import com.github.livingwithhippos.torbox.model.TorrentFileDto
import com.github.livingwithhippos.torbox.model.TorrentInfoDto
import com.github.livingwithhippos.torbox.model.UsenetDownloadDto
import com.github.livingwithhippos.torbox.model.UsenetDownloadFileDto
import com.github.livingwithhippos.torbox.model.UserDataDto
import com.github.livingwithhippos.torbox.model.UserStatsDto
import com.github.livingwithhippos.torbox.model.WebDownloadDto
import com.github.livingwithhippos.torbox.model.WebDownloadFileDto
import com.github.livingwithhippos.torbox.network.ApiResult
import java.util.Locale

// how user/stats groups the bandwidth history (TorBox keeps the last 30 days)
private const val STATS_BANDWIDTH_GROUPING = "month"

class TorboxService(
    private val client: TorboxClient,
) : DebridService {
    override val kind = ProviderKind.TORBOX

    // the download link endpoints take the token as a parameter
    @Volatile
    private var token: String = ""

    override fun setToken(token: String) {
        this.token = token
        client.updateBearerToken(token)
    }

    override suspend fun user(): DebridResult<UserProfile> =
        client.user
            .data()
            .data()
            .mapOk { it.toProfile() }

    override suspend fun listTorrents(
        offset: Int,
        limit: Int,
        bypassCache: Boolean,
    ): DebridResult<Page<DebridTorrent>> =
        client.torrent
            .list(bypassCache = bypassCache, offset = offset, limit = limit)
            .list()
            .mapOk { items -> Page(items.map { it.toDomain() }) }

    override suspend fun torrent(id: String): DebridResult<DebridTorrent> {
        val intId = id.toIntOrNull() ?: return DebridResult.Err(DebridError.NotFound)
        return client.torrent
            .get(bypassCache = true, id = intId)
            .data()
            .mapOk { it.toDomain() }
    }

    override suspend fun addMagnet(
        magnet: String,
        options: AddOptions,
    ): DebridResult<AddedItem> =
        client.torrent
            .create(magnet = magnet, seed = options.seeding, allowZip = options.allowZip)
            .data()
            .mapOk { AddedItem(it.torrentId.toString(), ItemKind.TORRENT) }

    override suspend fun addTorrentFile(
        fileName: String,
        bytes: ByteArray,
        options: AddOptions,
    ): DebridResult<AddedItem> =
        client.torrent
            .create(fileBytes = bytes, fileName = fileName, seed = options.seeding, allowZip = options.allowZip)
            .data()
            .mapOk { AddedItem(it.torrentId.toString(), ItemKind.TORRENT) }

    override suspend fun deleteTorrent(id: String): DebridResult<Unit> {
        val intId = id.toIntOrNull() ?: return DebridResult.Err(DebridError.NotFound)
        return client.torrent.control(torrentId = intId, operation = "Delete").unit()
    }

    override suspend fun torrentDownloadLink(
        torrent: DebridTorrent,
        fileId: Int?,
        zip: Boolean,
    ): DebridResult<String> {
        val intId = torrent.id.toIntOrNull() ?: return DebridResult.Err(DebridError.NotFound)
        if (token.isBlank()) return DebridResult.Err(DebridError.InvalidToken)
        return client.torrent
            .downloadLink(token = token, torrentId = intId, fileId = fileId, zipLink = zip)
            .data()
    }

    override suspend fun listWeb(
        offset: Int,
        limit: Int,
        bypassCache: Boolean,
    ): DebridResult<Page<DebridDownload>> =
        client.web
            .list(bypassCache = bypassCache, offset = offset, limit = limit)
            .list()
            .mapOk { items -> Page(items.map { it.toDomain() }) }

    override suspend fun web(id: String): DebridResult<DebridDownload> {
        val intId = id.toIntOrNull() ?: return DebridResult.Err(DebridError.NotFound)
        return client.web
            .get(bypassCache = true, id = intId)
            .data()
            .mapOk { it.toDomain() }
    }

    override suspend fun addWeb(
        link: String,
        password: String?,
    ): DebridResult<AddedItem> =
        client.web
            .create(link = link, password = password)
            .data()
            .mapOk { AddedItem(it.webDownloadId.toString(), ItemKind.WEB) }

    override suspend fun deleteWeb(id: String): DebridResult<Unit> {
        val intId = id.toIntOrNull() ?: return DebridResult.Err(DebridError.NotFound)
        return client.web.control(webId = intId, operation = "Delete").unit()
    }

    override suspend fun webDownloadLink(
        download: DebridDownload,
        fileId: Int?,
        zip: Boolean,
    ): DebridResult<String> {
        val intId = download.id.toIntOrNull() ?: return DebridResult.Err(DebridError.NotFound)
        if (token.isBlank()) return DebridResult.Err(DebridError.InvalidToken)
        return client.web
            .downloadLink(token = token, webId = intId, fileId = fileId, zipLink = zip)
            .data()
    }

    override suspend fun listUsenet(
        offset: Int,
        limit: Int,
        bypassCache: Boolean,
    ): DebridResult<Page<DebridDownload>> =
        client.usenet
            .list(bypassCache = bypassCache, offset = offset, limit = limit)
            .list()
            .mapOk { items -> Page(items.map { it.toDomain() }) }

    override suspend fun usenet(id: String): DebridResult<DebridDownload> {
        val intId = id.toIntOrNull() ?: return DebridResult.Err(DebridError.NotFound)
        return client.usenet
            .get(bypassCache = true, id = intId)
            .data()
            .mapOk { it.toDomain() }
    }

    override suspend fun addNzbLink(
        link: String,
        options: AddOptions,
    ): DebridResult<AddedItem> =
        client.usenet
            .create(link = link, password = options.password, postProcessing = options.usenetOption)
            .data()
            .mapOk { AddedItem(it.usenetId.toString(), ItemKind.USENET) }

    override suspend fun addNzbFile(
        fileName: String,
        bytes: ByteArray,
        options: AddOptions,
    ): DebridResult<AddedItem> =
        client.usenet
            .create(
                fileBytes = bytes,
                fileName = fileName,
                password = options.password,
                postProcessing = options.usenetOption,
            ).data()
            .mapOk { AddedItem(it.usenetId.toString(), ItemKind.USENET) }

    override suspend fun deleteUsenet(id: String): DebridResult<Unit> {
        val intId = id.toIntOrNull() ?: return DebridResult.Err(DebridError.NotFound)
        return client.usenet.control(usenetId = intId, operation = "Delete").unit()
    }

    override suspend fun usenetDownloadLink(
        download: DebridDownload,
        fileId: Int?,
        zip: Boolean,
    ): DebridResult<String> {
        val intId = download.id.toIntOrNull() ?: return DebridResult.Err(DebridError.NotFound)
        if (token.isBlank()) return DebridResult.Err(DebridError.InvalidToken)
        return client.usenet
            .downloadLink(token = token, usenetId = intId, fileId = fileId, zipLink = zip)
            .data()
    }

    override suspend fun cachedHashes(hashes: List<String>): DebridResult<Set<String>> =
        client.torrent
            .listAvailabilityBatch(hashes)
            .list()
            .mapOk { items -> items.map { it.hash.lowercase(Locale.ROOT) }.toSet() }

    override suspend fun exportTorrent(
        id: String,
        asMagnet: Boolean,
    ): DebridResult<ExportPayload> {
        val intId = id.toIntOrNull() ?: return DebridResult.Err(DebridError.NotFound)
        return when (val result = client.torrent.export(torrentId = intId, type = if (asMagnet) "magnet" else "file")) {
            is ApiResult.Success -> {
                DebridResult.Ok(
                    when (val value = result.value) {
                        is ExportResultDto.Magnet -> ExportPayload.Magnet(value.magnet)
                        is ExportResultDto.Torrent -> ExportPayload.TorrentFile(value.bytes)
                    },
                )
            }

            is ApiResult.Failure -> {
                DebridResult.Err(result.toDebridError())
            }
        }
    }

    override suspend fun torrentFullInfo(hash: String): DebridResult<TorrentFullInfo> {
        val full = client.torrent.fullInfo(hash = hash, cacheLookup = true).data()
        if (full is DebridResult.Ok) return DebridResult.Ok(full.value.toDomain())
        // fallback to info(hash)
        return client.torrent
            .info(hash = hash, cacheLookup = true)
            .data()
            .mapOk { it.toDomain() }
    }

    override suspend fun stats(): DebridResult<UserStatsDto> =
        client.user
            .stats(general = true, bandwidth = true, bandwidthGrouping = STATS_BANDWIDTH_GROUPING)
            .data()
}

// result helpers

private inline fun <T, R> DebridResult<T>.mapOk(transform: (T) -> R): DebridResult<R> =
    when (this) {
        is DebridResult.Ok -> DebridResult.Ok(transform(value))
        is DebridResult.Err -> this
    }

private fun <T> ApiResult<ApiResponseDto<T>>.data(): DebridResult<T> =
    when (this) {
        is ApiResult.Success -> {
            value.data?.let { DebridResult.Ok(it) }
                ?: DebridResult.Err(DebridError.Server(null, value.error ?: value.detail))
        }

        is ApiResult.Failure -> {
            DebridResult.Err(toDebridError())
        }
    }

private fun <T> ApiResult<ApiResponseListDto<T>>.list(): DebridResult<List<T>> =
    when (this) {
        is ApiResult.Success -> DebridResult.Ok(value.data.orEmpty())
        is ApiResult.Failure -> DebridResult.Err(toDebridError())
    }

private fun ApiResult<*>.unit(): DebridResult<Unit> =
    when (this) {
        is ApiResult.Success -> DebridResult.Ok(Unit)
        is ApiResult.Failure -> DebridResult.Err(toDebridError())
    }

private val tokenErrors = setOf("BAD_TOKEN", "AUTH_ERROR", "NO_AUTH")

internal fun ApiResult.Failure.toDebridError(): DebridError =
    when (this) {
        is ApiResult.Failure.Http -> {
            when (code) {
                401, 403 -> DebridError.InvalidToken
                404 -> DebridError.NotFound
                429 -> DebridError.RateLimited
                else -> DebridError.Server(code, body)
            }
        }

        is ApiResult.Failure.Network -> {
            DebridError.Network(cause)
        }

        is ApiResult.Failure.BadRequest -> {
            DebridError.Server(null, cause.message)
        }

        is ApiResult.Failure.Serialization -> {
            DebridError.Serialization(cause)
        }

        is ApiResult.Failure.NoSuccess<*> -> {
            val (error, detail) =
                when (val v = value) {
                    is ApiResponseDto<*> -> v.error to v.detail
                    is ApiResponseListDto<*> -> v.error to v.detail
                    else -> null to null
                }
            if (error != null && error.uppercase(Locale.ROOT) in tokenErrors) {
                DebridError.InvalidToken
            } else {
                DebridError.Server(null, detail ?: error)
            }
        }
    }

// mappers

internal fun torboxStatus(
    downloadState: String,
    cached: Boolean,
    downloadFinished: Boolean,
    seeding: Boolean = false,
): DownloadStatus {
    val state = downloadState.lowercase(Locale.ROOT)
    return when {
        state.startsWith("failed") || state == "error" -> DownloadStatus.FAILED
        state == "expired" -> DownloadStatus.EXPIRED
        state == "uploading" && seeding -> DownloadStatus.SEEDING
        state == "seeding" -> DownloadStatus.SEEDING
        cached || downloadFinished || state == "completed" || state == "cached" -> DownloadStatus.READY
        state == "metadl" -> DownloadStatus.CONVERTING
        state == "downloading" || state == "checkingresumedata" -> DownloadStatus.DOWNLOADING
        state == "uploading" -> DownloadStatus.UPLOADING
        state.startsWith("paused") -> DownloadStatus.PAUSED
        state.startsWith("stalled") -> DownloadStatus.STALLED
        state.startsWith("queued") -> DownloadStatus.QUEUED
        else -> DownloadStatus.UNKNOWN
    }
}

internal fun TorrentFileDto.toDomain() =
    DebridFile(id = id, name = name, shortName = shortName, size = size, mimetype = mimetype, zipped = zipped)

internal fun WebDownloadFileDto.toDomain() =
    DebridFile(id = id, name = name, shortName = shortName, size = size, mimetype = mimetype, zipped = zipped)

internal fun UsenetDownloadFileDto.toDomain() =
    DebridFile(id = id, name = name, shortName = shortName, size = size, mimetype = mimetype, zipped = zipped)

internal fun TorrentDto.toDomain() =
    DebridTorrent(
        id = id.toString(),
        provider = ProviderKind.TORBOX,
        name = name,
        hash = hash,
        size = size,
        progress = progress,
        status = torboxStatus(downloadState, cached, downloadFinished, seedTorrent),
        downloadState = downloadState,
        cached = cached,
        downloadFinished = downloadFinished,
        active = active,
        downloadSpeed = downloadSpeed,
        uploadSpeed = uploadSpeed,
        seeds = seeds,
        peers = peers,
        ratio = ratio,
        eta = eta,
        createdAt = createdAt,
        updatedAt = updatedAt,
        expiresAt = expiresAt,
        magnet = magnet,
        files = files.map { it.toDomain() },
    )

internal fun WebDownloadDto.toDomain() =
    DebridDownload(
        id = id.toString(),
        provider = ProviderKind.TORBOX,
        name = name,
        hash = hash,
        size = size,
        progress = progress,
        status = torboxStatus(downloadState, cached, downloadFinished),
        downloadState = downloadState,
        cached = cached,
        downloadFinished = downloadFinished,
        active = active,
        downloadSpeed = downloadSpeed,
        eta = eta,
        originalUrl = originalUrl,
        files = files.map { it.toDomain() },
        createdAt = createdAt,
        updatedAt = updatedAt,
        expiresAt = expiresAt,
        error = error,
    )

internal fun UsenetDownloadDto.toDomain() =
    DebridDownload(
        id = id.toString(),
        provider = ProviderKind.TORBOX,
        name = name,
        hash = hash,
        size = size,
        progress = progress,
        status = torboxStatus(downloadState, cached, downloadFinished),
        downloadState = downloadState,
        cached = cached,
        downloadFinished = downloadFinished,
        active = active,
        downloadSpeed = downloadSpeed,
        eta = eta,
        originalUrl = originalUrl,
        files = files.map { it.toDomain() },
        createdAt = createdAt,
        updatedAt = updatedAt,
        expiresAt = expiresAt,
    )

internal fun TorrentInfoDto.toDomain() =
    TorrentFullInfo(
        name = name,
        hash = hash,
        size = size,
        trackers = trackers,
        seeds = seeds,
        peers = peers,
        files = files.map { TorrentFullInfoFile(it.name, it.size) },
    )

internal fun UserDataDto.toProfile() =
    UserProfile(
        provider = ProviderKind.TORBOX,
        id = id.toString(),
        username = null,
        email = email,
        isPremium = isSubscribed,
        premiumExpiresAt = premiumExpiresAt,
        extras = ProviderUserExtras.Torbox(this),
    )

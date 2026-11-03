package com.github.livingwithhippos.nullseek.core.domain

import com.github.livingwithhippos.torbox.model.UserStatsDto

/**
 * A debrid provider. Optional operations return [DebridError.Unsupported],
 * check [capabilities] before showing them.
 */
interface DebridService {
    val kind: ProviderKind
    val capabilities: DebridCapabilities get() = DebridCapabilities.of(kind)

    fun setToken(token: String)

    suspend fun user(): DebridResult<UserProfile>

    // torrents

    /** [offset] is 0-based, the adapters translate it for their API */
    suspend fun listTorrents(
        offset: Int,
        limit: Int,
        bypassCache: Boolean = true,
    ): DebridResult<Page<DebridTorrent>>

    /** a single torrent, with its files */
    suspend fun torrent(id: String): DebridResult<DebridTorrent>

    suspend fun addMagnet(
        magnet: String,
        options: AddOptions = AddOptions(),
    ): DebridResult<AddedItem>

    suspend fun addTorrentFile(
        fileName: String,
        bytes: ByteArray,
        options: AddOptions = AddOptions(),
    ): DebridResult<AddedItem>

    /** @param fileIds null to select every file */
    suspend fun selectFiles(
        torrentId: String,
        fileIds: List<Int>?,
    ): DebridResult<Unit> = DebridResult.Err(DebridError.Unsupported)

    suspend fun deleteTorrent(id: String): DebridResult<Unit>

    /** @param fileId null for the whole torrent */
    suspend fun torrentDownloadLink(
        torrent: DebridTorrent,
        fileId: Int?,
        zip: Boolean,
    ): DebridResult<DebridLink>

    // web / hoster downloads

    /** [offset] is 0-based, the adapters translate it for their API */
    suspend fun listWeb(
        offset: Int,
        limit: Int,
        bypassCache: Boolean = true,
    ): DebridResult<Page<DebridDownload>>

    suspend fun web(id: String): DebridResult<DebridDownload>

    suspend fun addWeb(
        link: String,
        password: String? = null,
    ): DebridResult<AddedItem>

    suspend fun deleteWeb(id: String): DebridResult<Unit>

    suspend fun webDownloadLink(
        download: DebridDownload,
        fileId: Int?,
        zip: Boolean,
    ): DebridResult<DebridLink>

    // usenet

    /** [offset] is 0-based, the adapters translate it for their API */
    suspend fun listUsenet(
        offset: Int,
        limit: Int,
        bypassCache: Boolean = true,
    ): DebridResult<Page<DebridDownload>> = DebridResult.Err(DebridError.Unsupported)

    suspend fun usenet(id: String): DebridResult<DebridDownload> = DebridResult.Err(DebridError.Unsupported)

    suspend fun addNzbLink(
        link: String,
        options: AddOptions = AddOptions(),
    ): DebridResult<AddedItem> = DebridResult.Err(DebridError.Unsupported)

    suspend fun addNzbFile(
        fileName: String,
        bytes: ByteArray,
        options: AddOptions = AddOptions(),
    ): DebridResult<AddedItem> = DebridResult.Err(DebridError.Unsupported)

    suspend fun deleteUsenet(id: String): DebridResult<Unit> = DebridResult.Err(DebridError.Unsupported)

    suspend fun usenetDownloadLink(
        download: DebridDownload,
        fileId: Int?,
        zip: Boolean,
    ): DebridResult<DebridLink> = DebridResult.Err(DebridError.Unsupported)

    // extras

    /** the subset of [hashes] (lowercase) the provider has cached */
    suspend fun cachedHashes(hashes: List<String>): DebridResult<Set<String>> = DebridResult.Err(DebridError.Unsupported)

    suspend fun exportTorrent(
        id: String,
        asMagnet: Boolean,
    ): DebridResult<ExportPayload> = DebridResult.Err(DebridError.Unsupported)

    suspend fun torrentFullInfo(hash: String): DebridResult<TorrentFullInfo> = DebridResult.Err(DebridError.Unsupported)

    suspend fun stats(): DebridResult<UserStatsDto> = DebridResult.Err(DebridError.Unsupported)
}

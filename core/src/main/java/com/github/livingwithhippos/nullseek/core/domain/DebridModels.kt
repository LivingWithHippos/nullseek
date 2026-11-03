package com.github.livingwithhippos.nullseek.core.domain

import androidx.compose.runtime.Immutable
import com.github.livingwithhippos.torbox.model.UserDataDto

/**
 * The debrid services the app can talk to.
 * [dbType] is the value stored in the providers table.
 */
enum class ProviderKind(
    val dbType: Int,
    val displayName: String,
) {
    TORBOX(0, "TorBox"),
    REAL_DEBRID(1, "Real-Debrid"),
    ;

    companion object {
        fun fromDbType(type: Int): ProviderKind = entries.firstOrNull { it.dbType == type } ?: TORBOX
    }
}

/** A provider independent status, the original label is kept in the items as `downloadState`. */
enum class DownloadStatus {
    QUEUED,

    /** fetching the magnet metadata */
    CONVERTING,

    /** Real-Debrid waits for the files to download to be picked */
    AWAITING_FILE_SELECTION,
    DOWNLOADING,

    /** the provider is moving the files to its storage */
    UPLOADING,
    SEEDING,
    PAUSED,
    STALLED,

    /** links can be generated */
    READY,
    FAILED,
    EXPIRED,
    UNKNOWN,
}

/**
 * A file inside a torrent, web or usenet download.
 * Field names follow the TorBox ones so the screens can use both providers.
 */
@Immutable
data class DebridFile(
    val id: Int,
    /** full path inside the download */
    val name: String,
    val shortName: String,
    val size: Long,
    val mimetype: String?,
    val zipped: Boolean = false,
    /** false for files not picked on Real-Debrid */
    val selected: Boolean = true,
    /** Real-Debrid: the position of this file's hoster link in [DebridTorrent.hosterLinks] */
    val linkIndex: Int? = null,
)

@Immutable
data class DebridTorrent(
    val id: String,
    val provider: ProviderKind,
    val name: String,
    val hash: String,
    val size: Long,
    /** 0..1 */
    val progress: Double,
    val status: DownloadStatus,
    /** the provider status label, for display */
    val downloadState: String,
    /** links can be generated (TorBox cached, Real-Debrid downloaded) */
    val cached: Boolean,
    val downloadFinished: Boolean,
    val active: Boolean,
    val downloadSpeed: Long = 0,
    val uploadSpeed: Long? = null,
    val seeds: Int? = null,
    val peers: Int? = null,
    val ratio: Double? = null,
    val eta: Int? = null,
    val createdAt: String,
    val updatedAt: String? = null,
    val expiresAt: String? = null,
    val magnet: String? = null,
    val files: List<DebridFile> = emptyList(),
    /** false when the list endpoint does not return files (Real-Debrid) */
    val filesLoaded: Boolean = true,
    /** Real-Debrid hoster links, one per selected file, to unrestrict */
    val hosterLinks: List<String> = emptyList(),
)

/** A web (hoster) or usenet download. */
@Immutable
data class DebridDownload(
    val id: String,
    val provider: ProviderKind,
    val name: String,
    val hash: String? = null,
    val size: Long,
    val progress: Double,
    val status: DownloadStatus,
    val downloadState: String,
    val cached: Boolean,
    val downloadFinished: Boolean,
    val active: Boolean,
    val downloadSpeed: Long = 0,
    val eta: Int? = null,
    val originalUrl: String,
    val host: String? = null,
    val files: List<DebridFile> = emptyList(),
    /** Real-Debrid: the link is already unrestricted */
    val directUrl: String? = null,
    val streamable: Boolean? = null,
    val createdAt: String,
    val updatedAt: String? = null,
    val expiresAt: String? = null,
    val error: String? = null,
)

@Immutable
data class UserProfile(
    val provider: ProviderKind,
    val id: String,
    val username: String?,
    val email: String,
    val isPremium: Boolean,
    val premiumExpiresAt: String?,
    val avatarUrl: String? = null,
    val points: Int? = null,
    val extras: ProviderUserExtras,
)

/** Account data only one provider has. */
sealed interface ProviderUserExtras {
    data class Torbox(
        val data: UserDataDto,
    ) : ProviderUserExtras

    data class RealDebrid(
        val type: String,
        val premiumSecondsLeft: Long,
        val locale: String?,
    ) : ProviderUserExtras
}

data class TorrentFullInfo(
    val name: String,
    val hash: String?,
    val size: Long,
    val trackers: List<String>,
    val seeds: Int,
    val peers: Int,
    val files: List<TorrentFullInfoFile>,
)

data class TorrentFullInfoFile(
    val name: String,
    val size: Long,
)

sealed interface ExportPayload {
    data class Magnet(
        val magnet: String,
    ) : ExportPayload

    class TorrentFile(
        val bytes: ByteArray,
    ) : ExportPayload
}

/** A generated download link. */
data class DebridLink(
    val url: String,
    /** a web page that streams the file in the browser, when the provider has one */
    val streamingPageUrl: String? = null,
    // details of the linked file, when the provider sends them
    val fileName: String? = null,
    val size: Long? = null,
    val mimeType: String? = null,
)

data class Page<T>(
    val items: List<T>,
    val totalCount: Int? = null,
)

enum class ItemKind { TORRENT, WEB, USENET }

data class AddedItem(
    val id: String,
    val kind: ItemKind,
)

data class AddOptions(
    val seeding: Int = 1,
    val allowZip: Boolean = true,
    val password: String? = null,
    val usenetOption: Int = -1,
)

/** Shared helpers used by both the mobile and TV screens. */
val DebridTorrent.needsFileSelection: Boolean get() = status == DownloadStatus.AWAITING_FILE_SELECTION

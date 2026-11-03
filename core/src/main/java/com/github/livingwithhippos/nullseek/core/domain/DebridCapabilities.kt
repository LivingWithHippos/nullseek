package com.github.livingwithhippos.nullseek.core.domain

/** What the active provider supports, used to hide the rest of the UI. */
data class DebridCapabilities(
    val usenet: Boolean,
    val webDownloads: Boolean,
    /** links can be zipped by the provider */
    val zipDownloads: Boolean,
    /** hashes can be checked for instant availability */
    val cacheCheck: Boolean,
    val exportTorrent: Boolean,
    val seedingOptions: Boolean,
    val allowZipOption: Boolean,
    val fullTorrentInfo: Boolean,
    val detailedStats: Boolean,
    /** the files to download must be picked after adding a torrent */
    val fileSelectionAfterAdd: Boolean,
    /** a single file of a torrent can be downloaded on its own */
    val perFileLinks: Boolean,
    /** a torrent can be added again to pick other files */
    val reselectFiles: Boolean,
    /** the generated download links include the API token, so they must not be shared */
    val linksContainToken: Boolean,
    /** the largest page the list endpoints accept, null when there is no limit */
    val maxPageSize: Int?,
    val accountUrl: String,
    val tokenLink: String,
    val subscriptionUrl: String,
    val subscriptionReferralUrl: String?,
    /** lowercase file extensions accepted when uploading */
    val supportedFileExtensions: Set<String>,
) {
    companion object {
        val Torbox =
            DebridCapabilities(
                usenet = true,
                webDownloads = true,
                zipDownloads = true,
                cacheCheck = true,
                exportTorrent = true,
                seedingOptions = true,
                allowZipOption = true,
                fullTorrentInfo = true,
                detailedStats = true,
                fileSelectionAfterAdd = false,
                perFileLinks = true,
                reselectFiles = false,
                linksContainToken = true,
                maxPageSize = null,
                accountUrl = "https://torbox.app/settings",
                tokenLink = "https://torbox.app/settings?section=account",
                subscriptionUrl = "https://torbox.app/subscription",
                subscriptionReferralUrl = "https://torbox.app/subscription?referral=dba208d7-f53e-4711-96db-97c7051799bc",
                supportedFileExtensions = setOf("torrent", "nzb"),
            )

        val RealDebrid =
            DebridCapabilities(
                usenet = false,
                webDownloads = true,
                zipDownloads = false,
                cacheCheck = false,
                exportTorrent = false,
                seedingOptions = false,
                allowZipOption = false,
                fullTorrentInfo = false,
                detailedStats = false,
                fileSelectionAfterAdd = true,
                perFileLinks = false,
                reselectFiles = true,
                linksContainToken = false,
                maxPageSize = 5000,
                accountUrl = "https://real-debrid.com/account",
                tokenLink = "https://real-debrid.com/apitoken",
                subscriptionUrl = "https://real-debrid.com/premium",
                subscriptionReferralUrl = null,
                supportedFileExtensions = setOf("torrent"),
            )

        fun of(kind: ProviderKind): DebridCapabilities =
            when (kind) {
                ProviderKind.TORBOX -> Torbox
                ProviderKind.REAL_DEBRID -> RealDebrid
            }
    }
}

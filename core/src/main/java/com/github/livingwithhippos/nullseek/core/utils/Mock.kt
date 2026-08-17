package com.github.livingwithhippos.nullseek.core.utils

import com.github.livingwithhippos.torbox.model.TorrentDto
import com.github.livingwithhippos.torbox.model.TorrentFileDto
import com.github.livingwithhippos.torbox.model.UserDataDto
import com.github.livingwithhippos.torbox.model.WebDownloadDto
import com.github.livingwithhippos.torbox.model.WebDownloadFileDto

val mockUser = UserDataDto(
    id = 1,
    authId = "123",
    createdAt = "2026-05-22T10:53:44Z",
    updatedAt = "2026-06-11T22:18:32Z",
    plan = 2,
    totalDownloaded = 5,
    customer = "123",
    isSubscribed = true,
    premiumExpiresAt = "2026-08-25T12:51:51Z",
    cooldownUntil = "2026-06-12T22:18:31Z",
    email = "cool@mail.com",
    userReferral = "123456",
    baseEmail = "cool@mail.com",
    totalBytesDownloaded = 55111926106,
    totalBytesUploaded = 288805503,
    torrentsDownloaded = 45,
    webDownloadsDownloaded = 4,
    usenetDownloadsDownloaded = 0,
    additionalConcurrentSlots = 0,
    longTermSeeding = false,
    longTermStorage = false,
    isVendor = false,
    vendorId = null,
    purchasesReferred = 0,
)

val mockTorrent = TorrentDto(
    id = 75418987,
    authId = "123-456-789",
    hash = "d3adb33f",
    createdAt = "2026-08-12T22:09:36Z",
    updatedAt = "2026-08-12T22:09:36Z",
    magnet = null,
    size = 5300108273L,
    active = false,
    downloadState = "cached",
    seeds = 0,
    peers = 0,
    ratio = 0.0,
    progress = 1.0,
    downloadSpeed = 0,
    uploadSpeed = 0,
    name = "Totally Cool Open Source Movie x265",
    eta = 0,
    server = 0,
    torrentFile = false,
    expiresAt = null,
    downloadPresent = true,
    downloadFinished = true,
    downloadPath = "d3adb33f",
    files = listOf(
        TorrentFileDto(
            id = 2,
            md5 = null,
            hash = "d3adb33f",
            name = "www.absolutelylegitsource.org    -    Totally Cool Open Source Movie x265/Totally Cool Open Source Movie x265.mkv",
            size = 5300107342L,
            zipped = false,
            s3Path = "d3adb33f/www.absolutelylegitsource.org    -    Totally Cool Open Source Movie x265/Totally Cool Open Source Movie x265.mkv",
            infected = false,
            mimetype = "video/x-matroska",
            shortName = "Totally Cool Open Source Movie x265.mkv",
            absolutePath = "/completed/d3adb33f/www.absolutelylegitsource.org    -    Totally Cool Open Source Movie x265/Totally Cool Open Source Movie x265.mkv",
            opensubtitlesHash = "6218572"
        ), TorrentFileDto(
            id = 0,
            md5 = null,
            hash = "d3adb33f",
            name = "www.absolutelylegitsource.org    -    Totally Cool Open Source Movie x265/Totally Cool Open Source Movie x265.mkv.nfo",
            size = 804L,
            zipped = false,
            s3Path = "d3adb33f/www.absolutelylegitsource.org    -    Totally Cool Open Source Movie x265/Totally Cool Open Source Movie x265.mkv.nfo",
            infected = false,
            mimetype = "text/plain",
            shortName = "Totally Cool Open Source Movie x265.mkv.nfo",
            absolutePath = "/completed/d3adb33f/www.absolutelylegitsource.org    -    Totally Cool Open Source Movie x265/Totally Cool Open Source Movie x265.mkv.nfo",
            opensubtitlesHash = null
        ), TorrentFileDto(
            id = 1,
            md5 = null,
            hash = "d3adb33f",
            name = "www.absolutelylegitsource.org    -    Totally Cool Open Source Movie x265/Torrent Downloaded From     absolutelylegitsource.org      .txt",
            size = 127L,
            zipped = false,
            s3Path = "d3adb33f/www.absolutelylegitsource.org    -    Totally Cool Open Source Movie x265/Torrent Downloaded From     absolutelylegitsource.org      .txt",
            infected = false,
            mimetype = "text/plain",
            shortName = "Torrent Downloaded From     absolutelylegitsource.org      .txt",
            absolutePath = "/completed/d3adb33f/www.absolutelylegitsource.org    -    Totally Cool Open Source Movie x265/Torrent Downloaded From     absolutelylegitsource.org      .txt",
            opensubtitlesHash = null
        )
    ),
    availability = 0F,
    tracker = null,
    totalUploaded = 0L,
    totalDownloaded = 0L,
    cached = true,
    owner = "987-654-321",
    seedTorrent = false,
    allowZipped = true,
    longTermSeeding = false,
    trackerMessage = null,
    cachedAt = "2026-06-13T08:02:01Z",
    isPrivate = false,
    alternativeHashes = listOf(
        "b33fd3ad", "d3adb33f"
    ),
    tags = emptyList()
)

val mockWebDownload: WebDownloadDto =  WebDownloadDto(
    id = 101,
    createdAt = "2026-05-22T14:19:46Z",
    updatedAt = "2026-07-29T01:08:18Z",
    authId = "123-456-789",
    name = "7zip.rar",
    hash = "76529d837695741",
    downloadState = "expired",
    downloadSpeed = 0,
    originalUrl = "https://downloadzz.net/file/76529d837695741/7zip.rar.html",
    eta = 0,
    progress = 1.0,
    size = 2205617365L,
    downloadId = null,
    files = listOf(
        WebDownloadFileDto(
            id = 0,
            md5 = null,
            hash = "76529d837695741",
            name = "7zip/7zip.rar",
            size = 2205617365L,
            zipped = false,
            s3Path = "76529d837695741/7zip/7zip.rar",
            infected = false,
            mimetype = "application/x-rar",
            shortName = "7zip.rar",
            absolutePath = "/app/downloads/76529d837695741/7zip/7zip.rar",
            opensubtitlesHash = null
        )
    ),
    active = false,
    cached = false,
    downloadPresent = false,
    downloadFinished = true,
    expiresAt = "2026-06-21T14:20:56Z",
    error = null,
    cachedAt = "2026-05-22T14:20:56Z",
    server = 50,
    alternativeHashes = listOf(
        "9a54d765c70f8dea",
        "2d19d0f03e54efda",
        "d1e1f702922e0c14"
    ),
    tags = listOf("test")
)
package com.github.livingwithhippos.nullseek.data.repository

import com.github.livingwithhippos.torbox.client.TorboxClient
import com.github.livingwithhippos.torbox.network.ApiResult

enum class LinkType { MAGNET, NZB, WEB }

fun linkType(link: String): LinkType =
    when {
        link.startsWith("magnet:", ignoreCase = true) -> LinkType.MAGNET
        link.endsWith(".nzb", ignoreCase = true) -> LinkType.NZB
        else -> LinkType.WEB
    }

/** Creates a download on TorBox from a link, picking the endpoint from the link type. */
class DownloadCreator(
    private val client: TorboxClient,
) {
    suspend fun createFromLink(
        link: String,
        password: String? = null,
        seedingOption: Int = DEFAULT_SEEDING_OPTION,
        usenetOption: Int = DEFAULT_USENET_OPTION,
        allowZipped: Boolean = true,
    ): ApiResult<*> =
        when (linkType(link)) {
            LinkType.MAGNET -> {
                client.torrent.create(magnet = link, seed = seedingOption, allowZip = allowZipped)
            }

            LinkType.NZB -> {
                client.usenet.create(link = link, password = password, postProcessing = usenetOption)
            }

            LinkType.WEB -> {
                client.web.create(link = link, password = password)
            }
        }

    companion object {
        const val DEFAULT_SEEDING_OPTION = 1
        const val DEFAULT_USENET_OPTION = -1
    }
}

fun ApiResult.Failure.describe(subject: String): String =
    when (this) {
        is ApiResult.Failure.Http -> "$subject: HTTP Error $code"
        is ApiResult.Failure.Network -> "$subject: Network Error - $cause"
        is ApiResult.Failure.BadRequest -> "$subject: Bad Request - $cause"
        is ApiResult.Failure.NoSuccess<*> -> "$subject: No Success"
        is ApiResult.Failure.Serialization -> "$subject: Serialization Error - $cause"
    }

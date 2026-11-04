package com.github.livingwithhippos.nullseek.data.repository

import com.github.livingwithhippos.nullseek.core.domain.AddOptions
import com.github.livingwithhippos.nullseek.core.domain.AddedItem
import com.github.livingwithhippos.nullseek.core.domain.ConversionOutcome
import com.github.livingwithhippos.nullseek.core.domain.DebridResult
import com.github.livingwithhippos.nullseek.core.domain.DebridService
import com.github.livingwithhippos.nullseek.core.domain.DebridServiceProvider
import com.github.livingwithhippos.nullseek.core.domain.awaitConversion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import logcat.logcat

enum class LinkType { MAGNET, NZB, WEB }

fun linkType(link: String): LinkType =
    when {
        link.startsWith("magnet:", ignoreCase = true) -> LinkType.MAGNET
        link.endsWith(".nzb", ignoreCase = true) -> LinkType.NZB
        else -> LinkType.WEB
    }

/** Creates a download on the active provider from a link, picking the endpoint from the link type. */
class DownloadCreator(
    private val debridProvider: DebridServiceProvider,
    private val backgroundScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    /**
     * @param autoSelectAll on providers that need the files picked after adding (Real-Debrid),
     * pick all of them in the background once the magnet is converted
     */
    suspend fun createFromLink(
        link: String,
        password: String? = null,
        seedingOption: Int = DEFAULT_SEEDING_OPTION,
        usenetOption: Int = DEFAULT_USENET_OPTION,
        allowZipped: Boolean = true,
        autoSelectAll: Boolean = false,
    ): DebridResult<AddedItem> {
        val service = debridProvider.current
        val options =
            AddOptions(seeding = seedingOption, allowZip = allowZipped, password = password, usenetOption = usenetOption)
        return when (linkType(link)) {
            LinkType.MAGNET -> {
                service.addMagnet(link, options).also { result ->
                    if (autoSelectAll && result is DebridResult.Ok) selectAllWhenReady(service, result.value.id)
                }
            }

            LinkType.NZB -> {
                service.addNzbLink(link, options)
            }

            LinkType.WEB -> {
                service.addWeb(link, password)
            }
        }
    }

    private fun selectAllWhenReady(
        service: DebridService,
        torrentId: String,
    ) {
        if (!service.capabilities.fileSelectionAfterAdd) return
        backgroundScope.launch {
            when (val outcome = service.awaitConversion(torrentId, timeoutMs = AUTO_SELECT_TIMEOUT_MS)) {
                is ConversionOutcome.NeedsSelection -> {
                    val result = service.selectFiles(torrentId, null)
                    logcat { "Selected all files of $torrentId: $result" }
                }

                else -> {
                    logcat { "Not selecting the files of $torrentId: $outcome" }
                }
            }
        }
    }

    companion object {
        const val DEFAULT_SEEDING_OPTION = 1
        const val DEFAULT_USENET_OPTION = -1
        private const val AUTO_SELECT_TIMEOUT_MS = 30_000L
    }
}

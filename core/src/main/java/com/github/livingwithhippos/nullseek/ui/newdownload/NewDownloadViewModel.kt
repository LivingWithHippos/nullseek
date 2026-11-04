package com.github.livingwithhippos.nullseek.ui.newdownload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.core.domain.AddOptions
import com.github.livingwithhippos.nullseek.core.domain.AddedItem
import com.github.livingwithhippos.nullseek.core.domain.ConversionOutcome
import com.github.livingwithhippos.nullseek.core.domain.DebridCapabilities
import com.github.livingwithhippos.nullseek.core.domain.DebridResult
import com.github.livingwithhippos.nullseek.core.domain.DebridServiceProvider
import com.github.livingwithhippos.nullseek.core.domain.ItemKind
import com.github.livingwithhippos.nullseek.core.domain.awaitConversion
import com.github.livingwithhippos.nullseek.core.domain.describe
import com.github.livingwithhippos.nullseek.data.repository.DownloadCreator
import com.github.livingwithhippos.nullseek.data.repository.PreferencesRepository
import com.github.livingwithhippos.nullseek.data.repository.SeedingOption
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.koin.core.annotation.KoinViewModel

sealed interface UploadState {
    data object Idle : UploadState

    data class Uploading(
        val done: Int = 0,
        val total: Int = 0,
    ) : UploadState

    /** waiting for the provider to read the torrents metadata */
    data class ResolvingFiles(
        val count: Int,
    ) : UploadState

    /** the user must pick the files of [selection] */
    data class AwaitingSelection(
        val selection: PendingSelection,
        val remaining: Int,
    ) : UploadState

    data class Finished(
        val successful: Int,
        val failed: Int,
        val errors: List<String>,
        /** torrents still waiting for their files to be picked */
        val awaitingSelection: Int = 0,
    ) : UploadState
}

/** true while the form must stay locked */
val UploadState.isBusy: Boolean
    get() = this is UploadState.Uploading || this is UploadState.ResolvingFiles || this is UploadState.AwaitingSelection

// how many torrents are polled at the same time while waiting for their metadata
private const val MAX_CONCURRENT_POLLS = 3

@KoinViewModel
class NewDownloadViewModel(
    private val debridProvider: DebridServiceProvider,
    private val downloadCreator: DownloadCreator,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {
    /** The seeding choice preselected in the form. */
    val defaultSeedingOption: StateFlow<SeedingOption> =
        preferencesRepository.defaultSeedingOption.stateIn(viewModelScope, SharingStarted.Eagerly, SeedingOption.AUTO)

    val capabilities: StateFlow<DebridCapabilities> =
        debridProvider.active
            .map { it?.capabilities ?: debridProvider.capabilities }
            .stateIn(viewModelScope, SharingStarted.Eagerly, debridProvider.capabilities)

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState = _uploadState.asStateFlow()

    private var pendingDecision: CompletableDeferred<SelectionDecision>? = null

    fun resetState() {
        _uploadState.value = UploadState.Idle
    }

    fun confirmSelection(fileIds: Set<Int>) = decide(SelectionDecision.Confirm(fileIds))

    fun skipSelection() = decide(SelectionDecision.Skip)

    fun selectAllRemaining() = decide(SelectionDecision.SelectAllRemaining)

    fun deleteSelection() = decide(SelectionDecision.Delete)

    private fun decide(decision: SelectionDecision) {
        pendingDecision?.complete(decision)
    }

    override fun onCleared() {
        // leave the remaining torrents waiting, they can be finished from their page
        pendingDecision?.complete(SelectionDecision.Skip)
        super.onCleared()
    }

    fun queueDownloads(
        links: List<String>,
        files: List<Pair<String, ByteArray>>,
        password: String?,
        seedingOption: Int,
        usenetOption: Int,
        allowZipped: Boolean,
    ) {
        if (_uploadState.value.isBusy) return
        viewModelScope.launch {
            val service = debridProvider.current
            val capabilities = service.capabilities
            val total = links.size + files.size
            _uploadState.value = UploadState.Uploading(0, total)

            var successCount = 0
            var failCount = 0
            val errorMessages = mutableListOf<String>()
            val addedTorrents = mutableListOf<AddedItem>()
            var done = 0

            fun record(
                result: DebridResult<AddedItem>,
                subject: String,
            ) {
                when (result) {
                    is DebridResult.Ok -> {
                        successCount++
                        if (result.value.kind == ItemKind.TORRENT) addedTorrents.add(result.value)
                    }

                    is DebridResult.Err -> {
                        failCount++
                        errorMessages.add(result.error.describe(subject))
                    }
                }
                done++
                _uploadState.value = UploadState.Uploading(done, total)
            }

            for (link in links) {
                val result =
                    downloadCreator.createFromLink(
                        link = link,
                        password = password,
                        seedingOption = seedingOption,
                        usenetOption = usenetOption,
                        allowZipped = allowZipped,
                    )
                record(result, "Link ($link)")
            }

            val options =
                AddOptions(seeding = seedingOption, allowZip = allowZipped, password = password, usenetOption = usenetOption)
            for ((name, bytes) in files) {
                val result =
                    when {
                        name.endsWith(".torrent", ignoreCase = true) -> {
                            service.addTorrentFile(name, bytes, options)
                        }

                        name.endsWith(".nzb", ignoreCase = true) -> {
                            service.addNzbFile(name, bytes, options)
                        }

                        else -> {
                            failCount++
                            errorMessages.add("File ($name): Unsupported file type")
                            done++
                            continue
                        }
                    }
                record(result, "File ($name)")
            }

            var awaiting = 0
            if (capabilities.fileSelectionAfterAdd && addedTorrents.isNotEmpty()) {
                _uploadState.value = UploadState.ResolvingFiles(addedTorrents.size)
                val semaphore = Semaphore(MAX_CONCURRENT_POLLS)
                val outcomes =
                    coroutineScope {
                        addedTorrents
                            .map { added ->
                                async { semaphore.withPermit { service.awaitConversion(added.id) } }
                            }.awaitAll()
                    }

                val toSelect = mutableListOf<PendingSelection>()
                outcomes.forEach { outcome ->
                    when (outcome) {
                        is ConversionOutcome.NeedsSelection -> {
                            toSelect.add(
                                PendingSelection(outcome.torrent.id, outcome.torrent.name, outcome.torrent.files),
                            )
                        }

                        is ConversionOutcome.Started -> {
                            Unit
                        }

                        is ConversionOutcome.Failed -> {
                            successCount--
                            failCount++
                            errorMessages.add("Torrent: ${outcome.reason}")
                        }

                        ConversionOutcome.TimedOut -> {
                            awaiting++
                        }
                    }
                }

                val summary =
                    FileSelectionQueue(
                        select = { id, ids -> service.selectFiles(id, ids) },
                        delete = { id -> service.deleteTorrent(id) },
                        ask = { item, remaining ->
                            val deferred = CompletableDeferred<SelectionDecision>()
                            pendingDecision = deferred
                            _uploadState.value = UploadState.AwaitingSelection(item, remaining)
                            deferred.await().also { pendingDecision = null }
                        },
                    ).process(toSelect)

                awaiting += summary.skipped
                successCount -= summary.deleted + summary.errors.size
                failCount += summary.errors.size
                errorMessages.addAll(summary.errors)
            }

            _uploadState.value = UploadState.Finished(successCount, failCount, errorMessages, awaiting)
        }
    }
}

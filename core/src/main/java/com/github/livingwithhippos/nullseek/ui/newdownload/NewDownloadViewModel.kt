package com.github.livingwithhippos.nullseek.ui.newdownload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.data.repository.DownloadCreator
import com.github.livingwithhippos.nullseek.data.repository.PreferencesRepository
import com.github.livingwithhippos.nullseek.data.repository.SeedingOption
import com.github.livingwithhippos.nullseek.data.repository.describe
import com.github.livingwithhippos.torbox.client.TorboxClient
import com.github.livingwithhippos.torbox.network.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

sealed interface UploadState {
    data object Idle : UploadState

    data object Uploading : UploadState

    data class Finished(
        val successful: Int,
        val failed: Int,
        val errors: List<String>,
    ) : UploadState
}

@KoinViewModel
class NewDownloadViewModel(
    private val client: TorboxClient,
    private val downloadCreator: DownloadCreator,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {
    /** The seeding choice preselected in the form. */
    val defaultSeedingOption: StateFlow<SeedingOption> =
        preferencesRepository.defaultSeedingOption.stateIn(viewModelScope, SharingStarted.Eagerly, SeedingOption.AUTO)

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState = _uploadState.asStateFlow()

    fun resetState() {
        _uploadState.value = UploadState.Idle
    }

    fun queueDownloads(
        links: List<String>,
        files: List<Pair<String, ByteArray>>,
        password: String?,
        seedingOption: Int,
        usenetOption: Int,
        allowZipped: Boolean,
    ) {
        viewModelScope.launch {
            _uploadState.value = UploadState.Uploading

            var successCount = 0
            var failCount = 0
            val errorMessages = mutableListOf<String>()

            for (link in links) {
                val result =
                    downloadCreator.createFromLink(
                        link = link,
                        password = password,
                        seedingOption = seedingOption,
                        usenetOption = usenetOption,
                        allowZipped = allowZipped,
                    )

                if (result is ApiResult.Failure) {
                    failCount++
                    errorMessages.add(result.describe("Link ($link)"))
                } else {
                    successCount++
                }
            }

            for ((name, bytes) in files) {
                val result =
                    when {
                        name.endsWith(".torrent", ignoreCase = true) -> {
                            client.torrent.create(
                                fileBytes = bytes,
                                fileName = name,
                                seed = seedingOption,
                                allowZip = allowZipped,
                            )
                        }

                        name.endsWith(".nzb", ignoreCase = true) -> {
                            client.usenet.create(
                                fileBytes = bytes,
                                fileName = name,
                                password = password,
                                postProcessing = usenetOption,
                            )
                        }

                        else -> {
                            failCount++
                            errorMessages.add("File ($name): Unsupported file type")
                            continue
                        }
                    }
                if (result is ApiResult.Failure) {
                    failCount++
                    when (result) {
                        is ApiResult.Failure.Http -> {
                            errorMessages.add("File ($name): HTTP Error ${result.code}")
                        }

                        is ApiResult.Failure.Network -> {
                            errorMessages.add("File ($name): Network Error - ${result.cause}")
                        }

                        is ApiResult.Failure.BadRequest -> {
                            errorMessages.add("File ($name): Bad Request - ${result.cause}")
                        }

                        is ApiResult.Failure.NoSuccess<*> -> {
                            errorMessages.add("File ($name): No Success")
                        }

                        is ApiResult.Failure.Serialization -> {
                            errorMessages.add("File ($name): Serialization Error - ${result.cause}")
                        }
                    }
                } else {
                    successCount++
                }
            }

            _uploadState.value = UploadState.Finished(successCount, failCount, errorMessages)
        }
    }
}

package com.github.livingwithhippos.nullseek.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.core.data.database.model.StremioAddonEntity
import com.github.livingwithhippos.nullseek.core.data.stremio.AddonScrapedItem
import com.github.livingwithhippos.nullseek.core.data.stremio.AddonSearchHelper
import com.github.livingwithhippos.nullseek.core.data.stremio.ImdbTitleResult
import com.github.livingwithhippos.nullseek.core.data.stremio.ImdbUtils
import com.github.livingwithhippos.nullseek.core.data.stremio.SeriesEpisodeItem
import com.github.livingwithhippos.nullseek.data.model.ExternalService
import com.github.livingwithhippos.nullseek.data.model.ExternalServiceCategory
import com.github.livingwithhippos.nullseek.data.model.Indexer
import com.github.livingwithhippos.nullseek.data.model.IndexerSearchOutcome
import com.github.livingwithhippos.nullseek.data.model.SearchResult
import com.github.livingwithhippos.nullseek.data.model.hasDownloadLink
import com.github.livingwithhippos.nullseek.data.model.markCached
import com.github.livingwithhippos.nullseek.data.model.torrentHash
import com.github.livingwithhippos.nullseek.data.repository.CacheChecker
import com.github.livingwithhippos.nullseek.data.repository.DownloadCreator
import com.github.livingwithhippos.nullseek.data.repository.ExternalServicesRepository
import com.github.livingwithhippos.nullseek.data.repository.IndexersRepository
import com.github.livingwithhippos.nullseek.data.repository.PreferencesRepository
import com.github.livingwithhippos.torbox.network.ApiResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.milliseconds

enum class SearchMode { INDEXERS, STREMIO_ADDONS }

enum class SearchSortOption { RELEVANCE, SIZE, SEEDERS, PEERS, AGE, NAME }

val STREMIO_SORT_OPTIONS =
    listOf(
        SearchSortOption.RELEVANCE,
        SearchSortOption.SIZE,
        SearchSortOption.NAME,
    )

data class SearchSort(
    val option: SearchSortOption = SearchSortOption.RELEVANCE,
    val ascending: Boolean = false,
)

sealed interface SearchStatus {
    data object Idle : SearchStatus

    data class Loading(
        val completed: Int,
        val total: Int,
    ) : SearchStatus

    data object Results : SearchStatus
}

enum class StremioStage {
    IDLE,
    TITLES,
    EPISODES,
    STREAMS,
}

data class StremioSearchState(
    val stage: StremioStage = StremioStage.IDLE,
    val query: String = "",
    val isSearchingTitles: Boolean = false,
    val titles: List<ImdbTitleResult> = emptyList(),
    val selectedTitle: ImdbTitleResult? = null,
    val isLoadingEpisodes: Boolean = false,
    val episodes: List<SeriesEpisodeItem> = emptyList(),
    val selectedSeason: Int = 1,
    val selectedEpisode: SeriesEpisodeItem? = null,
    val activeStreamImdbId: String = "",
    val activeStreamLabel: String = "",
    val isLoadingStreams: Boolean = false,
    val streamResults: List<SearchResult> = emptyList(),
    val filterQuery: String = "",
    val showFilterBar: Boolean = true,
    val sort: SearchSort = SearchSort(),
) {
    val availableSeasons: List<Int>
        get() =
            episodes
                .map { it.season }
                .distinct()
                .sortedWith(compareBy { if (it == 0) Int.MAX_VALUE else it })

    val episodesForSelectedSeason: List<SeriesEpisodeItem>
        get() = episodes.filter { it.season == selectedSeason }.sortedBy { it.episode }

    val filteredStreamResults: List<SearchResult>
        get() {
            val activeFilter = filterQuery.trim()
            val filtered =
                if (showFilterBar && activeFilter.isNotEmpty()) {
                    val tokens = activeFilter.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
                    streamResults.filter { result ->
                        val target = "${result.title} ${result.indexerName}".lowercase()
                        tokens.all { token -> target.contains(token) }
                    }
                } else {
                    streamResults
                }
            return filtered.applySort(sort)
        }

    val canNavigateBack: Boolean
        get() =
            when (stage) {
                StremioStage.STREAMS -> selectedTitle != null || titles.isNotEmpty()
                StremioStage.EPISODES -> titles.isNotEmpty()
                else -> false
            }
}

/** Raw, unsorted progress of the current search. */
data class SearchProgress(
    val query: String = "",
    val total: Int = 0,
    val outcomes: List<IndexerSearchOutcome> = emptyList(),
    val started: Boolean = false,
    val addonResults: List<SearchResult> = emptyList(),
) {
    val completed: Int get() = outcomes.size
    val isLoading: Boolean get() = started && completed < total
}

data class SearchUiState(
    val sort: SearchSort = SearchSort(),
    val activeIndexers: List<Indexer> = emptyList(),
    val status: SearchStatus = SearchStatus.Idle,
    val results: List<SearchResult> = emptyList(),
    val failedIndexers: List<String> = emptyList(),
    val searchMode: SearchMode = SearchMode.INDEXERS,
    val activeAddons: List<StremioAddonEntity> = emptyList(),
    val stremioState: StremioSearchState = StremioSearchState(),
    val searchHistory: List<String> = emptyList(),
) {
    val hasActiveIndexers: Boolean get() = activeIndexers.isNotEmpty()
    val hasActiveAddons: Boolean get() = activeAddons.isNotEmpty()
    val canSearch: Boolean
        get() =
            when (searchMode) {
                SearchMode.INDEXERS -> hasActiveIndexers
                SearchMode.STREMIO_ADDONS -> hasActiveAddons
            }
}

fun List<Indexer>.activeOnly(): List<Indexer> = filter { it.active }

/** Relevance keeps the indexers order, the direction only applies to the other options. */
fun List<SearchResult>.applySort(sort: SearchSort): List<SearchResult> {
    val sorted =
        when (sort.option) {
            SearchSortOption.RELEVANCE -> return this
            SearchSortOption.SIZE -> sortedBy { it.sizeBytes }
            SearchSortOption.SEEDERS -> sortedBy { it.seeders }
            SearchSortOption.PEERS -> sortedBy { it.peers }
            SearchSortOption.AGE -> sortedBy { it.publishedAt ?: 0L }
            SearchSortOption.NAME -> sortedBy { it.title.lowercase() }
        }
    return if (sort.ascending) sorted else sorted.reversed()
}

internal fun AddonScrapedItem.toSearchResultOrNull(): SearchResult? {
    val magnetLink = scrapedItem.magnets.firstOrNull()
    val downloadLink = scrapedItem.hosting.firstOrNull()
    if (magnetLink == null && downloadLink == null) return null
    val isDirectStream = magnetLink.isNullOrBlank() && !downloadLink.isNullOrBlank()
    return SearchResult(
        title = scrapedItem.name,
        indexerName = addonName.ifBlank { "Stremio Addons" },
        sizeBytes = scrapedItem.parsedSize?.toLong() ?: 0L,
        seeders = scrapedItem.seeders?.toIntOrNull() ?: 0,
        peers = scrapedItem.leechers?.toIntOrNull() ?: 0,
        publishedAt = null,
        magnetLink = magnetLink,
        downloadLink = downloadLink,
        infoUrl = if (isDirectStream) null else scrapedItem.link,
        categories = categories,
        isDirectStream = isDirectStream,
        isFromStremioAddon = true,
    )
}

/** Combines the per-indexer outcomes, in indexers order, into a single sorted state. */
fun buildSearchState(
    progress: SearchProgress,
    sort: SearchSort,
    active: List<Indexer>,
    searchMode: SearchMode = SearchMode.INDEXERS,
    activeAddons: List<StremioAddonEntity> = emptyList(),
    stremioState: StremioSearchState = StremioSearchState(),
    cached: Set<String> = emptySet(),
): SearchUiState {
    val order = progress.outcomes.sortedBy { o -> active.indexOfFirst { it.id == o.indexer.id }.let { if (it < 0) Int.MAX_VALUE else it } }
    val results =
        order
            .asSequence()
            .filterIsInstance<IndexerSearchOutcome.Success>()
            .flatMap { it.results }
            .filter { it.hasDownloadLink() }
            .plus(progress.addonResults)
            .map { it.markCached(cached) }
            .toList()
            .applySort(sort)
    val failed = order.filterIsInstance<IndexerSearchOutcome.Failure>().map { it.indexer.name }
    val status =
        when {
            !progress.started -> SearchStatus.Idle
            progress.isLoading -> SearchStatus.Loading(progress.completed, progress.total)
            else -> SearchStatus.Results
        }
    val sortedStremioState =
        stremioState.copy(
            streamResults = stremioState.streamResults.applySort(stremioState.sort),
        )
    return SearchUiState(
        sort = sort,
        activeIndexers = active,
        status = status,
        results = results,
        failedIndexers = failed,
        searchMode = searchMode,
        activeAddons = activeAddons,
        stremioState = sortedStremioState,
    )
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@KoinViewModel
class SearchViewModel(
    private val indexersRepository: IndexersRepository,
    private val preferencesRepository: PreferencesRepository,
    private val downloadCreator: DownloadCreator,
    private val addonSearchHelper: AddonSearchHelper,
    private val externalServicesRepository: ExternalServicesRepository,
    private val cacheChecker: CacheChecker,
) : ViewModel() {
    val autoAddDownload: StateFlow<Boolean> =
        preferencesRepository.searchAutoAddDownload.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            false,
        )

    val defaultMediaPlayer: StateFlow<ExternalService?> =
        externalServicesRepository.defaultMediaPlayer.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val mediaPlayerServices: StateFlow<List<ExternalService>> =
        externalServicesRepository.mediaPlayerServices.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _addingDownload = MutableStateFlow(false)
    val addingDownload: StateFlow<Boolean> = _addingDownload

    private val _downloadEvents = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)

    /** Emits true when the download was added, false on failure. */
    val downloadEvents: SharedFlow<Boolean> = _downloadEvents

    private val _playbackEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val playbackEvents: SharedFlow<String> = _playbackEvents

    private val sort: StateFlow<SearchSort> =
        combine(
            preferencesRepository.searchSortOption,
            preferencesRepository.searchSortAscending,
        ) { option, ascending ->
            SearchSort(
                option = SearchSortOption.entries.firstOrNull { it.name == option } ?: SearchSortOption.RELEVANCE,
                ascending = ascending,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, SearchSort())

    private val stremioSort: StateFlow<SearchSort> =
        combine(
            preferencesRepository.stremioSortOption,
            preferencesRepository.stremioSortAscending,
        ) { option, ascending ->
            SearchSort(
                option = STREMIO_SORT_OPTIONS.firstOrNull { it.name == option } ?: SearchSortOption.RELEVANCE,
                ascending = ascending,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, SearchSort())

    private val stremioShowFilterBar: StateFlow<Boolean> =
        preferencesRepository.stremioShowFilterBar.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            true,
        )

    private val searchMode: StateFlow<SearchMode> =
        preferencesRepository.searchMode
            .map { modeName ->
                SearchMode.entries.firstOrNull { it.name == modeName } ?: SearchMode.INDEXERS
            }.stateIn(viewModelScope, SharingStarted.Eagerly, SearchMode.INDEXERS)

    private val progress = MutableStateFlow(SearchProgress())

    private val checkCache: StateFlow<Boolean> =
        preferencesRepository.searchCheckCache.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** Whether the IMDb details card starts expanded. */
    val imdbDetailsExpanded: StateFlow<Boolean> =
        preferencesRepository.imdbDetailsExpanded.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Torrent hashes TorBox reported as cached; empty while the check is turned off.
    private val cachedHashes = MutableStateFlow<Set<String>>(emptySet())
    private val visibleCachedHashes =
        combine(cachedHashes, checkCache) { cached, enabled -> if (enabled) cached else emptySet() }
    private val stremioState = MutableStateFlow(StremioSearchState())
    private var stremioJob: Job? = null

    private val _selectedResult = MutableStateFlow<SearchResult?>(null)
    val selectedResult: StateFlow<SearchResult?> = _selectedResult
    private val errorsDismissed = MutableStateFlow(false)
    private val submissions =
        MutableSharedFlow<String>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val uiState: StateFlow<SearchUiState> =
        combine(
            combine(
                sort,
                indexersRepository.indexers.map { it.activeOnly() },
                progress,
                errorsDismissed,
                visibleCachedHashes,
            ) { s, active, p, dismissed, cached ->
                val base = buildSearchState(p, s, active, cached = cached)
                if (dismissed) base.copy(failedIndexers = emptyList()) else base
            },
            searchMode,
            addonSearchHelper.getEnabledAddons(),
            combine(stremioState, stremioSort, stremioShowFilterBar, visibleCachedHashes) { stremio, sSort, showFilter, cached ->
                stremio.copy(
                    sort = sSort,
                    showFilterBar = showFilter,
                    streamResults = stremio.streamResults.map { it.markCached(cached) }.applySort(sSort),
                )
            },
            preferencesRepository.searchHistory,
        ) { baseState, mode, addons, stremio, history ->
            baseState.copy(
                searchMode = mode,
                activeAddons = addons,
                stremioState = stremio,
                searchHistory = history,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    init {
        // Bursts of submits are coalesced; flatMapLatest cancels the in-flight indexer search (and its HTTP calls).
        submissions
            .debounce(SEARCH_DEBOUNCE_MS.milliseconds)
            .filter { it.isNotBlank() }
            .flatMapLatest { query -> indexerSearchFlow(query) }
            .onEach { progress.value = it }
            .launchIn(viewModelScope)

        // Indexer, addon and Stremio stream results, checked in batches as they arrive.
        combine(
            progress,
            stremioState.map { it.streamResults }.distinctUntilChanged(),
            checkCache,
        ) { p, streams, enabled ->
            if (enabled) {
                p.outcomes.filterIsInstance<IndexerSearchOutcome.Success>().flatMap { it.results } + p.addonResults + streams
            } else {
                emptyList()
            }
        }.debounce(CACHE_CHECK_DEBOUNCE_MS.milliseconds)
            .distinctUntilChanged()
            .filter { it.isNotEmpty() }
            .mapLatest { results -> checkCached(results) }
            .onEach { found -> cachedHashes.update { it + found } }
            .launchIn(viewModelScope)
    }

    private suspend fun checkCached(results: List<SearchResult>): Set<String> =
        cacheChecker.cachedTorrents(results.mapNotNull { it.torrentHash() }.toSet())

    private fun indexerSearchFlow(query: String) =
        flow {
            val active = indexersRepository.getAll().activeOnly()
            if (active.isEmpty()) return@flow
            errorsDismissed.value = false
            var current = SearchProgress(query, active.size, emptyList(), started = true)
            emit(current)

            indexersRepository.search(active, query).collect { outcome ->
                current = current.copy(outcomes = current.outcomes + outcome)
                emit(current)
            }
        }

    private fun executeStremioQuery(query: String) {
        stremioJob?.cancel()
        val directImdbId = ImdbUtils.extractImdbId(query)
        if (directImdbId != null) {
            stremioState.value = StremioSearchState(query = query)
            fetchStremioStreams(
                imdbId = directImdbId,
                type = ImdbUtils.detectContentType(directImdbId),
                contentName = directImdbId,
                selectedTitle = null,
                selectedEpisode = null,
                streamLabel = directImdbId,
            )
        } else {
            stremioState.value =
                StremioSearchState(
                    stage = StremioStage.TITLES,
                    query = query,
                    isSearchingTitles = true,
                )
            stremioJob =
                viewModelScope.launch {
                    val titles =
                        addonSearchHelper
                            .searchTitles(query)
                            .getOrDefault(emptyList())
                    stremioState.update {
                        it.copy(
                            isSearchingTitles = false,
                            titles = titles,
                        )
                    }
                }
        }
    }

    fun selectImdbTitle(title: ImdbTitleResult) {
        stremioJob?.cancel()
        if (title.isSeries) {
            stremioState.update {
                it.copy(
                    stage = StremioStage.EPISODES,
                    selectedTitle = title,
                    selectedEpisode = null,
                    isLoadingEpisodes = true,
                    episodes = emptyList(),
                    selectedSeason = 1,
                )
            }
            stremioJob =
                viewModelScope.launch {
                    val meta =
                        addonSearchHelper
                            .getTitleMeta(title.imdbId, "series", existing = title)
                            .getOrNull()
                    val episodes = meta?.episodes.orEmpty()
                    val enrichedTitle = meta?.title ?: title
                    val firstSeason =
                        episodes.firstOrNull { it.season > 0 }?.season
                            ?: episodes.firstOrNull()?.season
                            ?: 1
                    stremioState.update {
                        it.copy(
                            isLoadingEpisodes = false,
                            selectedTitle = enrichedTitle,
                            episodes = episodes,
                            selectedSeason = firstSeason,
                        )
                    }
                }
        } else {
            fetchStremioStreams(
                imdbId = title.imdbId,
                type = title.type,
                contentName = title.title,
                selectedTitle = title,
                selectedEpisode = null,
                streamLabel = buildTitleSummary(title),
            )
        }
    }

    fun selectSeason(season: Int) {
        stremioState.update { it.copy(selectedSeason = season) }
    }

    fun selectSeriesEpisode(episode: SeriesEpisodeItem) {
        val currentTitle = stremioState.value.selectedTitle ?: return
        val episodeCode = "S%02dE%02d".format(episode.season, episode.episode)
        fetchStremioStreams(
            imdbId = episode.id,
            type = "series",
            contentName = "${currentTitle.title} $episodeCode",
            selectedTitle = currentTitle,
            selectedEpisode = episode,
            streamLabel = "$episodeCode · ${episode.title}",
        )
    }

    fun searchManualSeriesEpisode(
        season: Int,
        episode: Int,
    ) {
        if (season < 0 || episode <= 0) return
        val currentTitle = stremioState.value.selectedTitle ?: return
        val manualEpisode =
            SeriesEpisodeItem(
                id = "${currentTitle.imdbId}:$season:$episode",
                season = season,
                episode = episode,
                title = "Episode $episode",
            )
        selectSeriesEpisode(manualEpisode)
    }

    fun searchWholeSeries() {
        val currentTitle = stremioState.value.selectedTitle ?: return
        fetchStremioStreams(
            imdbId = currentTitle.imdbId,
            type = "series",
            contentName = currentTitle.title,
            selectedTitle = currentTitle,
            selectedEpisode = null,
            streamLabel = buildTitleSummary(currentTitle),
        )
    }

    fun navigateBackInStremio() {
        stremioJob?.cancel()
        stremioState.update { current ->
            when (current.stage) {
                StremioStage.STREAMS -> {
                    if (current.selectedTitle?.isSeries == true) {
                        current.copy(
                            stage = StremioStage.EPISODES,
                            isLoadingStreams = false,
                            selectedEpisode = null,
                            streamResults = emptyList(),
                            filterQuery = "",
                        )
                    } else if (current.titles.isNotEmpty()) {
                        current.copy(
                            stage = StremioStage.TITLES,
                            isLoadingStreams = false,
                            selectedTitle = null,
                            selectedEpisode = null,
                            streamResults = emptyList(),
                            filterQuery = "",
                        )
                    } else {
                        StremioSearchState()
                    }
                }

                StremioStage.EPISODES -> {
                    if (current.titles.isNotEmpty()) {
                        current.copy(
                            stage = StremioStage.TITLES,
                            isLoadingEpisodes = false,
                            selectedTitle = null,
                            episodes = emptyList(),
                        )
                    } else {
                        StremioSearchState()
                    }
                }

                else -> {
                    StremioSearchState()
                }
            }
        }
    }

    private fun fetchStremioStreams(
        imdbId: String,
        type: String?,
        contentName: String,
        selectedTitle: ImdbTitleResult?,
        selectedEpisode: SeriesEpisodeItem?,
        streamLabel: String,
    ) {
        stremioJob?.cancel()
        stremioState.update {
            it.copy(
                stage = StremioStage.STREAMS,
                selectedTitle = selectedTitle,
                selectedEpisode = selectedEpisode,
                activeStreamImdbId = imdbId,
                activeStreamLabel = streamLabel,
                isLoadingStreams = true,
                streamResults = emptyList(),
                filterQuery = "",
            )
        }
        stremioJob =
            viewModelScope.launch {
                val needsMeta =
                    selectedTitle == null ||
                        (selectedTitle.description.isNullOrBlank() && selectedTitle.imdbRating.isNullOrBlank())
                if (needsMeta) {
                    launch {
                        val primaryType =
                            type
                                ?: selectedTitle?.type
                                ?: ImdbUtils.detectContentType(imdbId)
                                ?: "movie"
                        val meta =
                            addonSearchHelper
                                .getTitleMeta(imdbId, primaryType, existing = selectedTitle)
                                .getOrNull()
                                ?: if (type == null && selectedTitle == null && primaryType == "movie") {
                                    addonSearchHelper
                                        .getTitleMeta(imdbId, "series", existing = null)
                                        .getOrNull()
                                } else {
                                    null
                                }
                        if (meta != null) {
                            stremioState.update { current ->
                                current.copy(
                                    selectedTitle = meta.title,
                                    activeStreamLabel =
                                        if (current.selectedEpisode == null) {
                                            buildTitleSummary(meta.title)
                                        } else {
                                            current.activeStreamLabel
                                        },
                                )
                            }
                        }
                    }
                }

                val results =
                    addonSearchHelper
                        .searchStreamsWithAddonInfo(
                            query = imdbId,
                            type = type,
                            contentName = contentName,
                        ).getOrDefault(emptyList())
                        .mapNotNull { it.toSearchResultOrNull() }
                stremioState.update {
                    it.copy(
                        isLoadingStreams = false,
                        streamResults = results,
                    )
                }
            }
    }

    private fun buildTitleSummary(title: ImdbTitleResult): String =
        buildString {
            append(title.title)
            title.year?.let { append(" (").append(it).append(")") }
        }

    fun playOnMedia(
        service: ExternalService,
        url: String,
        setAsDefault: Boolean = false,
    ) {
        viewModelScope.launch {
            if (setAsDefault) {
                externalServicesRepository.setDefault(
                    service.id,
                    ExternalServiceCategory.MEDIA_PLAYER,
                )
            }
            _playbackEvents.tryEmit("Sending to ${service.name}…")
            val result = externalServicesRepository.playMedia(service, url)
            result.fold(
                onSuccess = {
                    _playbackEvents.tryEmit("Playing on ${service.name}")
                },
                onFailure = { error ->
                    _playbackEvents.tryEmit("Failed to play on ${service.name}: ${error.message}")
                },
            )
        }
    }

    fun setSearchMode(mode: SearchMode) {
        viewModelScope.launch { preferencesRepository.setSearchMode(mode.name) }
    }

    fun setStremioFilterQuery(query: String) {
        stremioState.update { it.copy(filterQuery = query) }
    }

    fun setStremioShowFilterBar(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setStremioShowFilterBar(enabled) }
    }

    fun setStremioSortOption(option: SearchSortOption) {
        viewModelScope.launch { preferencesRepository.setStremioSortOption(option.name) }
    }

    fun setStremioAscending(ascending: Boolean) {
        viewModelScope.launch { preferencesRepository.setStremioSortAscending(ascending) }
    }

    fun setSortOption(option: SearchSortOption) {
        viewModelScope.launch { preferencesRepository.setSearchSortOption(option.name) }
    }

    fun setAscending(ascending: Boolean) {
        viewModelScope.launch { preferencesRepository.setSearchSortAscending(ascending) }
    }

    fun selectResult(result: SearchResult) {
        _selectedResult.value = result
    }

    fun closeDetails() {
        _selectedResult.value = null
    }

    fun addDownload(link: String) {
        if (!_addingDownload.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            val result = runCatching { downloadCreator.createFromLink(link) }.getOrNull()
            _addingDownload.value = false
            _downloadEvents.tryEmit(result != null && result !is ApiResult.Failure)
        }
    }

    fun dismissErrors() {
        errorsDismissed.value = true
    }

    fun onSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { preferencesRepository.addSearchHistoryQuery(trimmed) }
        when (searchMode.value) {
            SearchMode.INDEXERS -> submissions.tryEmit(trimmed)
            SearchMode.STREMIO_ADDONS -> executeStremioQuery(trimmed)
        }
    }

    fun removeSearchHistoryItem(query: String) {
        viewModelScope.launch { preferencesRepository.removeSearchHistoryQuery(query) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { preferencesRepository.clearSearchHistory() }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        const val CACHE_CHECK_DEBOUNCE_MS = 300L
    }
}

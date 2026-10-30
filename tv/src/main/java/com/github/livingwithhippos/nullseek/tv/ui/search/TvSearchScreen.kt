package com.github.livingwithhippos.nullseek.tv.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.FilterChip
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.core.data.stremio.ImdbTitleResult
import com.github.livingwithhippos.nullseek.core.data.stremio.SeriesEpisodeItem
import com.github.livingwithhippos.nullseek.core.utils.convertToSize
import com.github.livingwithhippos.nullseek.data.model.SearchResult
import com.github.livingwithhippos.nullseek.tv.ui.components.Badge
import com.github.livingwithhippos.nullseek.tv.ui.components.ConfirmDialog
import com.github.livingwithhippos.nullseek.tv.ui.components.EmptyState
import com.github.livingwithhippos.nullseek.tv.ui.components.ErrorBanner
import com.github.livingwithhippos.nullseek.tv.ui.components.LoadingBox
import com.github.livingwithhippos.nullseek.tv.ui.components.LocalMessenger
import com.github.livingwithhippos.nullseek.tv.ui.components.SectionCard
import com.github.livingwithhippos.nullseek.tv.ui.components.TvActionButton
import com.github.livingwithhippos.nullseek.tv.ui.components.TvTextField
import com.github.livingwithhippos.nullseek.ui.search.STREMIO_SORT_OPTIONS
import com.github.livingwithhippos.nullseek.ui.search.SearchMode
import com.github.livingwithhippos.nullseek.ui.search.SearchSort
import com.github.livingwithhippos.nullseek.ui.search.SearchSortOption
import com.github.livingwithhippos.nullseek.ui.search.SearchStatus
import com.github.livingwithhippos.nullseek.ui.search.SearchUiState
import com.github.livingwithhippos.nullseek.ui.search.SearchViewModel
import com.github.livingwithhippos.nullseek.ui.search.StremioSearchState
import com.github.livingwithhippos.nullseek.ui.search.StremioStage
import org.koin.androidx.compose.koinViewModel

@Composable
fun TvSearchScreen(
    canAddDownloads: Boolean,
    onDownloadLink: (String) -> Unit,
    onOpenExternalServices: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val messenger = LocalMessenger.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selected by viewModel.selectedResult.collectAsStateWithLifecycle()
    val autoAdd by viewModel.autoAddDownload.collectAsStateWithLifecycle()
    val adding by viewModel.addingDownload.collectAsStateWithLifecycle()
    val defaultPlayer by viewModel.defaultMediaPlayer.collectAsStateWithLifecycle()
    val mediaPlayers by viewModel.mediaPlayerServices.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(viewModel) {
        viewModel.downloadEvents.collect { added ->
            messenger.show(context.getString(if (added) R.string.search_download_added else R.string.search_download_failed))
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.playbackEvents.collect { messenger.show(it) }
    }

    val stremio = state.stremioState
    BackHandler(enabled = selected != null) { viewModel.closeDetails() }
    BackHandler(enabled = selected == null && state.searchMode == SearchMode.STREMIO_ADDONS && stremio.canNavigateBack) {
        viewModel.navigateBackInStremio()
    }

    val search = {
        viewModel.closeDetails()
        viewModel.onSearch(query)
    }

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // mode, query and history
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SearchMode.entries.forEach { mode ->
                FilterChip(selected = state.searchMode == mode, onClick = { viewModel.setSearchMode(mode) }) {
                    Text(
                        stringResource(
                            if (mode == SearchMode.INDEXERS) R.string.search_mode_indexers else R.string.search_mode_stremio_addons,
                        ),
                    )
                }
            }
            Text(
                text =
                    if (state.searchMode == SearchMode.INDEXERS) {
                        pluralStringResource(R.plurals.search_active_indexers, state.activeIndexers.size, state.activeIndexers.size)
                    } else {
                        pluralStringResource(R.plurals.search_active_addons, state.activeAddons.size, state.activeAddons.size)
                    },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TvTextField(
                value = query,
                onValueChange = { query = it },
                label =
                    stringResource(
                        if (state.searchMode == SearchMode.INDEXERS) R.string.search_hint else R.string.search_hint_stremio,
                    ),
                enabled = state.canSearch,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search,
                onImeAction = search,
                modifier = Modifier.weight(1f),
            )
            TvActionButton(
                text = stringResource(R.string.search_action),
                icon = R.drawable.ic_search,
                primary = true,
                enabled = state.canSearch && query.isNotBlank(),
                onClick = search,
            )
        }
        if (state.searchHistory.isNotEmpty() && state.canSearch) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                item { Text(stringResource(R.string.search_history_title), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(state.searchHistory) { entry ->
                    // click searches again, long press removes it from the history
                    FilterChip(
                        selected = false,
                        onClick = {
                            query = entry
                            viewModel.closeDetails()
                            viewModel.onSearch(entry)
                        },
                        onLongClick = { viewModel.removeSearchHistoryItem(entry) },
                    ) { Text(entry) }
                }
                item {
                    FilterChip(selected = false, onClick = viewModel::clearSearchHistory) {
                        Text(stringResource(R.string.search_history_clear_all))
                    }
                }
            }
        }

        if (!state.canSearch) {
            EmptyState(
                text =
                    stringResource(
                        if (state.searchMode == SearchMode.INDEXERS) R.string.search_no_indexers else R.string.search_no_stremio_addons,
                    ),
                action = {
                    TvActionButton(
                        stringResource(R.string.search_open_external_services),
                        onOpenExternalServices,
                        icon = R.drawable.ic_apps,
                    )
                },
            )
            return@Column
        }

        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(modifier = Modifier.weight(if (selected != null) 0.45f else 1f).fillMaxHeight()) {
                when (state.searchMode) {
                    SearchMode.INDEXERS -> IndexerResults(state, onSelect = viewModel::selectResult, viewModel = viewModel)
                    SearchMode.STREMIO_ADDONS -> StremioContent(stremio, viewModel)
                }
            }
            selected?.let { result ->
                Column(modifier = Modifier.weight(0.55f).fillMaxHeight()) {
                    TvSearchResultDetail(
                        result = result,
                        canAddDownloads = canAddDownloads,
                        downloadInProgress = adding,
                        onDownload = { link -> if (autoAdd) viewModel.addDownload(link) else onDownloadLink(link) },
                        defaultPlayer = defaultPlayer,
                        mediaPlayers = mediaPlayers,
                        onPlayMedia = viewModel::playOnMedia,
                        onClose = viewModel::closeDetails,
                    )
                }
            }
        }
    }
}

@Composable
private fun IndexerResults(
    state: SearchUiState,
    onSelect: (SearchResult) -> Unit,
    viewModel: SearchViewModel,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            SortChips(
                options = SearchSortOption.entries,
                sort = state.sort,
                onOption = viewModel::setSortOption,
                onAscending = viewModel::setAscending,
            )
        }
        if (state.failedIndexers.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ErrorBanner(
                        pluralStringResource(
                            R.plurals.search_indexers_failed,
                            state.failedIndexers.size,
                            state.failedIndexers.size,
                            state.failedIndexers.joinToString(),
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    TvActionButton(stringResource(R.string.search_dismiss), viewModel::dismissErrors)
                }
            }
        }
        when (val status = state.status) {
            SearchStatus.Idle -> item { EmptyState(stringResource(R.string.search_prompt)) }
            is SearchStatus.Loading ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.search_progress, status.completed, status.total))
                        LinearProgressIndicator(
                            progress = { if (status.total == 0) 0f else status.completed.toFloat() / status.total },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            SearchStatus.Results -> if (state.results.isEmpty()) item { EmptyState(stringResource(R.string.search_no_results)) }
        }
        resultItems(state.results, onSelect)
    }
}

private fun LazyListScope.resultItems(
    results: List<SearchResult>,
    onSelect: (SearchResult) -> Unit,
) {
    items(results) { result ->
        ListItem(
            selected = false,
            onClick = { onSelect(result) },
            supportingContent = {
                Text(
                    listOfNotNull(
                        result.indexerName,
                        result.sizeBytes.takeIf { it > 0 }?.let { convertToSize(it) },
                        if (result.isFromStremioAddon) null else "${result.seeders} ↑ · ${result.peers} ↓",
                        ageLabel(result.publishedAt),
                    ).joinToString(" · "),
                    maxLines = 1,
                )
            },
            headlineContent = { Text(result.title, maxLines = 3, overflow = TextOverflow.Ellipsis) },
            trailingContent =
                if (result.isCached) {
                    { Badge(stringResource(R.string.cached), color = MaterialTheme.colorScheme.tertiary) }
                } else {
                    null
                },
        )
    }
}

@Composable
private fun ageLabel(publishedAt: Long?): String? {
    if (publishedAt == null) return null
    val days = ((System.currentTimeMillis() - publishedAt) / 86_400_000L).toInt()
    return if (days <= 0) stringResource(R.string.search_result_age_today) else stringResource(R.string.search_result_age_days, days)
}

@Composable
private fun SortChips(
    options: List<SearchSortOption>,
    sort: SearchSort,
    onOption: (SearchSortOption) -> Unit,
    onAscending: (Boolean) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.search_sort_by), color = MaterialTheme.colorScheme.onSurfaceVariant)
        options.forEach { option ->
            FilterChip(selected = sort.option == option, onClick = { onOption(option) }) { Text(stringResource(option.label())) }
        }
        if (sort.option != SearchSortOption.RELEVANCE) {
            FilterChip(selected = true, onClick = { onAscending(!sort.ascending) }) {
                Text(stringResource(if (sort.ascending) R.string.search_order_ascending else R.string.search_order_descending))
            }
        }
    }
}

private fun SearchSortOption.label(): Int =
    when (this) {
        SearchSortOption.RELEVANCE -> R.string.search_sort_relevance
        SearchSortOption.SIZE -> R.string.search_sort_size
        SearchSortOption.SEEDERS -> R.string.search_sort_seeders
        SearchSortOption.PEERS -> R.string.search_sort_peers
        SearchSortOption.AGE -> R.string.search_sort_age
        SearchSortOption.NAME -> R.string.search_sort_name
    }

// ---------------- stremio ----------------

@Composable
private fun StremioContent(
    stremio: StremioSearchState,
    viewModel: SearchViewModel,
) {
    when (stremio.stage) {
        StremioStage.IDLE -> FavouritesOrPrompt(viewModel)
        StremioStage.TITLES ->
            when {
                stremio.isSearchingTitles -> LoadingBox(text = stringResource(R.string.search_imdb_searching))
                stremio.titles.isEmpty() -> EmptyState(stringResource(R.string.search_imdb_no_titles))
                else -> TitlesGrid(stremio.titles, onSelect = viewModel::selectImdbTitle)
            }
        StremioStage.EPISODES -> EpisodesSection(stremio, viewModel)
        StremioStage.STREAMS -> StreamsSection(stremio, viewModel)
    }
}

@Composable
private fun SearchViewModel.favouriteIdsState() =
    favourites.collectAsStateWithLifecycle().let { state ->
        remember(state.value) { derivedStateOf { state.value.mapTo(HashSet()) { it.imdbId } } }
    }

/** Idle IMDb search: the starred titles, or the usual prompt when there are none. */
@Composable
private fun FavouritesOrPrompt(viewModel: SearchViewModel) {
    val favourites by viewModel.favourites.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    if (favourites.isEmpty()) {
        EmptyState(stringResource(R.string.search_prompt_stremio))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "${stringResource(R.string.search_favourites_title)} (${favourites.size})",
                style = MaterialTheme.typography.titleLarge,
            )
            if (favourites.size >= 2) {
                TvActionButton(stringResource(R.string.search_favourites_clear), { confirmClear = true }, icon = R.drawable.ic_delete)
            }
        }
        TitlesGrid(favourites, onSelect = viewModel::selectImdbTitle)
    }
    if (confirmClear) {
        ConfirmDialog(
            title = stringResource(R.string.search_favourites_clear),
            message = stringResource(R.string.search_favourites_clear_confirm, favourites.size),
            confirmText = stringResource(R.string.search_favourites_clear),
            onConfirm = {
                confirmClear = false
                viewModel.clearFavourites()
            },
            onDismiss = { confirmClear = false },
            dismissText = stringResource(R.string.cancel),
        )
    }
}

@Composable
private fun TitlesGrid(
    titles: List<ImdbTitleResult>,
    onSelect: (ImdbTitleResult) -> Unit,
) {
    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(180.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp),
    ) {
        items(titles.size) { index ->
            val title = titles[index]
            PosterCard(title = title, onClick = { onSelect(title) })
        }
    }
}

@Composable
private fun EpisodesSection(
    stremio: StremioSearchState,
    viewModel: SearchViewModel,
) {
    val title = stremio.selectedTitle
    var manualSeason by rememberSaveable { mutableStateOf("") }
    var manualEpisode by rememberSaveable { mutableStateOf("") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (stremio.canNavigateBack) {
                    TvActionButton(stringResource(R.string.search_stremio_back_titles), viewModel::navigateBackInStremio, icon = R.drawable.ic_close)
                }
                TvActionButton(stringResource(R.string.search_stremio_whole_series), viewModel::searchWholeSeries, icon = R.drawable.ic_search)
            }
        }
        if (title != null) {
            item {
                val favouriteIds by viewModel.favouriteIdsState()
                TitleDetails(
                    title = title,
                    isFavourite = title.imdbId in favouriteIds,
                    onToggleFavourite = { viewModel.setFavourite(title, title.imdbId !in favouriteIds) },
                )
            }
        }
        item {
            SectionCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    TvTextField(
                        manualSeason,
                        { manualSeason = it.filter(Char::isDigit) },
                        label = stringResource(R.string.search_stremio_manual_season),
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                    TvTextField(
                        manualEpisode,
                        { manualEpisode = it.filter(Char::isDigit) },
                        label = stringResource(R.string.search_stremio_manual_episode),
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                    TvActionButton(
                        stringResource(R.string.search_stremio_manual_search),
                        {
                            val s = manualSeason.toIntOrNull()
                            val e = manualEpisode.toIntOrNull()
                            if (s != null && e != null) viewModel.searchManualSeriesEpisode(s, e)
                        },
                        enabled = manualSeason.isNotBlank() && manualEpisode.isNotBlank(),
                    )
                }
            }
        }
        if (stremio.isLoadingEpisodes) {
            item { LoadingBox(Modifier.size(120.dp), text = stringResource(R.string.search_stremio_loading_episodes)) }
        } else if (stremio.episodes.isEmpty()) {
            item { EmptyState(stringResource(R.string.search_stremio_no_episodes)) }
        } else {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(stremio.availableSeasons) { season ->
                        FilterChip(selected = stremio.selectedSeason == season, onClick = { viewModel.selectSeason(season) }) {
                            Text(
                                if (season == 0) {
                                    stringResource(R.string.search_stremio_specials)
                                } else {
                                    stringResource(R.string.search_stremio_season, season)
                                },
                            )
                        }
                    }
                }
            }
            items(stremio.episodesForSelectedSeason, key = { it.id }) { episode ->
                EpisodeRow(episode, onClick = { viewModel.selectSeriesEpisode(episode) })
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: SeriesEpisodeItem,
    onClick: () -> Unit,
) {
    ListItem(
        selected = false,
        onClick = onClick,
        overlineContent = { Text("S%02dE%02d".format(episode.season, episode.episode)) },
        supportingContent =
            episode.overview?.takeIf { it.isNotBlank() }?.let {
                { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            },
        headlineContent = { Text(episode.title) },
    )
}

@Composable
private fun StreamsSection(
    stremio: StremioSearchState,
    viewModel: SearchViewModel,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (stremio.canNavigateBack) {
                    val back =
                        if (stremio.selectedTitle?.isSeries == true) R.string.search_stremio_back_episodes else R.string.search_stremio_back_titles
                    TvActionButton(stringResource(back), viewModel::navigateBackInStremio, icon = R.drawable.ic_close)
                }
                Text(stremio.activeStreamLabel, style = MaterialTheme.typography.titleMedium, maxLines = 2, modifier = Modifier.weight(1f))
                // movies have no title details here: the star sits in the header
                val title = stremio.selectedTitle
                if (title != null && !title.isSeries) {
                    val favouriteIds by viewModel.favouriteIdsState()
                    TvFavouriteButton(title.imdbId in favouriteIds) { viewModel.setFavourite(title, title.imdbId !in favouriteIds) }
                }
            }
        }
        item {
            FilterChip(selected = stremio.showFilterBar, onClick = { viewModel.setStremioShowFilterBar(!stremio.showFilterBar) }) {
                Text(stringResource(R.string.search_stremio_show_filter_bar))
            }
        }
        if (stremio.showFilterBar) {
            item {
                TvTextField(
                    stremio.filterQuery,
                    viewModel::setStremioFilterQuery,
                    label = stringResource(R.string.search_stremio_filter_hint),
                )
            }
        }
        item {
            SortChips(
                options = STREMIO_SORT_OPTIONS,
                sort = stremio.sort,
                onOption = viewModel::setStremioSortOption,
                onAscending = viewModel::setStremioAscending,
            )
        }
        val results = stremio.filteredStreamResults
        when {
            stremio.isLoadingStreams && stremio.streamResults.isEmpty() ->
                item { LoadingBox(Modifier.size(160.dp), text = stringResource(R.string.search_stremio_loading_streams)) }
            stremio.streamResults.isEmpty() -> item { EmptyState(stringResource(R.string.search_no_results)) }
            results.isEmpty() -> item { EmptyState(stringResource(R.string.search_stremio_no_filtered_results)) }
        }
        resultItems(results, viewModel::selectResult)
    }
}

package com.github.livingwithhippos.nullseek.ui.search

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.livingwithhippos.nullseek.NullRootUiState
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.core.data.stremio.ImdbTitleResult
import com.github.livingwithhippos.nullseek.core.data.stremio.SeriesEpisodeItem
import com.github.livingwithhippos.nullseek.core.utils.convertToSize
import com.github.livingwithhippos.nullseek.data.model.ExternalService
import com.github.livingwithhippos.nullseek.data.model.Indexer
import com.github.livingwithhippos.nullseek.data.model.IndexerType
import com.github.livingwithhippos.nullseek.data.model.SearchResult
import com.github.livingwithhippos.nullseek.data.model.isDirectStreamResult
import com.github.livingwithhippos.nullseek.ui.theme.AssistChip
import com.github.livingwithhippos.nullseek.ui.theme.Button
import com.github.livingwithhippos.nullseek.ui.theme.FilledTonalButton
import com.github.livingwithhippos.nullseek.ui.theme.FilterChip
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.ui.theme.OutlinedButton
import com.github.livingwithhippos.nullseek.ui.theme.SuggestionChip
import com.github.livingwithhippos.nullseek.ui.theme.TextButton
import com.github.livingwithhippos.nullseek.ui.utils.ExtendableFabMenu
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType
import com.github.livingwithhippos.nullseek.utils.openWebPage
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SearchScreen(
    contentType: NullContentType,
    uiState: NullRootUiState,
    navigationType: NullNavigationType,
    modifier: Modifier = Modifier,
    onNewDownload: () -> Unit = {},
    onDownload: (link: String) -> Unit = {},
    showSettings: () -> Unit = {},
    onNavigateToExternalServices: () -> Unit = {},
    viewModel: SearchViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selected by viewModel.selectedResult.collectAsStateWithLifecycle()
    val autoAdd by viewModel.autoAddDownload.collectAsStateWithLifecycle()
    val adding by viewModel.addingDownload.collectAsStateWithLifecycle()
    val defaultPlayer by viewModel.defaultMediaPlayer.collectAsStateWithLifecycle()
    val mediaPlayers by viewModel.mediaPlayerServices.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.downloadEvents.collect { added ->
            val message =
                if (added) R.string.search_download_added else R.string.search_download_failed
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.playbackEvents.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    SearchContent(
        state = state,
        selectedResult = selected,
        onResultClick = viewModel::selectResult,
        onCloseDetails = viewModel::closeDetails,
        onDownload = { link -> if (autoAdd) viewModel.addDownload(link) else onDownload(link) },
        downloadInProgress = adding,
        defaultPlayer = defaultPlayer,
        mediaPlayers = mediaPlayers,
        onPlayMedia = viewModel::playOnMedia,
        navigationType = navigationType,
        modifier = modifier,
        onSearch = viewModel::onSearch,
        onSearchMode = viewModel::setSearchMode,
        onSortOption = viewModel::setSortOption,
        onAscending = viewModel::setAscending,
        onStremioFilterQuery = viewModel::setStremioFilterQuery,
        onStremioShowFilterBar = viewModel::setStremioShowFilterBar,
        onStremioSortOption = viewModel::setStremioSortOption,
        onStremioAscending = viewModel::setStremioAscending,
        onDismissErrors = viewModel::dismissErrors,
        onSelectImdbTitle = viewModel::selectImdbTitle,
        onSelectSeason = viewModel::selectSeason,
        onSelectEpisode = viewModel::selectSeriesEpisode,
        onSearchManualEpisode = viewModel::searchManualSeriesEpisode,
        onSearchWholeSeries = viewModel::searchWholeSeries,
        onStremioBack = viewModel::navigateBackInStremio,
        onRemoveHistoryItem = viewModel::removeSearchHistoryItem,
        onClearHistory = viewModel::clearSearchHistory,
        onNewDownload = onNewDownload,
        showSettings = showSettings,
        showNewDownload = !uiState.setup && uiState.user != null,
        onNavigateToExternalServices = onNavigateToExternalServices,
    )
}

@Composable
fun SearchContent(
    state: SearchUiState,
    navigationType: NullNavigationType,
    modifier: Modifier = Modifier,
    onSearch: (query: String) -> Unit = {},
    onSearchMode: (SearchMode) -> Unit = {},
    onSortOption: (SearchSortOption) -> Unit = {},
    onAscending: (Boolean) -> Unit = {},
    onStremioFilterQuery: (String) -> Unit = {},
    onStremioShowFilterBar: (Boolean) -> Unit = {},
    onStremioSortOption: (SearchSortOption) -> Unit = {},
    onStremioAscending: (Boolean) -> Unit = {},
    onDismissErrors: () -> Unit = {},
    onSelectImdbTitle: (ImdbTitleResult) -> Unit = {},
    onSelectSeason: (Int) -> Unit = {},
    onSelectEpisode: (SeriesEpisodeItem) -> Unit = {},
    onSearchManualEpisode: (Int, Int) -> Unit = { _, _ -> },
    onSearchWholeSeries: () -> Unit = {},
    onStremioBack: () -> Unit = {},
    onRemoveHistoryItem: (String) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onNewDownload: () -> Unit = {},
    showSettings: () -> Unit = {},
    showNewDownload: Boolean = true,
    onNavigateToExternalServices: () -> Unit = {},
    selectedResult: SearchResult? = null,
    onResultClick: (SearchResult) -> Unit = {},
    onCloseDetails: () -> Unit = {},
    onDownload: (link: String) -> Unit = {},
    downloadInProgress: Boolean = false,
    defaultPlayer: ExternalService? = null,
    mediaPlayers: List<ExternalService> = emptyList(),
    onPlayMedia: (ExternalService, String, Boolean) -> Unit = { _, _, _ -> },
    initialSheetVisible: Boolean = false,
) {
    var showSheet by rememberSaveable { mutableStateOf(initialSheetVisible) }
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    val scope = rememberCoroutineScope()

    val startSearch = {
        scope.launch {
            searchBarState.animateToCollapsed()
            val query = textFieldState.text.toString().trim()
            if (query.isNotEmpty()) {
                onSearch(query)
            }
        }
    }

    val hintRes =
        when (state.searchMode) {
            SearchMode.INDEXERS -> R.string.search_hint
            SearchMode.STREMIO_ADDONS -> R.string.search_hint_stremio
        }

    val inputField =
        @Composable {
            SearchBarDefaults.InputField(
                textFieldState = textFieldState,
                searchBarState = searchBarState,
                onSearch = { startSearch() },
                enabled = state.canSearch,
                placeholder = {
                    Text(
                        modifier = Modifier.clearAndSetSemantics {},
                        text = stringResource(hintRes),
                    )
                },
                leadingIcon = {
                    IconButton(
                        onClick = { startSearch() },
                        enabled = state.canSearch,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = stringResource(R.string.search_action),
                        )
                    }
                },
                trailingIcon = {
                    if (textFieldState.text.isNotEmpty()) {
                        IconButton(onClick = { textFieldState.clearText() }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.search_clear),
                            )
                        }
                    }
                },
            )
        }

    if (selectedResult != null) {
        if (selectedResult.isDirectStreamResult()) {
            SearchStreamDetailScreen(
                result = selectedResult,
                onClose = onCloseDetails,
                defaultPlayer = defaultPlayer,
                mediaPlayers = mediaPlayers,
                onPlayMedia = onPlayMedia,
                onNavigateToExternalServices = onNavigateToExternalServices,
                modifier = modifier,
            )
        } else {
            SearchResultDetailScreen(
                result = selectedResult,
                onClose = onCloseDetails,
                onDownload = onDownload,
                downloadInProgress = downloadInProgress,
                modifier = modifier,
            )
        }
        return
    }

    if (state.searchMode == SearchMode.STREMIO_ADDONS && state.stremioState.canNavigateBack) {
        BackHandler(onBack = onStremioBack)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchBar(
                    modifier = Modifier.weight(1f),
                    state = searchBarState,
                    inputField = inputField,
                    shape = RectangleShape,
                )
                ExpandedFullScreenSearchBar(
                    state = searchBarState,
                    inputField = inputField,
                ) {
                    if (state.searchHistory.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.search_history_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                TextButton(onClick = onClearHistory) {
                                    Text(text = stringResource(R.string.search_history_clear_all))
                                }
                            }
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(state.searchHistory, key = { it }) { historyQuery ->
                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    textFieldState.setTextAndPlaceCursorAtEnd(historyQuery)
                                                    scope.launch {
                                                        searchBarState.animateToCollapsed()
                                                        onSearch(historyQuery)
                                                    }
                                                }.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_history),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = historyQuery,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        IconButton(onClick = { onRemoveHistoryItem(historyQuery) }) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_close),
                                                contentDescription = stringResource(R.string.search_history_remove_item),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                IconButton(onClick = { showSheet = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tune),
                        contentDescription = stringResource(R.string.search_options),
                    )
                }
            }

            when (state.searchMode) {
                SearchMode.INDEXERS -> {
                    if (state.hasActiveIndexers) {
                        Text(
                            text =
                                pluralStringResource(
                                    R.plurals.search_active_indexers,
                                    state.activeIndexers.size,
                                    state.activeIndexers.size,
                                ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier =
                                Modifier
                                    .clickable(onClick = onNavigateToExternalServices)
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }

                    if (state.hasActiveIndexers && state.status != SearchStatus.Idle) {
                        SearchResultsSection(
                            state = state,
                            onDismissErrors = onDismissErrors,
                            onResultClick = onResultClick,
                        )
                    } else {
                        SearchEmptyOrPromptSection(
                            hasActiveSources = state.hasActiveIndexers,
                            promptText = stringResource(R.string.search_prompt),
                            emptyText = stringResource(R.string.search_no_indexers),
                            onNavigateToExternalServices = onNavigateToExternalServices,
                        )
                    }
                }

                SearchMode.STREMIO_ADDONS -> {
                    if (state.hasActiveAddons) {
                        Text(
                            text =
                                pluralStringResource(
                                    R.plurals.search_active_addons,
                                    state.activeAddons.size,
                                    state.activeAddons.size,
                                ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier =
                                Modifier
                                    .clickable(onClick = onNavigateToExternalServices)
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }

                    if (state.hasActiveAddons && state.stremioState.stage != StremioStage.IDLE) {
                        StremioSearchContentSection(
                            stremioState = state.stremioState,
                            onSelectImdbTitle = onSelectImdbTitle,
                            onSelectSeason = onSelectSeason,
                            onSelectEpisode = onSelectEpisode,
                            onSearchManualEpisode = onSearchManualEpisode,
                            onSearchWholeSeries = onSearchWholeSeries,
                            onStremioFilterQuery = onStremioFilterQuery,
                            onStremioBack = onStremioBack,
                            onResultClick = onResultClick,
                        )
                    } else {
                        SearchEmptyOrPromptSection(
                            hasActiveSources = state.hasActiveAddons,
                            promptText = stringResource(R.string.search_prompt_stremio),
                            emptyText = stringResource(R.string.search_no_stremio_addons),
                            onNavigateToExternalServices = onNavigateToExternalServices,
                        )
                    }
                }
            }
        }

        if (navigationType == NullNavigationType.BOTTOM_NAVIGATION) {
            ExtendableFabMenu(
                onNewDownload = onNewDownload,
                showSettings = showSettings,
                showNewDownload = showNewDownload,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }

    if (showSheet) {
        SearchSortSheet(
            searchMode = state.searchMode,
            onSearchMode = onSearchMode,
            sort = state.sort,
            onSortOption = onSortOption,
            onAscending = onAscending,
            stremioSort = state.stremioState.sort,
            onStremioSortOption = onStremioSortOption,
            onStremioAscending = onStremioAscending,
            stremioShowFilterBar = state.stremioState.showFilterBar,
            onStremioShowFilterBar = onStremioShowFilterBar,
            onDismiss = { showSheet = false },
        )
    }
}

@Composable
private fun SearchEmptyOrPromptSection(
    hasActiveSources: Boolean,
    promptText: String,
    emptyText: String,
    onNavigateToExternalServices: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (hasActiveSources) {
            Text(
                text = promptText,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onNavigateToExternalServices,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text(stringResource(R.string.search_open_external_services))
            }
        }
    }
}

@Composable
private fun StremioSearchContentSection(
    stremioState: StremioSearchState,
    onSelectImdbTitle: (ImdbTitleResult) -> Unit,
    onSelectSeason: (Int) -> Unit,
    onSelectEpisode: (SeriesEpisodeItem) -> Unit,
    onSearchManualEpisode: (Int, Int) -> Unit,
    onSearchWholeSeries: () -> Unit,
    onStremioFilterQuery: (String) -> Unit,
    onStremioBack: () -> Unit,
    onResultClick: (SearchResult) -> Unit,
) {
    when (stremioState.stage) {
        StremioStage.IDLE -> Unit
        StremioStage.TITLES -> {
            StremioTitlesSection(
                stremioState = stremioState,
                onSelectImdbTitle = onSelectImdbTitle,
            )
        }
        StremioStage.EPISODES -> {
            StremioEpisodesSection(
                stremioState = stremioState,
                onSelectSeason = onSelectSeason,
                onSelectEpisode = onSelectEpisode,
                onSearchManualEpisode = onSearchManualEpisode,
                onSearchWholeSeries = onSearchWholeSeries,
                onBackToTitles = onStremioBack,
            )
        }
        StremioStage.STREAMS -> {
            StremioStreamsSection(
                stremioState = stremioState,
                onFilterQueryChange = onStremioFilterQuery,
                onBack = onStremioBack,
                onResultClick = onResultClick,
            )
        }
    }
}

@Composable
private fun StremioTitlesSection(
    stremioState: StremioSearchState,
    onSelectImdbTitle: (ImdbTitleResult) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (stremioState.isSearchingTitles) {
            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = stringResource(R.string.search_imdb_searching),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        } else if (stremioState.titles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.search_imdb_no_titles),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 8.dp,
                        bottom = 88.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(stremioState.titles, key = { "${it.type}:${it.imdbId}" }) { titleItem ->
                    ImdbTitleCard(
                        item = titleItem,
                        onClick = { onSelectImdbTitle(titleItem) },
                    )
                }
            }
        }
    }
}

@Composable
fun ImdbTitleCard(
    item: ImdbTitleResult,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PosterImage(
                posterUrl = item.posterUrl,
                title = item.title,
                modifier =
                    Modifier
                        .width(64.dp)
                        .height(96.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SuggestionChip(
                        onClick = onClick,
                        label = {
                            Text(
                                text =
                                    item.qualifier
                                        ?: if (item.isSeries) {
                                            stringResource(R.string.search_imdb_type_series)
                                        } else {
                                            stringResource(R.string.search_imdb_type_movie)
                                        },
                            )
                        },
                    )
                    item.imdbRating?.takeIf { it.isNotBlank() }?.let { rating ->
                        Surface(
                            shape = RoundedCornerShape(0.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = stringResource(R.string.search_stremio_imdb_rating, rating),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    val yearRuntime =
                        listOfNotNull(
                            item.year?.takeIf { it.isNotBlank() },
                            item.runtime?.takeIf { it.isNotBlank() },
                        ).joinToString(" · ")
                    if (yearRuntime.isNotEmpty()) {
                        Text(
                            text = yearRuntime,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (item.genres.isNotEmpty()) {
                    Text(
                        text = item.genres.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                item.stars?.let { stars ->
                    Text(
                        text = stringResource(R.string.search_stremio_cast, stars),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                item.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val idAndRank =
                    listOfNotNull(
                        item.imdbId,
                        item.rank?.let { stringResource(R.string.search_imdb_rank, it) },
                    ).joinToString(" · ")
                Text(
                    text = idAndRank,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun StremioEpisodesSection(
    stremioState: StremioSearchState,
    onSelectSeason: (Int) -> Unit,
    onSelectEpisode: (SeriesEpisodeItem) -> Unit,
    onSearchManualEpisode: (Int, Int) -> Unit,
    onSearchWholeSeries: () -> Unit,
    onBackToTitles: () -> Unit,
) {
    val selectedTitle = stremioState.selectedTitle ?: return
    var manualSeasonText by rememberSaveable(selectedTitle.imdbId) { mutableStateOf("1") }
    var manualEpisodeText by rememberSaveable(selectedTitle.imdbId) { mutableStateOf("1") }

    Column(modifier = Modifier.fillMaxSize()) {
        StremioTitleDetailsCard(
            selectedTitle = selectedTitle,
            selectedEpisode = null,
            activeStreamImdbId = selectedTitle.imdbId,
            activeStreamLabel = selectedTitle.title,
            canNavigateBack = stremioState.canNavigateBack,
            backButtonLabel = stringResource(R.string.search_stremio_back_titles),
            onBack = onBackToTitles,
            initialExpanded = false,
            trailingAction = {
                FilledTonalButton(onClick = onSearchWholeSeries) {
                    Text(stringResource(R.string.search_stremio_whole_series))
                }
            },
        )

        // Manual Season / Episode quick entry
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = manualSeasonText,
                onValueChange = { value ->
                    if (value.all { it.isDigit() } && value.length <= 3) {
                        manualSeasonText = value
                    }
                },
                label = { Text(stringResource(R.string.search_stremio_manual_season)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = manualEpisodeText,
                onValueChange = { value ->
                    if (value.all { it.isDigit() } && value.length <= 4) {
                        manualEpisodeText = value
                    }
                },
                label = { Text(stringResource(R.string.search_stremio_manual_episode)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            val parsedS = manualSeasonText.toIntOrNull()
            val parsedE = manualEpisodeText.toIntOrNull()
            Button(
                onClick = {
                    if (parsedS != null && parsedE != null) {
                        onSearchManualEpisode(parsedS, parsedE)
                    }
                },
                enabled = parsedS != null && parsedS >= 0 && parsedE != null && parsedE > 0,
            ) {
                Text(stringResource(R.string.search_stremio_manual_search))
            }
        }

        if (stremioState.isLoadingEpisodes) {
            LinearWavyProgressIndicator(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
            )
            Text(
                text = stringResource(R.string.search_stremio_loading_episodes),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        } else if (stremioState.episodes.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.search_stremio_no_episodes),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            val seasons = stremioState.availableSeasons
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(seasons, key = { it }) { season ->
                    FilterChip(
                        selected = stremioState.selectedSeason == season,
                        onClick = {
                            manualSeasonText = season.toString()
                            onSelectSeason(season)
                        },
                        label = {
                            Text(
                                text =
                                    if (season == 0) {
                                        stringResource(R.string.search_stremio_specials)
                                    } else {
                                        stringResource(R.string.search_stremio_season, season)
                                    },
                            )
                        },
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 4.dp,
                        bottom = 88.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    stremioState.episodesForSelectedSeason,
                    key = { "${it.season}:${it.episode}:${it.id}" },
                ) { episode ->
                    SeriesEpisodeCard(
                        episode = episode,
                        onClick = { onSelectEpisode(episode) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SeriesEpisodeCard(
    episode: SeriesEpisodeItem,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "E%02d · %s".format(episode.episode, episode.title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                episode.released?.let { released ->
                    Text(
                        text = released,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            episode.overview?.let { overview ->
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StremioTitleDetailsCard(
    selectedTitle: ImdbTitleResult?,
    selectedEpisode: SeriesEpisodeItem?,
    activeStreamImdbId: String,
    activeStreamLabel: String,
    canNavigateBack: Boolean,
    backButtonLabel: String,
    onBack: () -> Unit,
    initialExpanded: Boolean = true,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val baseImdbId =
        (selectedTitle?.imdbId ?: activeStreamImdbId)
            .substringBefore(":")
            .trim()
    var isExpanded by rememberSaveable(baseImdbId, activeStreamImdbId) { mutableStateOf(initialExpanded) }

    val displayTitle = selectedTitle?.title ?: activeStreamLabel
    val compactSubtitle =
        buildList {
            selectedTitle?.imdbRating?.takeIf { it.isNotBlank() }?.let {
                add(stringResource(R.string.search_stremio_imdb_rating, it))
            }
            if (selectedEpisode != null) {
                add("S%02dE%02d · %s".format(selectedEpisode.season, selectedEpisode.episode, selectedEpisode.title))
            } else {
                selectedTitle?.year?.takeIf { it.isNotBlank() }?.let { add(it) }
                selectedTitle?.runtime?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
            activeStreamImdbId.ifBlank { baseImdbId }.takeIf { it.isNotBlank() }?.let { add(it) }
        }.joinToString(" · ")

    OutlinedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clickable { isExpanded = !isExpanded },
    ) {
        if (!isExpanded) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (selectedTitle != null) {
                        PosterImage(
                            posterUrl = selectedTitle.posterUrl,
                            title = selectedTitle.title,
                            modifier =
                                Modifier
                                    .width(40.dp)
                                    .height(60.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (compactSubtitle.isNotEmpty()) {
                            Text(
                                text = compactSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (canNavigateBack && trailingAction == null) {
                        TextButton(onClick = onBack) {
                            Text(text = backButtonLabel)
                        }
                    }
                    IconButton(
                        onClick = { isExpanded = true },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_drop_down),
                            contentDescription = stringResource(R.string.search_stremio_expand_details),
                        )
                    }
                }
                if (trailingAction != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (canNavigateBack) {
                            TextButton(onClick = onBack) {
                                Text(text = backButtonLabel)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }
                        trailingAction()
                    }
                }
            }
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (selectedTitle != null) {
                        PosterImage(
                            posterUrl = selectedTitle.posterUrl,
                            title = selectedTitle.title,
                            modifier =
                                Modifier
                                    .width(72.dp)
                                    .height(108.dp),
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = { isExpanded = false },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_drop_up),
                                    contentDescription = stringResource(R.string.search_stremio_collapse_details),
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            selectedTitle?.imdbRating?.takeIf { it.isNotBlank() }?.let { rating ->
                                Surface(
                                    shape = RoundedCornerShape(0.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                ) {
                                    Text(
                                        text = stringResource(R.string.search_stremio_imdb_rating, rating),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            val metaLine =
                                listOfNotNull(
                                    selectedTitle?.year?.takeIf { it.isNotBlank() },
                                    selectedTitle?.runtime?.takeIf { it.isNotBlank() },
                                    activeStreamImdbId.ifBlank { baseImdbId }.takeIf { it.isNotBlank() },
                                ).joinToString(" · ")
                            if (metaLine.isNotEmpty()) {
                                Text(
                                    text = metaLine,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        if (!selectedTitle?.genres.isNullOrEmpty()) {
                            Text(
                                text = selectedTitle.genres.joinToString(" · "),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        selectedTitle?.stars?.takeIf { it.isNotBlank() }?.let { stars ->
                            Text(
                                text = stringResource(R.string.search_stremio_cast, stars),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (!selectedTitle?.director.isNullOrEmpty()) {
                            Text(
                                text = stringResource(R.string.search_stremio_director, selectedTitle.director.joinToString(", ")),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (selectedEpisode != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "S%02dE%02d · %s".format(selectedEpisode.season, selectedEpisode.episode, selectedEpisode.title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        selectedEpisode.released?.let { released ->
                            Text(
                                text = released,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }

                val summaryText =
                    when {
                        !selectedEpisode?.overview.isNullOrBlank() && !selectedTitle?.description.isNullOrBlank() ->
                            "${selectedEpisode.overview}\n\n${selectedTitle.description}"
                        !selectedEpisode?.overview.isNullOrBlank() -> selectedEpisode.overview
                        !selectedTitle?.description.isNullOrBlank() -> selectedTitle.description
                        else -> null
                    }

                if (!summaryText.isNullOrBlank()) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                                .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            text = summaryText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (canNavigateBack) {
                        TextButton(onClick = onBack) {
                            Text(text = backButtonLabel)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (baseImdbId.startsWith("tt")) {
                            OutlinedButton(
                                onClick = {
                                    openWebPage(context, "https://www.imdb.com/title/$baseImdbId/")
                                },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_open_in_new),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = stringResource(R.string.search_stremio_open_imdb))
                            }
                        }
                        trailingAction?.invoke()
                    }
                }
            }
        }
    }
}

@Composable
private fun StremioStreamsSection(
    stremioState: StremioSearchState,
    onFilterQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onResultClick: (SearchResult) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        val selectedTitle = stremioState.selectedTitle
        if (selectedTitle != null || stremioState.canNavigateBack || stremioState.activeStreamImdbId.isNotBlank()) {
            StremioTitleDetailsCard(
                selectedTitle = selectedTitle,
                selectedEpisode = stremioState.selectedEpisode,
                activeStreamImdbId = stremioState.activeStreamImdbId,
                activeStreamLabel = stremioState.activeStreamLabel,
                canNavigateBack = stremioState.canNavigateBack,
                backButtonLabel =
                    if (selectedTitle?.isSeries == true) {
                        stringResource(R.string.search_stremio_back_episodes)
                    } else {
                        stringResource(R.string.search_stremio_back_titles)
                    },
                onBack = onBack,
                initialExpanded = true,
            )
        }

        if (stremioState.showFilterBar && (!stremioState.isLoadingStreams && stremioState.streamResults.isNotEmpty() || stremioState.filterQuery.isNotEmpty())) {
            OutlinedTextField(
                value = stremioState.filterQuery,
                onValueChange = onFilterQueryChange,
                placeholder = { Text(stringResource(R.string.search_stremio_filter_hint)) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    if (stremioState.filterQuery.isNotEmpty()) {
                        IconButton(onClick = { onFilterQueryChange("") }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.search_clear),
                            )
                        }
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        val displayedStreams = stremioState.filteredStreamResults
        if (stremioState.isLoadingStreams) {
            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = stringResource(R.string.search_stremio_loading_streams),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        } else if (stremioState.streamResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.search_no_results),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else if (displayedStreams.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.search_stremio_no_filtered_results),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 8.dp,
                        bottom = 88.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(displayedStreams) { result ->
                    SearchResultCard(
                        result = result,
                        onClick = { onResultClick(result) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultsSection(
    state: SearchUiState,
    onDismissErrors: () -> Unit,
    onResultClick: (SearchResult) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        val status = state.status
        if (status is SearchStatus.Loading) {
            LinearWavyProgressIndicator(
                modifier =
                    Modifier
                        .fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.search_progress, status.completed, status.total),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        if (state.failedIndexers.isNotEmpty()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text =
                        pluralStringResource(
                            R.plurals.search_indexers_failed,
                            state.failedIndexers.size,
                            state.failedIndexers.size,
                            state.failedIndexers.joinToString(", "),
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismissErrors) { Text(stringResource(R.string.search_dismiss)) }
            }
        }
        if (state.results.isEmpty()) {
            if (status == SearchStatus.Results) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.search_no_results),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 8.dp,
                        bottom = 88.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.results) { SearchResultCard(it, onClick = { onResultClick(it) }) }
            }
        }
    }
}

@Composable
internal fun ageText(publishedAt: Long?): String? {
    if (publishedAt == null) return null
    val days = ((System.currentTimeMillis() - publishedAt) / 86_400_000L).toInt()
    return if (days <= 0) {
        stringResource(R.string.search_result_age_today)
    } else {
        stringResource(R.string.search_result_age_days, days)
    }
}

@Composable
fun SearchResultCard(
    result: SearchResult,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                AssistChip(onClick = {}, label = { Text(result.indexerName) })
                result.categories.forEach { category ->
                    Surface(
                        shape = RoundedCornerShape(0.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        )
                    }
                }
                if (result.sizeBytes > 0L) {
                    Text(convertToSize(result.sizeBytes), style = MaterialTheme.typography.labelMedium)
                }
                ageText(result.publishedAt)?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium)
                }
            }
            if ((!result.isFromStremioAddon && !result.isDirectStream) || result.seeders > 0 || result.peers > 0) {
                Text(
                    text =
                        if (result.isFromStremioAddon && result.seeders > 0 && result.peers == 0) {
                            stringResource(R.string.search_result_seeders_only, result.seeders)
                        } else {
                            stringResource(R.string.search_result_seeders_peers, result.seeders, result.peers)
                        },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SearchMode.label(): String =
    stringResource(
        when (this) {
            SearchMode.INDEXERS -> R.string.search_mode_indexers
            SearchMode.STREMIO_ADDONS -> R.string.search_mode_stremio_addons
        },
    )

@Composable
private fun SearchSortOption.label(): String =
    stringResource(
        when (this) {
            SearchSortOption.RELEVANCE -> R.string.search_sort_relevance
            SearchSortOption.SIZE -> R.string.search_sort_size
            SearchSortOption.SEEDERS -> R.string.search_sort_seeders
            SearchSortOption.PEERS -> R.string.search_sort_peers
            SearchSortOption.AGE -> R.string.search_sort_age
            SearchSortOption.NAME -> R.string.search_sort_name
        },
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSortSheet(
    sort: SearchSort,
    onSortOption: (SearchSortOption) -> Unit,
    onAscending: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    searchMode: SearchMode = SearchMode.INDEXERS,
    onSearchMode: (SearchMode) -> Unit = {},
    stremioSort: SearchSort = SearchSort(),
    onStremioSortOption: (SearchSortOption) -> Unit = {},
    onStremioAscending: (Boolean) -> Unit = {},
    stremioShowFilterBar: Boolean = true,
    onStremioShowFilterBar: (Boolean) -> Unit = {},
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.search_mode_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            SingleChoiceSegmentedButtonRow(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
            ) {
                SearchMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = searchMode == mode,
                        onClick = { onSearchMode(mode) },
                        shape =
                            SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = SearchMode.entries.size,
                                baseShape = RoundedCornerShape(0.dp),
                            ),
                    ) {
                        Text(mode.label())
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

            when (searchMode) {
                SearchMode.INDEXERS -> {
                    SortOptionsSection(
                        options = SearchSortOption.entries,
                        sort = sort,
                        onSortOption = onSortOption,
                        onAscending = onAscending,
                    )
                }

                SearchMode.STREMIO_ADDONS -> {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onStremioShowFilterBar(!stremioShowFilterBar) }
                                .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.search_stremio_show_filter_bar),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(R.string.search_stremio_show_filter_bar_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = stremioShowFilterBar,
                            onCheckedChange = onStremioShowFilterBar,
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

                    SortOptionsSection(
                        options = STREMIO_SORT_OPTIONS,
                        sort = stremioSort,
                        onSortOption = onStremioSortOption,
                        onAscending = onStremioAscending,
                    )
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier =
                    Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp),
            ) { Text(stringResource(R.string.search_done)) }
        }
    }
}

@Composable
private fun SortOptionsSection(
    options: List<SearchSortOption>,
    sort: SearchSort,
    onSortOption: (SearchSortOption) -> Unit,
    onAscending: (Boolean) -> Unit,
) {
    Text(
        text = stringResource(R.string.search_sort_by),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    options.forEach { option ->
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = sort.option == option,
                        onClick = { onSortOption(option) },
                        role = Role.RadioButton,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = sort.option == option, onClick = null)
            Text(
                option.label(),
                modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
            )
        }
    }
    SingleChoiceSegmentedButtonRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
    ) {
        SegmentedButton(
            selected = sort.ascending,
            onClick = { onAscending(true) },
            shape = SegmentedButtonDefaults.itemShape(0, 2),
        ) { Text(stringResource(R.string.search_order_ascending)) }
        SegmentedButton(
            selected = !sort.ascending,
            onClick = { onAscending(false) },
            shape = SegmentedButtonDefaults.itemShape(1, 2),
        ) { Text(stringResource(R.string.search_order_descending)) }
    }
}

private val previewIndexers =
    listOf(
        Indexer(1, "Jackett", IndexerType.JACKETT, "http://localhost:9117"),
        Indexer(2, "Prowlarr", IndexerType.PROWLARR, "http://localhost:9696"),
    )

@Preview
@Composable
fun SearchScreenNoIndexersPreview() {
    NullSeekTheme {
        Surface {
            SearchContent(SearchUiState(), NullNavigationType.BOTTOM_NAVIGATION)
        }
    }
}

@Preview
@Composable
fun SearchScreenIndexersPreview() {
    NullSeekTheme {
        Surface {
            SearchContent(
                SearchUiState(activeIndexers = previewIndexers),
                NullNavigationType.BOTTOM_NAVIGATION,
            )
        }
    }
}

@Preview
@Composable
fun SearchSortSheetPreview() {
    NullSeekTheme {
        SearchSortSheet(SearchSort(SearchSortOption.SEEDERS, false), {}, {}, {})
    }
}

private val previewResults =
    listOf(
        SearchResult("Ubuntu 24.04 Desktop amd64", "Jackett", 6_200_000_000, 120, 15, System.currentTimeMillis() - 3 * 86_400_000L),
        SearchResult("Debian 12 netinst", "Prowlarr", 650_000_000, 80, 4, null),
    )

@Preview
@Composable
fun SearchScreenResultsPreview() {
    NullSeekTheme {
        Surface {
            SearchContent(
                SearchUiState(
                    activeIndexers = previewIndexers,
                    status = SearchStatus.Results,
                    results = previewResults,
                    failedIndexers = listOf("Prowlarr"),
                ),
                NullNavigationType.BOTTOM_NAVIGATION,
            )
        }
    }
}

@Preview
@Composable
fun SearchScreenLoadingPreview() {
    NullSeekTheme {
        Surface {
            SearchContent(
                SearchUiState(
                    activeIndexers = previewIndexers,
                    status = SearchStatus.Loading(1, 2),
                    results = previewResults.take(1),
                ),
                NullNavigationType.BOTTOM_NAVIGATION,
            )
        }
    }
}

@Preview
@Composable
fun SearchScreenEmptyPreview() {
    NullSeekTheme {
        Surface {
            SearchContent(
                SearchUiState(activeIndexers = previewIndexers, status = SearchStatus.Results),
                NullNavigationType.BOTTOM_NAVIGATION,
            )
        }
    }
}

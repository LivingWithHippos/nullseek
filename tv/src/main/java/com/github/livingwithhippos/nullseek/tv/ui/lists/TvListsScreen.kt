package com.github.livingwithhippos.nullseek.tv.ui.lists

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.github.livingwithhippos.nullseek.NullRootUiState
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.core.utils.convertToSize
import com.github.livingwithhippos.nullseek.tv.ui.components.Badge
import com.github.livingwithhippos.nullseek.tv.ui.components.EmptyState
import com.github.livingwithhippos.nullseek.tv.ui.components.ErrorBanner
import com.github.livingwithhippos.nullseek.tv.ui.components.LoadingBox
import com.github.livingwithhippos.nullseek.tv.ui.components.TvActionButton
import com.github.livingwithhippos.nullseek.tv.ui.components.TvTextField
import com.github.livingwithhippos.nullseek.ui.lists.ListsViewModel
import com.github.livingwithhippos.torbox.model.TorrentDto
import com.github.livingwithhippos.torbox.model.UsenetDownloadDto
import com.github.livingwithhippos.torbox.model.WebDownloadDto
import org.koin.androidx.compose.koinViewModel

private enum class ListTab(
    val label: Int,
    @DrawableRes val icon: Int,
) {
    TORRENTS(R.string.torrents, R.drawable.ic_magnet),
    WEB(R.string.web_downloads, R.drawable.ic_file),
    USENET(R.string.usenet_downloads, R.drawable.ic_edit),
}

/** Fields shown in a list row, shared by the three download types. */
private data class RowData(
    val id: Int,
    val name: String,
    val size: Long,
    val state: String,
    val progress: Double,
    val cached: Boolean,
)

private fun TorrentDto.toRow() = RowData(id, name, size, downloadState, progress, cached)

private fun WebDownloadDto.toRow() = RowData(id, name, size, downloadState, progress, cached)

private fun UsenetDownloadDto.toRow() = RowData(id, name, size, downloadState, progress, cached)

@Composable
fun TvListsScreen(
    uiState: NullRootUiState,
    loadTorrentDetails: (TorrentDto) -> Unit,
    loadWebDetails: (WebDownloadDto) -> Unit,
    loadUsenetDetails: (UsenetDownloadDto) -> Unit,
    closeDetails: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ListsViewModel = koinViewModel(),
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var detailFullScreen by rememberSaveable { mutableStateOf(false) }

    val torrents = viewModel.torrentsPagingFlow.collectAsLazyPagingItems()
    val webDownloads = viewModel.webPagingFlow.collectAsLazyPagingItems()
    val usenetDownloads = viewModel.usenetPagingFlow.collectAsLazyPagingItems()

    val tab = ListTab.entries[selectedTab]
    val hasDetail =
        when (tab) {
            ListTab.TORRENTS -> uiState.selectedTorrent != null
            ListTab.WEB -> uiState.selectedWeb != null
            ListTab.USENET -> uiState.selectedUsenet != null
        }
    val listFocus = remember { FocusRequester() }
    val detailFocus = remember { FocusRequester() }

    val closeDetail = {
        detailFullScreen = false
        closeDetails()
    }

    // keep focus on screen when one of the panes is removed
    var detailWasFullScreen by remember { mutableStateOf(false) }
    LaunchedEffect(hasDetail, detailFullScreen) {
        if (hasDetail && detailFullScreen) {
            detailWasFullScreen = true
            runCatching { detailFocus.requestFocus() }
        } else if (detailWasFullScreen) {
            detailWasFullScreen = false
            runCatching { listFocus.requestFocus() }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TabRow(selectedTabIndex = selectedTab, modifier = Modifier.weight(1f)) {
                ListTab.entries.forEachIndexed { index, entry ->
                    Tab(
                        selected = index == selectedTab,
                        onFocus = {
                            if (selectedTab != index) {
                                selectedTab = index
                                closeDetail()
                            }
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(painterResource(entry.icon), contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(entry.label))
                        }
                    }
                }
            }
            TvActionButton(
                text = stringResource(R.string.action_refresh),
                icon = R.drawable.ic_refresh,
                onClick = {
                    when (tab) {
                        ListTab.TORRENTS -> torrents.refresh()
                        ListTab.WEB -> webDownloads.refresh()
                        ListTab.USENET -> usenetDownloads.refresh()
                    }
                },
            )
        }

        Spacer(Modifier.size(16.dp))

        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            if (!(hasDetail && detailFullScreen)) {
                Column(
                    modifier =
                        Modifier
                            .weight(if (hasDetail) 0.42f else 1f)
                            .fillMaxHeight()
                            .focusRequester(listFocus),
                ) {
                    TvTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = stringResource(R.string.find_in_list_hint),
                    )
                    Spacer(Modifier.size(12.dp))
                    when (tab) {
                        ListTab.TORRENTS ->
                            PagedDownloads(
                                items = torrents,
                                query = query,
                                selectedId = uiState.selectedTorrent?.id,
                                toRow = { it.toRow() },
                                onClick = loadTorrentDetails,
                            )

                        ListTab.WEB ->
                            PagedDownloads(
                                items = webDownloads,
                                query = query,
                                selectedId = uiState.selectedWeb?.id,
                                toRow = { it.toRow() },
                                onClick = loadWebDetails,
                            )

                        ListTab.USENET ->
                            PagedDownloads(
                                items = usenetDownloads,
                                query = query,
                                selectedId = uiState.selectedUsenet?.id,
                                toRow = { it.toRow() },
                                onClick = loadUsenetDetails,
                            )
                    }
                }
            }

            if (hasDetail) {
                Box(
                    modifier =
                        Modifier
                            .weight(if (detailFullScreen) 1f else 0.58f)
                            .fillMaxHeight()
                            .focusRequester(detailFocus),
                ) {
                    val toggle = { detailFullScreen = !detailFullScreen }
                    when (tab) {
                        ListTab.TORRENTS ->
                            uiState.selectedTorrent?.let {
                                TvTorrentDetailScreen(
                                    item = it,
                                    onClose = closeDetail,
                                    isFullScreen = detailFullScreen,
                                    onToggleFullScreen = toggle,
                                )
                            }

                        ListTab.WEB ->
                            uiState.selectedWeb?.let {
                                TvWebDownloadDetailScreen(
                                    item = it,
                                    onClose = closeDetail,
                                    isFullScreen = detailFullScreen,
                                    onToggleFullScreen = toggle,
                                    onDeleted = {
                                        closeDetail()
                                        webDownloads.refresh()
                                    },
                                )
                            }

                        ListTab.USENET ->
                            uiState.selectedUsenet?.let {
                                TvUsenetDetailScreen(
                                    item = it,
                                    onClose = closeDetail,
                                    isFullScreen = detailFullScreen,
                                    onToggleFullScreen = toggle,
                                    onDeleted = {
                                        closeDetail()
                                        usenetDownloads.refresh()
                                    },
                                )
                            }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T : Any> PagedDownloads(
    items: LazyPagingItems<T>,
    query: String,
    selectedId: Int?,
    toRow: (T) -> RowData,
    onClick: (T) -> Unit,
) {
    val refresh = items.loadState.refresh
    when {
        refresh is LoadState.Loading && items.itemCount == 0 -> {
            LoadingBox()
            return
        }

        refresh is LoadState.Error && items.itemCount == 0 -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ErrorBanner(refresh.error.message ?: "Loading failed")
                TvActionButton(text = stringResource(R.string.action_refresh), icon = R.drawable.ic_refresh, onClick = items::retry)
            }
            return
        }

        refresh is LoadState.NotLoading && items.itemCount == 0 -> {
            EmptyState(text = stringResource(R.string.search_no_results))
            return
        }
    }

    // when filtering, only the already loaded items are searched (peek doesn't trigger new pages)
    val trimmed = query.trim()
    val filteredIndexes by remember(items.itemCount, trimmed, items.itemSnapshotList) {
        derivedStateOf {
            if (trimmed.isEmpty()) {
                null
            } else {
                (0 until items.itemCount).filter { index ->
                    items.peek(index)?.let { toRow(it).name.contains(trimmed, ignoreCase = true) } == true
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        val indexes = filteredIndexes
        if (indexes != null) {
            if (indexes.isEmpty()) {
                item { EmptyState(text = stringResource(R.string.find_in_list_no_matches)) }
            }
            items(indexes, key = { it }) { index ->
                items.peek(index)?.let { value ->
                    val row = toRow(value)
                    DownloadRow(row = row, selected = row.id == selectedId, onClick = { onClick(value) })
                }
            }
        } else {
            items(count = items.itemCount) { index ->
                val value = items[index]
                if (value != null) {
                    val row = toRow(value)
                    DownloadRow(row = row, selected = row.id == selectedId, onClick = { onClick(value) })
                } else {
                    DownloadRowPlaceholder()
                }
            }
        }
        if (items.loadState.append is LoadState.Loading) {
            item {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        val append = items.loadState.append
        if (append is LoadState.Error) {
            item {
                TvActionButton(text = stringResource(R.string.action_refresh), icon = R.drawable.ic_refresh, onClick = items::retry)
            }
        }
    }
}

@Composable
private fun DownloadRow(
    row: RowData,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        selected = selected,
        onClick = onClick,
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(convertToSize(row.size))
                    Badge(row.state)
                    if (row.cached) Badge(stringResource(R.string.cached), color = MaterialTheme.colorScheme.tertiary)
                }
                if (row.progress < 1.0) {
                    LinearProgressIndicator(
                        progress = { row.progress.toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        headlineContent = { Text(row.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
    )
}

@Composable
private fun DownloadRowPlaceholder() {
    ListItem(
        selected = false,
        onClick = {},
        enabled = false,
        headlineContent = { Text("…") },
    )
}

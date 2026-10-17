package com.github.livingwithhippos.nullseek.ui.lists.torrent

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.compositeOver
import kotlinx.coroutines.launch
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.ui.lists.utils.findMatchIndices
import com.github.livingwithhippos.nullseek.ui.lists.utils.wrapMatchPosition
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.github.livingwithhippos.nullseek.core.utils.mockTorrents
import com.github.livingwithhippos.nullseek.ui.lists.utils.PagingRefreshState
import com.github.livingwithhippos.nullseek.ui.lists.utils.pagingAppendState
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType
import com.github.livingwithhippos.nullseek.utils.convertToSize
import com.github.livingwithhippos.torbox.model.TorrentDto
import kotlinx.coroutines.flow.Flow

@Composable
fun TorrentsScreen(
    modifier: Modifier = Modifier,
    contentType: NullContentType,
    navigationType: NullNavigationType,
    torrent: TorrentDto?,
    flow: Flow<PagingData<TorrentDto>>,
    loadDetails: (torrent: TorrentDto) -> Unit,
    closeDetails: () -> Unit
) {
    val context = LocalContext.current
    // kept here so the query survives opening/closing the details
    var query by rememberSaveable { mutableStateOf("") }

    // todo: manage showing 2 panes with the torrent details if selected

    if (torrent != null) {
        TorrentDetailScreen(
            modifier = modifier,
            contentType = contentType,
            navigationType = navigationType,
            item = torrent,
            closeDetails = closeDetails,
        )
    } else {
        TorrentsScreen(
            modifier = modifier,
            context = context,
            flow = flow,
            query = query,
            onQueryChange = { query = it },
            loadDetails = loadDetails
        )
    }
}

@Composable
private fun TorrentsScreen(
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
    flow: Flow<PagingData<TorrentDto>>,
    query: String,
    onQueryChange: (String) -> Unit,
    loadDetails: (torrent: TorrentDto) -> Unit
) {
    val listState: LazyListState = rememberLazyListState()
    val lazyPagingItems = flow.collectAsLazyPagingItems()
    val scope = rememberCoroutineScope()

    // only the currently loaded items are searched
    val matches by remember(lazyPagingItems, query) {
        derivedStateOf {
            findMatchIndices(
                lazyPagingItems.itemSnapshotList.map { it?.name },
                query
            )
        }
    }
    var position by remember { mutableIntStateOf(0) }
    val refreshState = lazyPagingItems.loadState.refresh

    LaunchedEffect(query, refreshState is LoadState.Loading) {
        position = 0
    }
    LaunchedEffect(matches.size) {
        if (position >= matches.size) position = 0
    }

    fun move(step: Int) {
        if (matches.isEmpty()) return
        position = wrapMatchPosition(position, step, matches.size)
        val target = matches[position]
        scope.launch { listState.animateScrollToItem(target) }
    }

    val currentMatch = matches.getOrNull(position)
    val matchSet = remember(matches) { matches.toSet() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FindBar(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(top = 8.dp),
            query = query,
            onQueryChange = onQueryChange,
            matchCount = matches.size,
            position = position,
            onPrevious = { move(-1) },
            onNext = { move(1) },
        )
    PullToRefreshBox(
        isRefreshing = lazyPagingItems.loadState.refresh is LoadState.Loading,
        onRefresh = { lazyPagingItems.refresh() },
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
        ) {
            items(
                lazyPagingItems.itemCount, key = lazyPagingItems.itemKey { it.id }) { index ->
                val torrent = lazyPagingItems[index]
                if (torrent != null) {
                    TorrentRow(
                        torrent,
                        loadDetails = loadDetails,
                        highlight = when {
                            index == currentMatch -> MatchHighlight.CURRENT
                            index in matchSet -> MatchHighlight.MATCH
                            else -> MatchHighlight.NONE
                        }
                    )
                } else {
                    TorrentRowPlaceholder()
                }
            }
            pagingAppendState(lazyPagingItems.loadState.append, lazyPagingItems::retry)
        }
        PagingRefreshState(lazyPagingItems.loadState.refresh, lazyPagingItems::retry)
    }
    }
}

private enum class MatchHighlight { NONE, MATCH, CURRENT }

@Composable
private fun FindBar(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    matchCount: Int,
    position: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val hasQuery = query.isNotBlank()
    val canMove = matchCount > 0
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.find_in_list_hint)) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onNext() }),
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasQuery) {
                    Text(
                        text = if (canMove) stringResource(
                            R.string.find_in_list_counter, position + 1, matchCount
                        ) else stringResource(R.string.find_in_list_no_matches),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                IconButton(onClick = onPrevious, enabled = canMove) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_drop_up),
                        contentDescription = stringResource(R.string.find_in_list_previous),
                    )
                }
                IconButton(onClick = onNext, enabled = canMove) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_drop_down),
                        contentDescription = stringResource(R.string.find_in_list_next),
                    )
                }
                if (hasQuery) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.find_in_list_clear),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun TorrentRow(
    item: TorrentDto,
    modifier: Modifier = Modifier,
    loadDetails: (torrent: TorrentDto) -> Unit,
    highlight: MatchHighlight = MatchHighlight.NONE,
) {
    val primary = MaterialTheme.colorScheme.primary
    OutlinedCard(
        onClick = { loadDetails(item) },
        modifier = modifier.fillMaxWidth(),
        colors = when (highlight) {
            MatchHighlight.CURRENT -> CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
            MatchHighlight.MATCH -> CardDefaults.outlinedCardColors(
                containerColor = primary.copy(alpha = 0.08f)
                    .compositeOver(MaterialTheme.colorScheme.surface)
            )
            MatchHighlight.NONE -> CardDefaults.outlinedCardColors()
        },
        border = when (highlight) {
            MatchHighlight.CURRENT -> BorderStroke(2.dp, primary)
            MatchHighlight.MATCH -> BorderStroke(1.dp, primary.copy(alpha = 0.5f))
            MatchHighlight.NONE -> CardDefaults.outlinedCardBorder()
        },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header: Name and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                Spacer(modifier = Modifier.width(8.dp))

                val isDone = item.downloadFinished || item.progress >= 1.0
                val badgeContainerColor =
                    when {
                        isDone -> MaterialTheme.colorScheme.primaryContainer
                        item.cached -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                val badgeContentColor =
                    when {
                        isDone -> MaterialTheme.colorScheme.onPrimaryContainer
                        item.cached -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                val badgeText =
                    when {
                        isDone -> "Ready"
                        item.cached -> "Cached"
                        item.downloadState.isNotBlank() -> item.downloadState.replaceFirstChar { it.uppercase() }
                        else -> "${(item.progress * 100).toInt()}%"
                    }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeContainerColor,
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeContentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            // Slim progress bar for in-progress downloads
            val isDownloading = !item.downloadFinished && item.progress < 1.0
            if (isDownloading) {
                LinearProgressIndicator(
                    progress = { item.progress.toFloat().coerceIn(0f, 1f) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                )
            }

            // Bottom Metadata Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = convertToSize(item.size),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (item.seeds > 0) {
                        Text(
                            text = " • ${item.seeds} seeds",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (isDownloading && item.downloadSpeed > 0) {
                    Text(
                        text = "${item.downloadSpeed} kB/s",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else if (!isDownloading) {
                    Text(
                        text = if (item.active) "Active" else "Inactive",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TorrentRowPlaceholder() {
    OutlinedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(96.dp),
    ) {}
}


@Preview
@Composable
fun TorrentScreenPreview() {
    NullSeekTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FindBar(
                    modifier = Modifier.fillMaxWidth(),
                    query = "ubuntu",
                    onQueryChange = {},
                    matchCount = 2,
                    position = 0,
                    onPrevious = {},
                    onNext = {},
                )
                mockTorrents.forEachIndexed { i, it ->
                    TorrentRow(
                        it,
                        loadDetails = { },
                        highlight = when (i) {
                            0 -> MatchHighlight.CURRENT
                            1 -> MatchHighlight.MATCH
                            else -> MatchHighlight.NONE
                        }
                    )
                }
            }
        }
    }
}

package com.github.livingwithhippos.nullseek.ui.lists.torrent

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.github.livingwithhippos.nullseek.core.utils.mockTorrent
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType
import com.github.livingwithhippos.nullseek.ui.lists.utils.PagingRefreshState
import com.github.livingwithhippos.nullseek.ui.lists.utils.pagingAppendState
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
            modifier = modifier, context = context, flow = flow, loadDetails = loadDetails
        )
    }
}

@Composable
private fun TorrentsScreen(
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
    flow: Flow<PagingData<TorrentDto>>,
    loadDetails: (torrent: TorrentDto) -> Unit
) {
    val listState: LazyListState = rememberLazyListState()
    val lazyPagingItems = flow.collectAsLazyPagingItems()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(
                lazyPagingItems.itemCount, key = lazyPagingItems.itemKey { it.id }) { index ->
                val torrent = lazyPagingItems[index]
                if (torrent != null) {
                    TorrentRow(torrent, loadDetails = loadDetails)
                } else {
                    TorrentRowPlaceholder()
                }
            }
            pagingAppendState(lazyPagingItems.loadState.append, lazyPagingItems::retry)
        }
        PagingRefreshState(lazyPagingItems.loadState.refresh, lazyPagingItems::retry)
    }
}

@Composable
private fun TorrentRow(
    item: TorrentDto,
    modifier: Modifier = Modifier,
    loadDetails: (torrent: TorrentDto) -> Unit
) {
    ElevatedCard(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ), modifier = modifier,
        onClick = { loadDetails(item) }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {

            Text(
                text = item.name,
                textAlign = TextAlign.Start,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // todo: pick colors for different states
                item {
                    // todo: remove this if we don't merge the list with web and usenet
                    AssistChip(onClick = { }, label = { Text("torrent") })
                }

                item {
                    AssistChip(onClick = { }, label = {
                        Text(text = item.downloadState)
                    })
                }

                if (item.downloadFinished) {
                    item {
                        AssistChip(onClick = { }, label = {
                            Text(text = "Download Ready")
                        })
                    }
                }
            }

            Text(
                text = "Total Size: ${convertToSize(item.size)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun TorrentRowPlaceholder() {
    ElevatedCard(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ), modifier = Modifier.height(100.dp)
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
                TorrentRow(
                    mockTorrent,
                    loadDetails = {}
                )
                TorrentRow(
                    mockTorrent,
                    loadDetails = {}
                )
            }
        }
    }
}

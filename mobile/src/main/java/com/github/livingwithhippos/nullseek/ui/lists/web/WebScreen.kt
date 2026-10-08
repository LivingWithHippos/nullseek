package com.github.livingwithhippos.nullseek.ui.lists.web

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

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.paging.LoadState

import com.github.livingwithhippos.nullseek.core.utils.mockWebDownload
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType
import com.github.livingwithhippos.nullseek.ui.lists.utils.PagingRefreshState
import com.github.livingwithhippos.nullseek.ui.lists.utils.pagingAppendState
import com.github.livingwithhippos.nullseek.utils.convertToSize
import com.github.livingwithhippos.torbox.model.WebDownloadDto
import kotlinx.coroutines.flow.Flow

@Composable
fun WebScreen(
    modifier: Modifier = Modifier,
    contentType: NullContentType,
    navigationType: NullNavigationType,
    webDownload: WebDownloadDto?,
    flow: Flow<PagingData<WebDownloadDto>>,
    loadDetails: (webDownload: WebDownloadDto) -> Unit,
    closeDetails: () -> Unit
) {
    val context = LocalContext.current

    if (webDownload != null) {
        WebDownloadDetailScreen(
            modifier = modifier,
            contentType = contentType,
            navigationType = navigationType,
            webDownload = webDownload,
            closeDetails = closeDetails,
        )
    } else {
        WebDownloadsScreen(
            modifier = modifier, context = context, flow = flow, loadDetails = loadDetails
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebDownloadsScreen(
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
    flow: Flow<PagingData<WebDownloadDto>>,
    loadDetails: (webDownload: WebDownloadDto) -> Unit
) {
    val listState: LazyListState = rememberLazyListState()
    val lazyPagingItems = flow.collectAsLazyPagingItems()
    PullToRefreshBox(
        isRefreshing = lazyPagingItems.loadState.refresh is LoadState.Loading,
        onRefresh = { lazyPagingItems.refresh() },
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
                val webDownload = lazyPagingItems[index]
                if (webDownload != null) {
                    WebDownloadRow(webDownload, loadDetails = loadDetails)
                } else {
                    WebDownloadRowPlaceholder()
                }
            }
            pagingAppendState(lazyPagingItems.loadState.append, lazyPagingItems::retry)
        }
        PagingRefreshState(lazyPagingItems.loadState.refresh, lazyPagingItems::retry)
    }
}

@Composable
private fun WebDownloadRow(
    item: WebDownloadDto,
    modifier: Modifier = Modifier,
    loadDetails: (webDownload: WebDownloadDto) -> Unit
) {
    // other possible details:
    //  files number, cached
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
                item {
                    AssistChip(onClick = { }, label = { Text("web") })
                }

                if (item.error !== null) {
                    // todo: color
                    item {
                        AssistChip(onClick = { }, label = {
                            Text(text ="error")
                        })
                    }
                }

                item {
                    AssistChip(onClick = { }, label = {
                        Text(text = item.downloadState)
                    })
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
private fun WebDownloadRowPlaceholder() {
    ElevatedCard(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ), modifier = Modifier.height(100.dp)
    ) {}
}


@Preview
@Composable
fun WebDownloadsScreenPreview() {
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
                WebDownloadRow(
                    mockWebDownload,
                    loadDetails = {}
                )
                WebDownloadRow(
                    mockWebDownload,
                    loadDetails = {}
                )
            }
        }
    }
}

package com.github.livingwithhippos.nullseek.ui.lists.usenet

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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

import com.github.livingwithhippos.nullseek.core.utils.mockUsenetDownload
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType
import com.github.livingwithhippos.nullseek.ui.lists.utils.PagingRefreshState
import com.github.livingwithhippos.nullseek.ui.lists.utils.pagingAppendState
import com.github.livingwithhippos.nullseek.utils.convertToSize
import com.github.livingwithhippos.torbox.model.UsenetDownloadDto
import kotlinx.coroutines.flow.Flow

@Composable
fun UsenetScreen(
    modifier: Modifier = Modifier,
    contentType: NullContentType,
    navigationType: NullNavigationType,
    usenet: UsenetDownloadDto?,
    flow: Flow<PagingData<UsenetDownloadDto>>,
    loadDetails: (usenetDownload: UsenetDownloadDto) -> Unit,
    closeDetails: () -> Unit
) {
    val context = LocalContext.current

    if (usenet != null) {
        UsenetDetailScreen(
            modifier = modifier,
            contentType = contentType,
            navigationType = navigationType,
            item = usenet,
            closeDetails = closeDetails,
        )
    } else {
        UsenetScreen(
            modifier = modifier, context = context, flow = flow, loadDetails = loadDetails
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsenetScreen(
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
    flow: Flow<PagingData<UsenetDownloadDto>>,
    loadDetails: (usenetDownload: UsenetDownloadDto) -> Unit
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
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
        ) {
            items(
                lazyPagingItems.itemCount, key = lazyPagingItems.itemKey { it.id }) { index ->
                val usenet = lazyPagingItems[index]
                if (usenet != null) {
                    UsenetRow(usenet, loadDetails = loadDetails)
                } else {
                    UsenetRowPlaceholder()
                }
            }
            pagingAppendState(lazyPagingItems.loadState.append, lazyPagingItems::retry)
        }
        PagingRefreshState(lazyPagingItems.loadState.refresh, lazyPagingItems::retry)
    }
}



@Composable
private fun UsenetRow(
    item: UsenetDownloadDto,
    modifier: Modifier = Modifier,
    loadDetails: (usenetDownload: UsenetDownloadDto) -> Unit,
) {
    OutlinedCard(
        onClick = { loadDetails(item) },
        modifier = modifier.fillMaxWidth(),
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
                Text(
                    text = convertToSize(item.size),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

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
private fun UsenetRowPlaceholder() {
    OutlinedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(96.dp),
    ) {}
}


@Preview
@Composable
fun UsenetScreenPreview() {
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

                UsenetRow(
                    mockUsenetDownload,
                    loadDetails = {}
                )
                UsenetRow(
                    mockUsenetDownload,
                    loadDetails = {}
                )
            }
        }
    }
}
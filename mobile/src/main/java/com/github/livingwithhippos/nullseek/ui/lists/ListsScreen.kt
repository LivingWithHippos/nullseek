package com.github.livingwithhippos.nullseek.ui.lists

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import com.github.livingwithhippos.nullseek.NullRootUiState
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.ui.lists.torrent.TorrentsScreen
import com.github.livingwithhippos.nullseek.ui.lists.web.WebScreen
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType
import com.github.livingwithhippos.torbox.model.TorrentDto
import com.github.livingwithhippos.torbox.model.UsenetDownloadDto
import com.github.livingwithhippos.torbox.model.WebDownloadDto
import kotlinx.coroutines.flow.Flow
import org.koin.androidx.compose.koinViewModel

enum class Destination(
    val label: String, @DrawableRes val icon: Int, val contentDescription: String
) {
    // todo: update icons
    TORRENTS("Torrents", R.drawable.ic_magnet, "Torrents"), WEB(
        "Web",
        R.drawable.ic_file,
        "Web"
    ),
    USENET("Usenet", R.drawable.ic_edit, "Usenet")
}

@Composable
fun ListsScreen(
    modifier: Modifier = Modifier,
    contentType: NullContentType,
    navigationType: NullNavigationType,
    loadTorrentDetails: (torrent: TorrentDto) -> Unit,
    loadWebDetails: (webDownload: WebDownloadDto) -> Unit,
    loadUsenetDetails: (usenetDownload: UsenetDownloadDto) -> Unit,
    closeDetails: () -> Unit,
    uiState: NullRootUiState,
    viewModel: ListsViewModel = koinViewModel()
) {
    val torrentFlow: Flow<PagingData<TorrentDto>> = viewModel.torrentsPagingFlow
    val webFlow: Flow<PagingData<WebDownloadDto>> = viewModel.webPagingFlow
    val usenetFlow: Flow<PagingData<UsenetDownloadDto>> = viewModel.usenetPagingFlow

    // todo: single pane with multiple data from torrent + download + usenet
    //  will also need a detail page on the right side  org.koin.compose.viewmodel.if double sided, or just opening a new page if single sided


    // todo: decide if we should use a second navController instead, we'd have a separate backstack
    val startDestination = Destination.TORRENTS
    var selectedDestination by rememberSaveable { mutableIntStateOf(startDestination.ordinal) }

    Column(modifier = modifier) {
        PrimaryTabRow(selectedTabIndex = selectedDestination, modifier = Modifier.padding(10.dp)) {
            Destination.entries.forEachIndexed { index, destination ->
                Tab(
                    // todo: add icon to see if it's better
                    selected = selectedDestination == index, onClick = {
                        selectedDestination = index
                    }, text = {
                        Text(
                            text = destination.label, maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                    })
            }
        }
        // todo: decide if we should use a single viewModel and pass the functions/lists down
        when (Destination.entries[selectedDestination]) {
            Destination.TORRENTS -> {
                TorrentsScreen(
                    modifier = modifier,
                    contentType = contentType,
                    navigationType = navigationType,
                    loadDetails = loadTorrentDetails,
                    closeDetails = closeDetails,
                    torrent = uiState.selectedTorrent,
                    flow = torrentFlow
                )
            }

            Destination.WEB -> {
                WebScreen(
                    modifier = modifier,
                    contentType = contentType,
                    navigationType = navigationType,
                    loadDetails = loadWebDetails,
                    closeDetails = closeDetails,
                    webDownload = uiState.selectedWeb,
                    flow = webFlow
                )
            }

            Destination.USENET -> {
                // UsenetScreen()
            }
        }
    }
}

@Preview
@Composable
fun ListsScreenPreview() {
    ListsScreen(
        contentType = NullContentType.SINGLE_PANE,
        uiState = NullRootUiState(),
        navigationType = NullNavigationType.BOTTOM_NAVIGATION,
        loadTorrentDetails = {},
        loadWebDetails = {},
        loadUsenetDetails = {},
        closeDetails = {}
    )
}
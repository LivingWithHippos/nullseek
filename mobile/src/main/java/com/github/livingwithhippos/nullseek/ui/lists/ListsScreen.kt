package com.github.livingwithhippos.nullseek.ui.lists

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.github.livingwithhippos.nullseek.NullRootUiState
import com.github.livingwithhippos.nullseek.ui.lists.torrent.TorrentsScreen
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType
import com.github.livingwithhippos.torbox.model.TorrentDto


@Composable
fun ListsScreen(
    modifier: Modifier = Modifier,
    contentType: NullContentType,
    navigationType: NullNavigationType,
    loadTorrentDetails: (torrent: TorrentDto) -> Unit,
    closeDetails: () -> Unit,
    uiState: NullRootUiState
) {

    // todo: single pane with multiple data from torrent + download + usenet
    //  need to check how to use paging with three sources
    //  at worst provide a selection button to switch
    //  will also need a detail page on the right side if double sided, or just opening a new page if single sided

    Surface {
        TorrentsScreen(
            modifier = modifier,
            contentType = contentType,
            navigationType = navigationType,
            loadDetails = loadTorrentDetails,
            closeDetails = closeDetails,
            torrent = uiState.selectedTorrent
        )
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
        closeDetails = {}
    )
}
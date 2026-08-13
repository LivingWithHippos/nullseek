package com.github.livingwithhippos.nullseek.ui.lists

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.github.livingwithhippos.nullseek.NullRootUiState
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType


@Composable
fun ListsScreen(
    modifier: Modifier = Modifier,
    contentType: NullContentType,
    navigationType: NullNavigationType,
    uiState: NullRootUiState
) {

    // todo: single pane with multiple data from torrent + download + usenet
    //  need to check how to use paging with three sources
    //  at worst provide a selection button to switch

    Surface {
        TorrentsScreen(
            modifier = modifier,
            contentType = contentType,
            navigationType = navigationType,
            uiState = uiState
        )
    }
}

@Preview
@Composable
fun ListsScreenPreview() {
    ListsScreen(
        contentType = NullContentType.SINGLE_PANE,
        uiState = NullRootUiState(),
        navigationType = NullNavigationType.BOTTOM_NAVIGATION
    )
}
package com.github.livingwithhippos.nullseek.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.github.livingwithhippos.nullseek.NullRootUiState
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType

@Composable
fun SearchScreen(
    contentType: NullContentType,
    uiState: NullRootUiState,
    navigationType: NullNavigationType,
    modifier: Modifier = Modifier,
) {
    Surface {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Search Screen", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Preview
@Composable
fun SearchScreenPreview() {
    SearchScreen(
        contentType = NullContentType.SINGLE_PANE,
        uiState = NullRootUiState(),
        navigationType = NullNavigationType.BOTTOM_NAVIGATION
    )
}
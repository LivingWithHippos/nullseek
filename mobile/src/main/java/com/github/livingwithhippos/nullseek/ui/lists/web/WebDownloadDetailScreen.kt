package com.github.livingwithhippos.nullseek.ui.lists.web

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.core.utils.mockWebDownload
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.ui.utils.IconText
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType
import com.github.livingwithhippos.nullseek.utils.convertToSize
import com.github.livingwithhippos.torbox.model.WebDownloadDto


@Composable
fun WebDownloadDetailScreen(
    modifier: Modifier = Modifier,
    contentType: NullContentType,
    navigationType: NullNavigationType,
    webDownload: WebDownloadDto,
    closeDetails: () -> Unit,
) {
    BackHandler {
        closeDetails()
    }

    WebDownloadDetailScreen(
        modifier = modifier,
        context = LocalContext.current,
        webDownload = webDownload
    )
}

@Composable
private fun WebDownloadDetailScreen(
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
    webDownload: WebDownloadDto,
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column {
            Text(
                text = webDownload.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 8.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Size: ${convertToSize(webDownload.size)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            if (webDownload.active) {
                AssistChip(onClick = { }, label = { Text("active") })
            }

            if (webDownload.cached) {
                AssistChip(onClick = { }, label = { Text("cached") })
            }

            IconText(
                text = "File",
                icon = R.drawable.ic_file,
            )

            ButtonGroup(
                overflowIndicator = { menuState ->
                    ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                },
                verticalAlignment = Alignment.Top,
            ) {
                clickableItem(
                    label = "Download",
                    onClick = { /* Handle click */ }
                )
                clickableItem(
                    label = "Download Zip",
                    onClick = { /* Handle click */ }
                )
                clickableItem(
                    label = "Share",
                    onClick = { /* Handle click */ }
                )
                clickableItem(
                    label = "Copy",
                    onClick = { /* Handle click */ }
                )
                clickableItem(
                    label = "Delete",
                    onClick = { /* Handle click */ }
                )
            }
        }
    }

}


@Preview
@Composable
fun WebDownloadDetailScreenPreview() {
    NullSeekTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            WebDownloadDetailScreen(
                webDownload = mockWebDownload
            )
        }
    }
}
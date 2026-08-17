package com.github.livingwithhippos.nullseek.ui.lists.usenet

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
import com.github.livingwithhippos.nullseek.core.utils.mockUsenetDownload
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.ui.utils.IconText
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType
import com.github.livingwithhippos.nullseek.utils.convertToSize
import com.github.livingwithhippos.torbox.model.UsenetDownloadDto

@Composable
fun  UsenetDetailScreen(
    modifier: Modifier = Modifier,
    contentType: NullContentType,
    navigationType: NullNavigationType,
    item: UsenetDownloadDto,
    closeDetails: () -> Unit,
) {
    BackHandler {
        closeDetails()
    }
    UsenetDetailScreen(
        modifier = modifier,
        context = LocalContext.current,
        item = item
    )
}


@Composable
private fun UsenetDetailScreen(
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
    item: UsenetDownloadDto,
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 8.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Size: ${convertToSize(item.size)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            if (item.active) {
                AssistChip(onClick = { }, label = { Text("active") })
            }

            if (item.cached) {
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
fun UsenetDetailScreenPreview() {
    NullSeekTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            UsenetDetailScreen(
                item = mockUsenetDownload
            )
        }
    }

}
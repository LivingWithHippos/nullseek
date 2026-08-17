package com.github.livingwithhippos.nullseek.ui.lists.torrent

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
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
import com.github.livingwithhippos.nullseek.core.utils.mockTorrent
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.ui.utils.CardStat
import com.github.livingwithhippos.nullseek.ui.utils.IconText
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType
import com.github.livingwithhippos.nullseek.utils.convertToSize
import com.github.livingwithhippos.torbox.model.TorrentDto

@Composable
fun TorrentDetailScreen(
    modifier: Modifier = Modifier,
    contentType: NullContentType,
    navigationType: NullNavigationType,
    item: TorrentDto,
    closeDetails: () -> Unit,
) {
    BackHandler {
        closeDetails()
    }

    TorrentDetailScreen(
        modifier = modifier,
        context = LocalContext.current,
        item = item
    )
}

@Composable
private fun TorrentDetailScreen(
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
    item: TorrentDto,
) {

    // todo:
    //  - buttons: download, download as zip, airlock, delete, share, copy etc
    //  - files list at the bottom, with stream + copy + download buttons (and then share etc)

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

            val magnet = item.magnet

            IconText(
                text = "Magnet",
                icon = R.drawable.ic_magnet,
            )

            // todo: the magnet can be recovered with another API
            ButtonGroup(
                overflowIndicator = { menuState ->
                    ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                },
                verticalAlignment = Alignment.Top,
            ) {
                clickableItem(
                    label = "Copy",
                    onClick = { /* Handle click */ }
                )
                clickableItem(
                    label = "Share",
                    onClick = { /* Handle click */ }
                )
                clickableItem(
                    label = "Open",
                    onClick = { /* Handle click */ }
                )
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

            TorrentProgress(
                progress = item.progress,
                completed = item.downloadFinished,
                // todo: check speeds type
                downloadSpeed = item.downloadSpeed,
                uploadSpeed = item.uploadSpeed,
                // todo: check eta type
                eta = item.eta,
                seeds = item.seeds,
                peers = item.peers
            )
        }
    }

}

@Composable
fun TorrentProgress(
    progress: Double,
    completed: Boolean,
    downloadSpeed: Long,
    uploadSpeed: Long,
    eta: Int,
    seeds: Int,
    peers: Int
) {
    if (progress >= 1 || completed) {
        return
    }

    Card {
        Row {
            CardStat(
                titles = listOf("Progress"),
                description = "${(progress * 100).toInt()}%"
            )
            CardStat(
                titles = listOf("Seeds"),
                description = "$seeds"
            )
            CardStat(
                titles = listOf("Peers"),
                description = "$peers"
            )
        }

        Row {
            CardStat(
                titles = listOf("ETA"),
                description = "$eta s"
            )
            CardStat(
                titles = listOf("Download Speed"),
                description = "$downloadSpeed kB/s"
            )
            CardStat(
                titles = listOf("Upload Speed"),
                description = "$uploadSpeed kB/s"
            )
        }

    }

}

@Preview
@Composable
fun TorrentDetailScreenPreview() {
    NullSeekTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            TorrentDetailScreen(
                item = mockTorrent
            )
        }
    }

}
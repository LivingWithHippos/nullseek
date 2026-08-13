package com.github.livingwithhippos.nullseek.ui.lists.torrent

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
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
import com.github.livingwithhippos.nullseek.ui.utils.CopyButton
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType
import com.github.livingwithhippos.nullseek.utils.convertToSize
import com.github.livingwithhippos.torbox.model.TorrentDto

@Composable
fun TorrentDetailScreen(
    modifier: Modifier = Modifier,
    contentType: NullContentType,
    navigationType: NullNavigationType,
    torrent: TorrentDto,
    viewModel: TorrentsViewModel
) {
    TorrentDetailScreen(
        modifier = modifier,
        context = LocalContext.current,
        torrent = torrent
    )
}

@Composable
private fun TorrentDetailScreen(
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
    torrent: TorrentDto,
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
                text = torrent.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 8.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Size: ${convertToSize(torrent.size)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            if (torrent.active) {
                AssistChip(onClick = { }, label = { Text("active") })
            }

            if (torrent.cached) {
                AssistChip(onClick = { }, label = { Text("cached") })
            }

            val magnet = torrent.magnet

            CopyButton(
                text = magnet,
                description = "Copy Magnet",
                customIcon = R.drawable.ic_magnet,
                context = context
            )

            TorrentProgress(
                progress = torrent.progress,
                completed = torrent.downloadFinished,
                // todo: check speeds type
                downloadSpeed = torrent.downloadSpeed,
                uploadSpeed = torrent.uploadSpeed,
                // todo: check eta type
                eta = torrent.eta,
                seeds = torrent.seeds,
                peers = torrent.peers
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
                title = "Progress",
                description = "${(progress * 100).toInt()}%"
            )
            CardStat(
                title = "Seeds",
                description = "$seeds"
            )
            CardStat(
                title = "Peers",
                description = "$peers"
            )
        }

        Row {
            CardStat(
                title = "ETA",
                description = "$eta s"
            )
            CardStat(
                title = "Download Speed",
                description = "$downloadSpeed kB/s"
            )
            CardStat(
                title = "Upload Speed",
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
                torrent = mockTorrent
            )
        }
    }

}
package com.github.livingwithhippos.nullseek.ui.search

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.core.utils.convertToSize
import com.github.livingwithhippos.nullseek.data.model.SearchResult
import com.github.livingwithhippos.nullseek.data.model.preferredLink
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.utils.setClipboardText
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchResultDetailScreen(
    result: SearchResult,
    onClose: () -> Unit,
    onDownload: (link: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val link = result.preferredLink()
    val linkCopied = stringResource(R.string.search_detail_link_copied)

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.search_detail_back),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SelectionContainer {
                Text(text = result.title, style = MaterialTheme.typography.titleLarge)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AssistChip(onClick = {}, label = { Text(result.indexerName) })
                AssistChip(onClick = {}, label = { Text(convertToSize(result.sizeBytes)) })
            }
            Text(
                text = stringResource(R.string.search_result_seeders_peers, result.seeders, result.peers),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            result.publishedAt?.let { published ->
                val absolute = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(published))
                val relative = ageText(published)
                Text(
                    text = stringResource(R.string.search_detail_published, absolute) +
                        (relative?.let { " ($it)" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (result.categories.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    result.categories.forEach { SuggestionChip(onClick = {}, label = { Text(it) }) }
                }
            }
            if (link != null) {
                Text(
                    text = stringResource(R.string.search_detail_link),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = link,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            setClipboardText(context, link)
                            Toast.makeText(context, linkCopied, Toast.LENGTH_SHORT).show()
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_copy),
                            contentDescription = stringResource(R.string.search_detail_copy_link),
                        )
                    }
                }
            }
            if (!result.infoUrl.isNullOrBlank()) {
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, result.infoUrl.toUri()))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.search_detail_open_info))
                }
            }
        }
        Button(
            onClick = { link?.let(onDownload) },
            enabled = link != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(stringResource(R.string.search_detail_download))
        }
    }
}

@Preview
@Composable
fun SearchResultDetailMagnetPreview() {
    NullSeekTheme {
        Surface {
            SearchResultDetailScreen(
                result = SearchResult(
                    "Ubuntu 24.04 Desktop amd64", "Jackett", 6_200_000_000, 120, 15,
                    System.currentTimeMillis() - 3 * 86_400_000L,
                    magnetLink = "magnet:?xt=urn:btih:abcdef&dn=Ubuntu%2024.04&tr=udp%3A%2F%2Ftracker",
                    infoUrl = "https://example.com/ubuntu",
                    categories = listOf("Software", "Linux"),
                ),
                onClose = {},
                onDownload = {},
            )
        }
    }
}

@Preview
@Composable
fun SearchResultDetailTorrentPreview() {
    NullSeekTheme {
        Surface {
            SearchResultDetailScreen(
                result = SearchResult(
                    "Debian 12 netinst", "Prowlarr", 650_000_000, 80, 4, null,
                    downloadLink = "https://example.com/download/debian.torrent",
                ),
                onClose = {},
                onDownload = {},
            )
        }
    }
}

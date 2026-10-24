package com.github.livingwithhippos.nullseek.ui.search

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.core.utils.convertToSize
import com.github.livingwithhippos.nullseek.data.model.ExternalService
import com.github.livingwithhippos.nullseek.data.model.ExternalServiceType
import com.github.livingwithhippos.nullseek.data.model.SearchResult
import com.github.livingwithhippos.nullseek.ui.externalservices.PlayChoiceDialog
import com.github.livingwithhippos.nullseek.ui.theme.AssistChip
import com.github.livingwithhippos.nullseek.ui.theme.Button
import com.github.livingwithhippos.nullseek.ui.theme.FilledTonalButton
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.ui.theme.OutlinedButton
import com.github.livingwithhippos.nullseek.ui.theme.SuggestionChip
import com.github.livingwithhippos.nullseek.ui.utils.FileTypeBadge
import com.github.livingwithhippos.nullseek.utils.FileTypeCategory
import com.github.livingwithhippos.nullseek.utils.detectFileTypeCategory
import com.github.livingwithhippos.nullseek.utils.isStreamableOrSubtitle
import com.github.livingwithhippos.nullseek.utils.openWebPage
import com.github.livingwithhippos.nullseek.utils.openWith
import com.github.livingwithhippos.nullseek.utils.setClipboardText
import com.github.livingwithhippos.nullseek.utils.shareText
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchStreamDetailScreen(
    result: SearchResult,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    defaultPlayer: ExternalService? = null,
    mediaPlayers: List<ExternalService> = emptyList(),
    onPlayMedia: (ExternalService, String, Boolean) -> Unit = { _, _, _ -> },
    onNavigateToExternalServices: (() -> Unit)? = null,
) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val streamUrl = result.downloadLink.orEmpty()
    val linkCopied = stringResource(R.string.search_detail_link_copied)
    var showPlayChoiceDialog by remember { mutableStateOf(false) }

    val effectiveFileName = remember(result.title, streamUrl) {
        resolveStreamFileName(result.title, streamUrl)
    }
    val fileCategory = remember(effectiveFileName) {
        detectFileTypeCategory(effectiveFileName) ?: FileTypeCategory.VIDEO
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier =
                Modifier
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
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier =
                    Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SelectionContainer {
                    Text(text = result.title, style = MaterialTheme.typography.titleLarge)
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                     itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    AssistChip(onClick = {}, label = { Text(result.indexerName) })
                    if (result.sizeBytes > 0) {
                        AssistChip(onClick = {}, label = { Text(convertToSize(result.sizeBytes)) })
                    }
                    FileTypeBadge(category = fileCategory)
                }

                if (result.seeders > 0 || result.peers > 0) {
                    Text(
                        text =
                            stringResource(
                                R.string.search_result_seeders_peers,
                                result.seeders,
                                result.peers,
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                result.publishedAt?.let { published ->
                    val absolute =
                        DateFormat
                            .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                            .format(Date(published))
                    val relative = ageText(published)
                    Text(
                        text =
                            stringResource(R.string.search_detail_published, absolute) +
                                (relative?.let { " ($it)" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                if (result.categories.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        result.categories.forEach {
                            SuggestionChip(onClick = {}, label = { Text(it) })
                        }
                    }
                }

                if (streamUrl.isNotBlank()) {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_download),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = stringResource(id = R.string.download_link_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = streamUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(8.dp),
                                )
                            }

                            val isMedia = isStreamableOrSubtitle(effectiveFileName)

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (isMedia) {
                                    Button(
                                        onClick = {
                                            if (defaultPlayer != null) {
                                                onPlayMedia(defaultPlayer, streamUrl, false)
                                            } else {
                                                showPlayChoiceDialog = true
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_play_arrow),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val playText =
                                            defaultPlayer?.let {
                                                stringResource(id = R.string.action_play_on, it.name)
                                            } ?: stringResource(id = R.string.action_play_external)
                                        Text(text = playText)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    if (isMedia) {
                                        FilledTonalButton(
                                            onClick = { openWebPage(context, streamUrl) },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_open_in_new),
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = stringResource(id = R.string.action_open_link))
                                        }
                                    } else {
                                        Button(
                                            onClick = { openWebPage(context, streamUrl) },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_open_in_new),
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = stringResource(id = R.string.action_open_link))
                                        }
                                    }

                                    val chooserTitle = stringResource(id = R.string.action_open_with)
                                    FilledTonalButton(
                                        onClick = {
                                            openWith(
                                                context = context,
                                                url = streamUrl,
                                                fileName = effectiveFileName,
                                                chooserTitle = chooserTitle,
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_apps),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = stringResource(id = R.string.action_open_with))
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            setClipboardText(context, streamUrl)
                                            Toast.makeText(context, linkCopied, Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_copy),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = stringResource(id = R.string.copy_link))
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            shareText(
                                                context = context,
                                                text = streamUrl,
                                                title = result.title.lineSequence().firstOrNull()?.trim(),
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_share),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = stringResource(id = R.string.action_share))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPlayChoiceDialog && streamUrl.isNotBlank()) {
        PlayChoiceDialog(
            services = mediaPlayers,
            onSelect = { selected, setAsDefault ->
                onPlayMedia(selected, streamUrl, setAsDefault)
                showPlayChoiceDialog = false
            },
            onDismiss = { showPlayChoiceDialog = false },
            onNavigateToSettings = onNavigateToExternalServices,
        )
    }
}

@Preview
@Composable
fun SearchStreamDetailScreenPreview() {
    NullSeekTheme {
        Surface {
            SearchStreamDetailScreen(
                result =
                    SearchResult(
                        title = "Movie.2024.2160p.WEB-DL.DDP5.1.H.265.mkv\n💿 WEB-DL | 🎞️ HEVC | ⚡ TorBox",
                        indexerName = "Stremio Addons",
                        sizeBytes = 6_200_000_000,
                        downloadLink = "https://example.com/resolve/torbox/stream.mkv",
                        isDirectStream = true,
                    ),
                onClose = {},
                defaultPlayer =
                    ExternalService(
                        id = 1L,
                        name = "Living Room Kodi",
                        type = ExternalServiceType.KODI,
                        address = "192.168.1.100:8080",
                    ),
            )
        }
    }
}

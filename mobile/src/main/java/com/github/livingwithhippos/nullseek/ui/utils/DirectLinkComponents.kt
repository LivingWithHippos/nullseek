package com.github.livingwithhippos.nullseek.ui.utils

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.github.livingwithhippos.nullseek.data.model.ExternalService
import com.github.livingwithhippos.nullseek.ui.theme.Button
import com.github.livingwithhippos.nullseek.ui.theme.OutlinedButton
import com.github.livingwithhippos.nullseek.utils.isStreamableOrSubtitle
import com.github.livingwithhippos.nullseek.utils.openWebPage
import com.github.livingwithhippos.nullseek.utils.openWith
import com.github.livingwithhippos.nullseek.utils.setClipboardText
import com.github.livingwithhippos.nullseek.utils.shareText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.ui.theme.FilledTonalButton
import com.github.livingwithhippos.nullseek.utils.FileTypeCategory

/** Tells whether a generated link is a zip or a direct file, and which kind of file. */
@Composable
fun DownloadLinkTypeRow(
    isZipped: Boolean,
    fileType: FileTypeCategory?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val (labelRes, container, content) =
                if (isZipped) {
                    Triple(
                        R.string.download_link_zipped,
                        MaterialTheme.colorScheme.tertiaryContainer,
                        MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                } else {
                    Triple(
                        R.string.download_link_direct,
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            Surface(shape = RoundedCornerShape(0.dp), color = container) {
                Text(
                    text = stringResource(id = labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = content,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            if (fileType != null) {
                FileTypeBadge(category = fileType)
            }
        }
        if (isZipped) {
            Text(
                text = stringResource(id = R.string.download_link_zipped_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Offered when downloading a whole item would zip it, but it contains a single video that could be streamed. */
@Composable
fun SingleVideoShortcut(
    fileName: String,
    sizeText: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilledTonalButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_play_arrow),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(id = R.string.download_largest_video))
        }
        Text(
            text = "${stringResource(id = R.string.selected_video)} ${fileName.substringAfterLast('/')} · $sizeText",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(id = R.string.download_mixed_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Shown instead of [SingleVideoShortcut] when an item holds several videos: points to the file list. */
@Composable
fun MultipleVideosNotice(
    count: Int,
    enabled: Boolean,
    onShowVideos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_movie),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(id = R.string.download_multiple_videos_title, count),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = stringResource(id = R.string.download_multiple_videos_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilledTonalButton(
            onClick = onShowVideos,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_manage_search),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(id = R.string.download_show_videos))
        }
    }
}

/**
 * The actions for a generated link: play on a media player, stream in the browser,
 * open, open with, copy and share.
 */
@Composable
fun DownloadLinkActions(
    url: String,
    title: String,
    defaultPlayer: ExternalService?,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    streamingPageUrl: String? = null,
    context: Context = LocalContext.current,
) {
    // Real-Debrid marks streamable links even when the title has no media extension
    val isMedia = isStreamableOrSubtitle(title) || streamingPageUrl != null

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isMedia) {
            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_play_arrow),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                val playText = defaultPlayer?.let {
                    stringResource(id = R.string.action_play_on, it.name)
                } ?: stringResource(id = R.string.action_play_external)
                Text(text = playText)
            }
        }
        // Real-Debrid can stream the file in the browser
        streamingPageUrl?.let { streamingPage ->
            OutlinedButton(
                onClick = { openWebPage(context, streamingPage) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_play_arrow),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(id = R.string.action_stream_browser))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isMedia) {
                FilledTonalButton(
                    onClick = { openWebPage(context, url) },
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
                    onClick = { openWebPage(context, url) },
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
                        url = url,
                        fileName = title,
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
                    setClipboardText(context, url)
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
                    shareText(context, url, title = title)
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

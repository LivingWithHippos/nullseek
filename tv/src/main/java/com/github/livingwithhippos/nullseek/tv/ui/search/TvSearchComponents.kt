package com.github.livingwithhippos.nullseek.tv.ui.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.core.data.stremio.ImdbTitleResult
import com.github.livingwithhippos.nullseek.core.utils.convertToSize
import com.github.livingwithhippos.nullseek.data.model.ExternalService
import com.github.livingwithhippos.nullseek.data.model.SearchResult
import com.github.livingwithhippos.nullseek.data.model.isDirectStreamResult
import com.github.livingwithhippos.nullseek.data.model.preferredLink
import com.github.livingwithhippos.nullseek.tv.ui.components.Badge
import com.github.livingwithhippos.nullseek.tv.ui.components.LocalMessenger
import com.github.livingwithhippos.nullseek.tv.ui.components.SectionCard
import com.github.livingwithhippos.nullseek.tv.ui.components.TvActionButton
import com.github.livingwithhippos.nullseek.tv.ui.components.safeOpenUrl
import com.github.livingwithhippos.nullseek.tv.ui.lists.DownloadLinkSection
import com.github.livingwithhippos.nullseek.tv.ui.lists.LinkResult
import com.github.livingwithhippos.nullseek.ui.search.PosterLoader
import com.github.livingwithhippos.nullseek.ui.search.resolveStreamFileName
import com.github.livingwithhippos.nullseek.utils.setClipboardText
import java.text.DateFormat
import java.util.Date

@Composable
fun TvPosterImage(
    posterUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
) {
    val cleanUrl = posterUrl?.trim()?.takeIf { it.isNotEmpty() }
    val bitmap by produceState<ImageBitmap?>(
        initialValue = cleanUrl?.let { PosterLoader.cached(it)?.asImageBitmap() },
        key1 = cleanUrl,
    ) {
        value = cleanUrl?.let { PosterLoader.load(it)?.asImageBitmap() }
    }
    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val current = bitmap
        if (current != null) {
            Image(bitmap = current, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        } else {
            Text(title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?", style = MaterialTheme.typography.headlineLarge)
        }
    }
}

@Composable
fun PosterCard(
    title: ImdbTitleResult,
    onClick: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        TvPosterImage(title.posterUrl, title.title, Modifier.fillMaxWidth().aspectRatio(2f / 3f))
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
            Text(
                listOfNotNull(
                    title.year,
                    stringResource(if (title.isSeries) R.string.search_imdb_type_series else R.string.search_imdb_type_movie),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun TitleDetails(title: ImdbTitleResult) {
    val context = LocalContext.current
    SectionCard {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            TvPosterImage(title.posterUrl, title.title, Modifier.width(140.dp).aspectRatio(2f / 3f))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                Text(
                    listOfNotNull(title.title, title.year?.let { "($it)" }).joinToString(" "),
                    style = MaterialTheme.typography.titleLarge,
                )
                title.imdbRating?.let { Text(stringResource(R.string.search_stremio_imdb_rating, it)) }
                if (title.genres.isNotEmpty()) Text(title.genres.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
                title.description?.let { Text(it, maxLines = 4, overflow = TextOverflow.Ellipsis) }
                if (title.director.isNotEmpty()) Text(stringResource(R.string.search_stremio_director, title.director.joinToString()))
                title.stars?.let { Text(stringResource(R.string.search_stremio_cast, it), maxLines = 2) }
                TvActionButton(
                    stringResource(R.string.search_stremio_open_imdb),
                    { safeOpenUrl(context, "https://www.imdb.com/title/${title.imdbId}/") },
                    icon = R.drawable.ic_open_in_new,
                )
            }
        }
    }
}

/** Right pane of the search screen: torrent/magnet result or a direct Stremio stream. */
@Composable
fun TvSearchResultDetail(
    result: SearchResult,
    canAddDownloads: Boolean,
    downloadInProgress: Boolean,
    onDownload: (String) -> Unit,
    defaultPlayer: ExternalService?,
    mediaPlayers: List<ExternalService>,
    onPlayMedia: (ExternalService, String, Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val messenger = LocalMessenger.current
    val copied = stringResource(R.string.search_detail_link_copied)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(result.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f), maxLines = 4)
            TvActionButton(stringResource(R.string.search_detail_back), onClose, icon = R.drawable.ic_close)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Badge(result.indexerName)
            if (result.sizeBytes > 0) Badge(convertToSize(result.sizeBytes), color = MaterialTheme.colorScheme.secondary)
            if (!result.isFromStremioAddon) {
                Badge(stringResource(R.string.search_result_seeders_peers, result.seeders, result.peers), color = MaterialTheme.colorScheme.tertiary)
            }
            result.categories.forEach { Badge(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        result.publishedAt?.let {
            Text(stringResource(R.string.search_detail_published, DateFormat.getDateInstance().format(Date(it))))
        }

        if (result.isDirectStreamResult()) {
            val url = result.downloadLink.orEmpty()
            val fileName = remember(result.title, url) { resolveStreamFileName(result.title, url) }
            DownloadLinkSection(
                result = LinkResult(title = fileName, url = url),
                defaultPlayer = defaultPlayer,
                mediaPlayers = mediaPlayers,
                onPlayMedia = onPlayMedia,
                onDismiss = null,
                isMedia = true,
            )
        } else {
            val link = result.preferredLink()
            SectionCard(title = stringResource(R.string.search_detail_link), icon = R.drawable.ic_magnet) {
                if (link != null) {
                    Text(link, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (link != null && canAddDownloads) {
                        TvActionButton(
                            text = stringResource(R.string.search_detail_download),
                            icon = R.drawable.ic_download,
                            primary = true,
                            loading = downloadInProgress,
                            onClick = { onDownload(link) },
                        )
                    }
                    if (link != null) {
                        TvActionButton(
                            text = stringResource(R.string.search_detail_copy_link),
                            icon = R.drawable.ic_copy,
                            onClick = {
                                setClipboardText(context, link)
                                messenger.show(copied)
                            },
                        )
                    }
                    result.infoUrl?.takeIf { it.isNotBlank() }?.let { info ->
                        TvActionButton(
                            text = stringResource(R.string.search_detail_open_info),
                            icon = R.drawable.ic_open_in_new,
                            onClick = { safeOpenUrl(context, info) },
                        )
                    }
                }
            }
        }
    }
}

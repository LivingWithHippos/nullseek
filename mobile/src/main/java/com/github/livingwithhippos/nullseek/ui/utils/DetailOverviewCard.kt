package com.github.livingwithhippos.nullseek.ui.utils

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.core.utils.convertToSize
import com.github.livingwithhippos.nullseek.core.utils.formatToLocalDate
import com.github.livingwithhippos.nullseek.utils.setClipboardText
import com.github.livingwithhippos.nullseek.utils.shareText

/** Everything the overview card shows; each details screen maps its DTO into this. */
data class OverviewInfo(
    @StringRes val titleRes: Int,
    val name: String,
    val sizeBytes: Long,
    val downloadState: String,
    val active: Boolean,
    val cached: Boolean,
    val createdAt: String?,
    val expiresAt: String?,
    // shown in an error box under the name
    val error: String? = null,
    // shown as a visible row with a copy button
    val hash: String? = null,
    // torrent magnet, or web/usenet source URL
    val link: String? = null,
    @StringRes val linkLabelRes: Int = R.string.source_link_title,
    @StringRes val copyLinkDescriptionRes: Int = R.string.copy_source_link,
    // false for magnets, which are long and unreadable
    val showLinkText: Boolean = true,
)

/** Overview of a torrent, web or usenet download. Secondary rows start collapsed. */
@Composable
fun DetailOverviewCard(
    info: OverviewInfo,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(id = info.titleRes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = info.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            if (!info.error.isNullOrBlank()) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        ),
                ) {
                    Text(
                        text = info.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            if (!info.hash.isNullOrBlank()) {
                OverviewDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.hash),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        Text(
                            text = info.hash,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        CopyButton(
                            text = info.hash,
                            description = "Copy Hash",
                            context = context,
                        )
                    }
                }
            }

            OverviewDivider()
            OverviewRow(label = stringResource(id = R.string.size), value = convertToSize(info.sizeBytes))

            OverviewDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.status),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = info.downloadState.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            OverviewDivider()
            OverviewRow(
                label = stringResource(R.string.state),
                value =
                    buildString {
                        append(if (info.active) stringResource(R.string.active) else stringResource(R.string.inactive))
                        if (info.cached) append(" • " + stringResource(R.string.cached))
                    },
            )

            OverviewDivider()
            CollapsibleDetails {
                OverviewRow(label = stringResource(id = R.string.added_on), value = formatToLocalDate(info.createdAt))

                if (info.expiresAt != null) {
                    OverviewDivider()
                    OverviewRow(label = stringResource(R.string.expires), value = formatToLocalDate(info.expiresAt))
                }

                val link = info.link
                if (!link.isNullOrBlank()) {
                    OverviewDivider()
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(id = info.linkLabelRes),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Row {
                                IconButton(
                                    onClick = { setClipboardText(context, link) },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_copy),
                                        contentDescription = stringResource(id = info.copyLinkDescriptionRes),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { shareText(context, link, info.name) },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_share),
                                        contentDescription = stringResource(id = R.string.action_share),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                        if (info.showLinkText) {
                            Text(
                                text = link,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A "More details" toggle that hides its content until tapped. */
@Composable
fun CollapsibleDetails(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(if (expanded) R.string.overview_less_details else R.string.overview_more_details),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                painter = painterResource(id = if (expanded) R.drawable.ic_arrow_drop_up else R.drawable.ic_arrow_drop_down),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun OverviewDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun OverviewRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

package com.github.livingwithhippos.nullseek.ui.user

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.core.net.toUri
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.core.utils.convertToDays
import com.github.livingwithhippos.nullseek.core.utils.convertToSize
import com.github.livingwithhippos.nullseek.core.utils.formatBandwidthMonth
import com.github.livingwithhippos.nullseek.core.utils.formatToLocalDate
import com.github.livingwithhippos.nullseek.core.utils.getPlanName
import com.github.livingwithhippos.nullseek.core.utils.isDateNotInPast
import com.github.livingwithhippos.nullseek.core.utils.isFreePlan
import com.github.livingwithhippos.nullseek.core.utils.mockUser
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.ui.theme.OutlinedButton
import com.github.livingwithhippos.nullseek.ui.theme.TextButton
import com.github.livingwithhippos.nullseek.utils.setClipboardText
import com.github.livingwithhippos.torbox.model.UserDataDto
import java.util.Locale
import logcat.asLog
import logcat.logcat
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UserScreen(
    modifier: Modifier = Modifier,
    user: UserDataDto?,
    loading: Boolean,
    error: String?,
    context: Context = LocalContext.current,
    viewModel: UserViewModel = koinViewModel(),
    enabled: Boolean = true,
) {
    if (loading || user == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            LoadingIndicator()
        }
        return
    }

    val referralPreference by viewModel.includeReferralPreference.collectAsState()
    val detailedStats by viewModel.detailedStats.collectAsState()
    var showReferralDialog by remember { mutableStateOf(false) }

    if (showReferralDialog) {
        AlertDialog(
            onDismissRequest = { showReferralDialog = false },
            title = {
                Text(text = stringResource(id = R.string.referral_dialog_title))
            },
            text = {
                Text(text = stringResource(id = R.string.referral_dialog_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReferralDialog = false
                        viewModel.setReferralPreference(true)
                        openBrowser(context, viewModel.buildTorboxSubscriptionUrl(true))
                    },
                ) {
                    Text(text = stringResource(id = R.string.referral_dialog_positive))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showReferralDialog = false
                        viewModel.setReferralPreference(false)
                        openBrowser(context, viewModel.buildTorboxSubscriptionUrl(false))
                    },
                ) {
                    Text(text = stringResource(id = R.string.referral_dialog_negative))
                }
            },
        )
    }

    UserScreenContent(
        modifier = modifier,
        user = user,
        error = error,
        detailedStats = detailedStats,
        onLoadDetailedStats = viewModel::loadDetailedStats,
        onOpenSettingsClick = {
            openBrowser(context, viewModel.getTorboxSettingsUrl())
        },
        onOpenSubscriptionClick = {
            if (referralPreference == null) {
                showReferralDialog = true
            } else {
                val url = viewModel.buildTorboxSubscriptionUrl(referralPreference == true)
                openBrowser(context, url)
            }
        },
        context = context,
        enabled = enabled,
    )
}

@Composable
fun UserScreenContent(
    modifier: Modifier = Modifier,
    user: UserDataDto,
    error: String?,
    onOpenSettingsClick: () -> Unit,
    onOpenSubscriptionClick: () -> Unit,
    context: Context = LocalContext.current,
    enabled: Boolean = true,
    detailedStats: DetailedStatsState = DetailedStatsState.Idle,
    onLoadDetailedStats: () -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AccountOverviewAndActionsCard(
                user = user,
                onOpenSettingsClick = onOpenSettingsClick,
                onOpenSubscriptionClick = onOpenSubscriptionClick,
                context = context,
                enabled = enabled,
            )

            UserStatsCard(user = user, detailedStats = detailedStats, onLoadDetailedStats = onLoadDetailedStats)

            AccountFeaturesCard(user = user)

            // Error Notice (if any)
            if (error != null) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        ),
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            // Extra bottom spacing to account for the FAB
            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}

@Composable
private fun AccountOverviewAndActionsCard(
    user: UserDataDto,
    onOpenSettingsClick: () -> Unit,
    onOpenSubscriptionClick: () -> Unit,
    context: Context,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(id = R.string.account_overview),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.user),
                    modifier = Modifier.padding(end = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.user_plan),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = getPlanName(user.plan).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.user_premium_expires),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = convertToDays(user.premiumExpiresAt),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.user_subscription),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = if (user.isSubscribed) stringResource(R.string.active) else stringResource(R.string.none),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color =
                        if (user.isSubscribed) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Web Action Buttons
            OutlinedButton(
                onClick = onOpenSettingsClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_link),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(id = R.string.open_torbox_web))
            }

            OutlinedButton(
                onClick = onOpenSubscriptionClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_contact_page),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(id = R.string.open_torbox_subscription))
            }


            OutlinedButton(
                onClick = {
                    setClipboardText(context, user.userReferral)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_copy),
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(id = R.string.copy_referral))
            }

            if (user.purchasesReferred > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.purchases_referred),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = user.purchasesReferred.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountFeaturesCard(
    user: UserDataDto,
    modifier: Modifier = Modifier,
) {
    val coolDown = user.cooldownUntil

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(id = R.string.account_features),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.concurrent_slots),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "+${user.additionalConcurrentSlots}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.long_term_seeding),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = if (user.longTermSeeding) stringResource(R.string.active) else stringResource(R.string.inactive),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color =
                        if (user.longTermSeeding) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.long_term_storage),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = if (user.longTermStorage) stringResource(R.string.active) else stringResource(R.string.inactive),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color =
                        if (user.longTermStorage) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }

            if (user.isVendor) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.vendor_badge),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.enabled),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            if (isFreePlan(user.plan) && coolDown != null && isDateNotInPast(coolDown)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.cooldown_active),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = formatToLocalDate(coolDown),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}


@Composable
private fun UserStatsCard(
    user: UserDataDto,
    detailedStats: DetailedStatsState,
    onLoadDetailedStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(id = R.string.stats_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(id = R.string.stats_since, formatToLocalDate(user.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            StatsRow(
                label = stringResource(id = R.string.stats_data_downloaded),
                value = convertToSize(user.totalBytesDownloaded),
                hint = stringResource(id = R.string.stats_data_downloaded_hint),
                highlight = true,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatsRow(
                label = stringResource(id = R.string.stats_data_uploaded),
                value = convertToSize(user.totalBytesUploaded),
                hint = stringResource(id = R.string.stats_data_uploaded_hint),
                highlight = true,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = stringResource(id = R.string.stats_items_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            // the per-type counts are indented under the total so they read as a breakdown
            StatsRow(label = stringResource(id = R.string.stats_items_total), value = user.totalDownloaded.toString())
            Column(
                modifier = Modifier.padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatsRow(label = stringResource(id = R.string.torrents), value = user.torrentsDownloaded.toString(), small = true)
                StatsRow(label = stringResource(id = R.string.web_downloads), value = user.webDownloadsDownloaded.toString(), small = true)
                StatsRow(label = stringResource(id = R.string.usenet_downloads), value = user.usenetDownloadsDownloaded.toString(), small = true)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            DetailedStatsSection(state = detailedStats, onLoad = onLoadDetailedStats)
        }
    }
}

/** Ratio and the last 30 days of bandwidth from user/stats, loaded when the user asks for it. */
@Composable
private fun DetailedStatsSection(
    state: DetailedStatsState,
    onLoad: () -> Unit,
) {
    when (state) {
        DetailedStatsState.Idle -> {
            OutlinedButton(onClick = onLoad, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(id = R.string.stats_show_detailed))
            }
        }

        DetailedStatsState.Loading -> {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }

        is DetailedStatsState.Error -> {
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            OutlinedButton(onClick = onLoad, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(id = R.string.retry))
            }
        }

        is DetailedStatsState.Loaded -> {
            state.stats.general?.let { general ->
                StatsRow(
                    label = stringResource(id = R.string.stats_ratio),
                    value = String.format(Locale.getDefault(), "%.2f", general.ratio),
                )
            }
            Text(
                text = stringResource(id = R.string.stats_bandwidth_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            val bandwidth = state.stats.bandwidth.orEmpty()
            if (bandwidth.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.stats_bandwidth_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                bandwidth.forEach { period ->
                    StatsRow(label = formatBandwidthMonth(period.date), value = convertToSize(period.bytesDownloaded), small = true)
                }
            }
        }
    }
}

@Composable
private fun StatsRow(
    label: String,
    value: String,
    hint: String? = null,
    highlight: Boolean = false,
    small: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = if (small) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            )
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = value,
            style = if (small) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun openBrowser(
    context: Context,
    url: String,
) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    } catch (e: Exception) {
        logcat("UserScreen") { "Failed to open browser: ${e.asLog()}" }
    }
}

@Preview
@Composable
fun UserScreenPreview() {
    NullSeekTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        ) { innerPadding ->
            UserScreenContent(
                modifier = Modifier.padding(innerPadding),
                user = mockUser,
                error = null,
                onOpenSettingsClick = {},
                onOpenSubscriptionClick = {},
            )
        }
    }
}
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
import com.github.livingwithhippos.nullseek.core.utils.formatToLocalDate
import com.github.livingwithhippos.nullseek.core.utils.getPlanName
import com.github.livingwithhippos.nullseek.core.utils.isDateNotInPast
import com.github.livingwithhippos.nullseek.core.utils.mockUser
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.ui.theme.OutlinedButton
import com.github.livingwithhippos.nullseek.ui.theme.TextButton
import com.github.livingwithhippos.nullseek.utils.setClipboardText
import com.github.livingwithhippos.torbox.model.UserDataDto
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

            UserStatsCard(user = user)

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
                    painter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(id = R.string.open_torbox_settings))
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

            if (coolDown != null && isDateNotInPast(coolDown)) {
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
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(id = R.string.usage_stats_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.data_downloaded),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = convertToSize(user.totalBytesDownloaded),
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
                    text = stringResource(id = R.string.data_uploaded),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = convertToSize(user.totalBytesUploaded),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.total_files),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = user.totalDownloaded.toString(),
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
                    text = stringResource(id = R.string.torrents),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = user.torrentsDownloaded.toString(),
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
                    text = stringResource(id = R.string.web_downloads),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = user.webDownloadsDownloaded.toString(),
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
                    text = stringResource(id = R.string.usenet_downloads),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = user.usenetDownloadsDownloaded.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
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
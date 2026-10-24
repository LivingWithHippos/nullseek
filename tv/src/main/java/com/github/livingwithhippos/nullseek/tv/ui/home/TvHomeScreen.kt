package com.github.livingwithhippos.nullseek.tv.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.livingwithhippos.nullseek.NullRootUiState
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.core.data.NetProvider
import com.github.livingwithhippos.nullseek.core.utils.convertToDays
import com.github.livingwithhippos.nullseek.core.utils.convertToSize
import com.github.livingwithhippos.nullseek.core.utils.formatToLocalDate
import com.github.livingwithhippos.nullseek.core.utils.getPlanName
import com.github.livingwithhippos.nullseek.core.utils.isDateNotInPast
import com.github.livingwithhippos.nullseek.tv.ui.components.Badge
import com.github.livingwithhippos.nullseek.tv.ui.components.ErrorBanner
import com.github.livingwithhippos.nullseek.tv.ui.components.InfoRow
import com.github.livingwithhippos.nullseek.tv.ui.components.LoadingBox
import com.github.livingwithhippos.nullseek.tv.ui.components.LocalMessenger
import com.github.livingwithhippos.nullseek.tv.ui.components.ScreenHeader
import com.github.livingwithhippos.nullseek.tv.ui.components.SectionCard
import com.github.livingwithhippos.nullseek.tv.ui.components.TvActionButton
import com.github.livingwithhippos.nullseek.tv.ui.components.TvDialog
import com.github.livingwithhippos.nullseek.tv.ui.components.safeOpenUrl
import com.github.livingwithhippos.nullseek.tv.ui.provider.TvProviderSetupScreen
import com.github.livingwithhippos.nullseek.ui.user.UserViewModel
import com.github.livingwithhippos.nullseek.utils.setClipboardText
import com.github.livingwithhippos.torbox.model.UserDataDto
import org.koin.androidx.compose.koinViewModel

@Composable
fun TvHomeScreen(
    uiState: NullRootUiState,
    checkValidToken: (NetProvider, String) -> Unit,
    onBackupRestored: () -> Unit,
    onOpenLists: () -> Unit,
    onOpenSearch: () -> Unit,
    onNewDownload: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val user = uiState.user
    when {
        uiState.setup ->
            TvProviderSetupScreen(
                modifier = modifier,
                uiState = uiState,
                checkValidToken = checkValidToken,
                onBackupRestored = onBackupRestored,
            )

        user != null ->
            UserOverview(
                modifier = modifier,
                user = user,
                onOpenLists = onOpenLists,
                onOpenSearch = onOpenSearch,
                onNewDownload = onNewDownload,
                onOpenSettings = onOpenSettings,
            )

        uiState.loading -> LoadingBox(modifier)

        else -> uiState.error?.let { ErrorBanner(it, modifier) } ?: LoadingBox(modifier)
    }
}

@Composable
private fun UserOverview(
    user: UserDataDto,
    onOpenLists: () -> Unit,
    onOpenSearch: () -> Unit,
    onNewDownload: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UserViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val messenger = LocalMessenger.current
    val referralPreference by viewModel.includeReferralPreference.collectAsStateWithLifecycle()
    var showReferralDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader(title = stringResource(R.string.account_overview), subtitle = user.email)

        // quick actions first: they are the first thing focused when the app opens
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TvActionButton(stringResource(R.string.lists), onOpenLists, icon = R.drawable.ic_list_bullet, primary = true)
            TvActionButton(stringResource(R.string.search), onOpenSearch, icon = R.drawable.ic_manage_search)
            TvActionButton(stringResource(R.string.new_download), onNewDownload, icon = R.drawable.ic_add)
            TvActionButton(stringResource(R.string.settings), onOpenSettings, icon = R.drawable.ic_settings)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                SectionCard(title = getPlanName(user.plan), icon = R.drawable.ic_contact_page) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (user.isSubscribed) {
                            Badge(stringResource(R.string.active_subscription))
                        } else {
                            Badge(stringResource(R.string.no_subscription), color = MaterialTheme.colorScheme.error)
                        }
                        if (user.isVendor) Badge(stringResource(R.string.vendor_badge), color = MaterialTheme.colorScheme.tertiary)
                    }
                    InfoRow("Expires", convertToDays(user.premiumExpiresAt))
                    InfoRow(stringResource(R.string.added_on), formatToLocalDate(user.createdAt))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TvActionButton(
                            text = stringResource(R.string.open_torbox_subscription),
                            icon = R.drawable.ic_open_in_new,
                            onClick = {
                                val pref = referralPreference
                                if (pref == null) {
                                    showReferralDialog = true
                                } else {
                                    safeOpenUrl(context, viewModel.buildTorboxSubscriptionUrl(pref))
                                }
                            },
                        )
                        TvActionButton(
                            text = stringResource(R.string.open_torbox_settings),
                            icon = R.drawable.ic_settings,
                            onClick = { safeOpenUrl(context, viewModel.getTorboxSettingsUrl()) },
                        )
                    }
                }

                SectionCard(title = stringResource(R.string.account_features), icon = R.drawable.ic_tune) {
                    InfoRow(stringResource(R.string.concurrent_slots), user.additionalConcurrentSlots.toString())
                    InfoRow(stringResource(R.string.long_term_seeding), yesNo(user.longTermSeeding))
                    InfoRow(stringResource(R.string.long_term_storage), yesNo(user.longTermStorage))
                    val cooldown = user.cooldownUntil
                    if (isDateNotInPast(cooldown)) {
                        InfoRow(stringResource(R.string.cooldown_active), formatToLocalDate(cooldown))
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                SectionCard(title = stringResource(R.string.usage_stats_title), icon = R.drawable.ic_download) {
                    InfoRow(stringResource(R.string.data_downloaded), convertToSize(user.totalBytesDownloaded))
                    InfoRow(stringResource(R.string.data_uploaded), convertToSize(user.totalBytesUploaded))
                    InfoRow(stringResource(R.string.total_files), user.totalDownloaded.toString())
                    InfoRow(stringResource(R.string.torrents), user.torrentsDownloaded.toString())
                    InfoRow(stringResource(R.string.web_downloads), user.webDownloadsDownloaded.toString())
                    InfoRow(stringResource(R.string.usenet_downloads), user.usenetDownloadsDownloaded.toString())
                }

                SectionCard(title = stringResource(R.string.referral_section_title), icon = R.drawable.ic_share) {
                    InfoRow(stringResource(R.string.referral_code), user.userReferral)
                    InfoRow(stringResource(R.string.purchases_referred), user.purchasesReferred.toString())
                    Text(
                        text =
                            when (referralPreference) {
                                true -> stringResource(R.string.referral_support_active)
                                else -> stringResource(R.string.referral_support_inactive)
                            },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val copied = stringResource(R.string.copy_referral)
                        TvActionButton(
                            text = stringResource(R.string.copy_referral),
                            icon = R.drawable.ic_copy,
                            onClick = {
                                setClipboardText(context, user.userReferral)
                                messenger.show(copied)
                            },
                        )
                        if (referralPreference != null) {
                            TvActionButton(
                                text = stringResource(R.string.change_referral_choice),
                                icon = R.drawable.ic_edit,
                                onClick = viewModel::resetReferralPreference,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showReferralDialog) {
        TvDialog(
            title = stringResource(R.string.referral_dialog_title),
            onDismiss = { showReferralDialog = false },
            confirmText = stringResource(R.string.referral_dialog_positive),
            onConfirm = {
                showReferralDialog = false
                viewModel.setReferralPreference(true)
                safeOpenUrl(context, viewModel.buildTorboxSubscriptionUrl(true))
            },
            dismissText = stringResource(R.string.referral_dialog_negative),
            onDismissClick = {
                showReferralDialog = false
                viewModel.setReferralPreference(false)
                safeOpenUrl(context, viewModel.buildTorboxSubscriptionUrl(false))
            },
        ) {
            androidx.compose.material3.Text(stringResource(R.string.referral_dialog_message))
        }
    }
}

private fun yesNo(value: Boolean) = if (value) "✓" else "—"

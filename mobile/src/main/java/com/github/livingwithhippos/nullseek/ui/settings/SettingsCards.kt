package com.github.livingwithhippos.nullseek.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.github.livingwithhippos.nullseek.BuildConfig
import com.github.livingwithhippos.nullseek.billing.EntitlementState
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.core.utils.AppLanguage
import com.github.livingwithhippos.nullseek.core.utils.PRIVACY_POLICY_URL
import com.github.livingwithhippos.nullseek.core.utils.REPOSITORY_URL
import com.github.livingwithhippos.nullseek.data.repository.DownloadZipMode
import com.github.livingwithhippos.nullseek.data.repository.PAGING_LOAD_SIZE_PRESETS
import com.github.livingwithhippos.nullseek.data.repository.SeedingOption
import com.github.livingwithhippos.nullseek.data.repository.ThemeColor
import com.github.livingwithhippos.nullseek.data.repository.ThemeMode
import com.github.livingwithhippos.nullseek.ui.newdownload.labelRes
import com.github.livingwithhippos.nullseek.ui.theme.Button
import com.github.livingwithhippos.nullseek.ui.theme.FilterChip
import com.github.livingwithhippos.nullseek.ui.theme.OutlinedButton
import com.github.livingwithhippos.nullseek.ui.theme.TextButton
import com.github.livingwithhippos.nullseek.ui.theme.shouldUseDarkTheme
import com.github.livingwithhippos.nullseek.ui.theme.swatchColor

/** A clickable card that opens another screen. */
@Composable
internal fun NavigationSettingCard(
    @StringRes title: Int,
    @StringRes summary: Int,
    onClick: () -> Unit,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(id = title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(id = summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_open_in_new),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Dark mode, theme color and standard font choices. */
@Composable
internal fun ThemeSettingsCard(
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    themeColor: ThemeColor,
    onThemeColorSelected: (ThemeColor) -> Unit,
    useStandardFont: Boolean,
    onUseStandardFontChanged: (Boolean) -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        val resolvedDarkTheme = shouldUseDarkTheme(themeMode)
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(id = R.string.theme_preference_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(id = R.string.theme_preference_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = stringResource(id = R.string.theme_dark_mode_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { onThemeModeSelected(mode) },
                        label = { Text(text = stringResource(id = mode.labelRes())) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = stringResource(id = R.string.theme_color_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ThemeColor.entries.forEach { colorOption ->
                    val swatch = colorOption.swatchColor(resolvedDarkTheme)
                    FilterChip(
                        selected = themeColor == colorOption,
                        onClick = { onThemeColorSelected(colorOption) },
                        leadingIcon =
                            if (swatch != null) {
                                {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(12.dp)
                                                .background(swatch)
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.outline,
                                                ),
                                    )
                                }
                            } else {
                                null
                            },
                        label = { Text(text = stringResource(id = colorOption.labelRes())) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.theme_standard_font_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(id = R.string.theme_standard_font_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = useStandardFont,
                    onCheckedChange = onUseStandardFontChanged,
                )
            }
        }
    }
}

/** A card with a title, a summary and a dropdown under them. */
@Composable
private fun DropdownSettingsCard(
    @StringRes title: Int,
    @StringRes summary: Int,
    dropdown: @Composable () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(id = title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(id = summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            dropdown()
        }
    }
}

@Composable
internal fun DownloadFormatSettingsCard(
    mode: DownloadZipMode,
    onModeSelected: (DownloadZipMode) -> Unit,
) {
    DropdownSettingsCard(R.string.download_zip_preference_title, R.string.download_zip_preference_summary) {
        DownloadZipModeDropdown(selected = mode, onSelected = onModeSelected)
    }
}

@Composable
internal fun DefaultSeedingSettingsCard(
    option: SeedingOption,
    onOptionSelected: (SeedingOption) -> Unit,
) {
    DropdownSettingsCard(R.string.default_seeding_title, R.string.default_seeding_summary) {
        SeedingOptionDropdown(selected = option, onSelected = onOptionSelected)
    }
}

/** A card with a title and summary on the left and a switch on the right. */
@Composable
internal fun SwitchSettingCard(
    @StringRes title: Int,
    @StringRes summary: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(id = title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(id = summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

/** Page size for the download lists: preset chips plus a free number field. */
@Composable
internal fun PagingLoadSizeSettingsCard(
    pagingLoadSize: Int,
    onPagingLoadSizeSelected: (Int) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var pageSizeInput by rememberSaveable { mutableStateOf(pagingLoadSize.toString()) }

    LaunchedEffect(pagingLoadSize) {
        pageSizeInput = pagingLoadSize.toString()
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(id = R.string.paging_load_size_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(id = R.string.paging_load_size_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PAGING_LOAD_SIZE_PRESETS.forEach { preset ->
                    FilterChip(
                        selected = pagingLoadSize == preset,
                        onClick = {
                            pageSizeInput = preset.toString()
                            onPagingLoadSizeSelected(preset)
                        },
                        label = { Text(text = preset.toString()) },
                    )
                }
            }

            OutlinedTextField(
                value = pageSizeInput,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { it.isDigit() }
                    pageSizeInput = filtered
                    filtered.toIntOrNull()?.takeIf { it > 0 }?.let { validSize ->
                        onPagingLoadSizeSelected(validSize)
                    }
                },
                label = { Text(text = stringResource(id = R.string.paging_load_size_label)) },
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onDone = {
                            val parsed = pageSizeInput.toIntOrNull()?.takeIf { it > 0 }
                            if (parsed != null) {
                                onPagingLoadSizeSelected(parsed)
                            } else {
                                pageSizeInput = pagingLoadSize.toString()
                            }
                            focusManager.clearFocus()
                        },
                    ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Create or restore a backup; the restore confirmation dialog is shown by the screen. */
@Composable
internal fun BackupSettingsCard(
    isBusy: Boolean,
    onCreateBackup: () -> Unit,
    onRestoreClick: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(id = R.string.backup_preference_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(id = R.string.backup_preference_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onCreateBackup,
                    enabled = !isBusy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(id = R.string.backup_create_button))
                }

                OutlinedButton(
                    onClick = onRestoreClick,
                    enabled = !isBusy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(id = R.string.backup_restore_button))
                }
            }
        }
    }
}

/** App version, an invitation to contribute and the privacy policy link. */
@Composable
internal fun AboutSettingsCard() {
    val uriHandler = LocalUriHandler.current
    // openUri throws when no app can handle the link
    val openLink: (String) -> Unit = { url -> runCatching { uriHandler.openUri(url) } }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_info),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(id = R.string.about_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = stringResource(id = R.string.about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(id = R.string.about_contribute),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedButton(
                onClick = { openLink(REPOSITORY_URL) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = R.string.about_open_repository))
                Spacer(modifier = Modifier.size(8.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_open_in_new),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            TextButton(
                onClick = { openLink(PRIVACY_POLICY_URL) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = R.string.about_privacy_policy))
            }
        }
    }
}

@Composable
internal fun DonationSettingsCard() {
    val uriHandler = LocalUriHandler.current
    // openUri throws when no app can handle the link
    val openLink: (String) -> Unit = { url -> runCatching { uriHandler.openUri(url) } }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_favorite),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(id = R.string.donate_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = stringResource(id = R.string.donate_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(id = R.string.donate_github_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { openLink(BuildConfig.DONATE_KOFI_URL) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(id = R.string.donate_kofi_button))
                    Spacer(modifier = Modifier.size(8.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_open_in_new),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }

                OutlinedButton(
                    onClick = { openLink(BuildConfig.DONATE_GITHUB_URL) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(id = R.string.donate_github_button))
                    Spacer(modifier = Modifier.size(8.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_open_in_new),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun WidgetsPremiumSettingsCard(
    entitlementState: EntitlementState,
    onSubscribeClick: () -> Unit,
    onRestorePurchasesClick: () -> Unit,
    onPinDownloadsWidgetClick: () -> Unit,
    onPinQuickActionsWidgetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summaryText =
        when {
            entitlementState.isFossBuild -> stringResource(id = R.string.widgets_foss_summary)
            entitlementState.isPremiumUnlocked -> stringResource(id = R.string.widgets_playstore_unlocked_summary)
            else -> stringResource(id = R.string.widgets_playstore_locked_summary)
        }

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(id = R.string.widgets_premium_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (entitlementState.errorMessage != null) {
                Text(
                    text = entitlementState.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (entitlementState.isPremiumUnlocked) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onPinDownloadsWidgetClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = stringResource(id = R.string.widgets_pin_downloads))
                    }

                    OutlinedButton(
                        onClick = onPinQuickActionsWidgetClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = stringResource(id = R.string.widgets_pin_quick_actions))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onSubscribeClick,
                        enabled = !entitlementState.isLoading,
                        modifier = Modifier.weight(1f),
                    ) {
                        val buttonText =
                            if (!entitlementState.formattedPrice.isNullOrBlank()) {
                                stringResource(
                                    id = R.string.widgets_subscribe_with_price,
                                    entitlementState.formattedPrice,
                                )
                            } else {
                                stringResource(id = R.string.widgets_subscribe_button)
                            }
                        Text(text = buttonText)
                    }

                    OutlinedButton(
                        onClick = onRestorePurchasesClick,
                        enabled = !entitlementState.isLoading,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = stringResource(id = R.string.widgets_restore_purchases))
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadZipModeDropdown(
    selected: DownloadZipMode,
    onSelected: (DownloadZipMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = stringResource(id = selected.labelRes()),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DOWNLOAD_ZIP_MODE_ORDER.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = mode.labelRes())) },
                    onClick = {
                        expanded = false
                        onSelected(mode)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

/** Follow system, English or Italian; choosing one recreates the screen in that language. */
@Composable
internal fun LanguageSettingsCard() {
    var language by remember { mutableStateOf(AppLanguage.current()) }
    var expanded by remember { mutableStateOf(false) }
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(id = R.string.language_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(id = R.string.language_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = stringResource(id = language.labelRes),
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    AppLanguage.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(text = stringResource(id = option.labelRes)) },
                            onClick = {
                                expanded = false
                                if (option != language) {
                                    language = option
                                    option.apply()
                                }
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeedingOptionDropdown(
    selected: SeedingOption,
    onSelected: (SeedingOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = stringResource(id = selected.labelRes()),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            SeedingOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = option.labelRes())) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

private val DOWNLOAD_ZIP_MODE_ORDER =
    listOf(
        DownloadZipMode.ASK,
        DownloadZipMode.ZIP,
        DownloadZipMode.ZIP_NON_MEDIA,
        DownloadZipMode.UNCOMPRESSED,
    )

private fun DownloadZipMode.labelRes(): Int =
    when (this) {
        DownloadZipMode.ASK -> R.string.download_zip_preference_ask
        DownloadZipMode.ZIP -> R.string.download_zip_preference_zip
        DownloadZipMode.ZIP_NON_MEDIA -> R.string.download_zip_preference_zip_non_media
        DownloadZipMode.UNCOMPRESSED -> R.string.download_zip_preference_uncompressed
    }

private fun ThemeMode.labelRes(): Int =
    when (this) {
        ThemeMode.SYSTEM -> R.string.theme_mode_system
        ThemeMode.LIGHT -> R.string.theme_mode_light
        ThemeMode.DARK -> R.string.theme_mode_dark
        ThemeMode.AMOLED -> R.string.theme_mode_amoled
    }

private fun ThemeColor.labelRes(): Int =
    when (this) {
        ThemeColor.SYSTEM -> R.string.theme_color_system
        ThemeColor.PURPLE -> R.string.theme_color_purple
        ThemeColor.BLUE -> R.string.theme_color_blue
        ThemeColor.TEAL -> R.string.theme_color_teal
        ThemeColor.GREEN -> R.string.theme_color_green
        ThemeColor.ORANGE -> R.string.theme_color_orange
        ThemeColor.RED -> R.string.theme_color_red
        ThemeColor.TORBOX -> R.string.theme_color_torbox
    }


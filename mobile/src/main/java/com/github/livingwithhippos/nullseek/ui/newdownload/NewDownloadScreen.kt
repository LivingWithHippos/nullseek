package com.github.livingwithhippos.nullseek.ui.newdownload

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.ui.theme.Button
import com.github.livingwithhippos.nullseek.ui.theme.TextButton
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun NewDownloadScreen(
    contentType: NullContentType,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    initialLinks: String = "",
    initialFiles: List<Uri> = emptyList(),
    viewModel: NewDownloadViewModel = koinViewModel()
) {
    var linkText by rememberSaveable(initialLinks) { mutableStateOf(initialLinks) }
    var password by rememberSaveable { mutableStateOf("") }

    var advancedExpanded by rememberSaveable { mutableStateOf(false) }

    var selectedFiles by remember(initialFiles) { mutableStateOf(initialFiles) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedFiles = uris
        }
    }

    val validLinksCount = remember(linkText) {
        if (linkText.isBlank()) 0
        else linkText.split("\\s+".toRegex()).count { isValidDownloadLink(it) }
    }
    val totalDownloads = validLinksCount + selectedFiles.size

    val seedingOptions = listOf(
        R.string.seeding_auto,
        R.string.seeding_seed,
        R.string.seeding_dont_seed
    )
    var selectedSeedingOption by rememberSaveable { mutableStateOf(seedingOptions[0]) }

    val usenetOptions = listOf(
        R.string.usenet_default,
        R.string.usenet_none,
        R.string.usenet_repair,
        R.string.usenet_repair_unpack,
        R.string.usenet_repair_unpack_delete
    )
    var selectedUsenetOption by rememberSaveable { mutableStateOf(usenetOptions[0]) }

    var allowZipped by rememberSaveable { mutableStateOf(true) }

    val uploadState by viewModel.uploadState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(uploadState) {
        if (uploadState is UploadState.Finished) {
            val state = uploadState as UploadState.Finished
            if (state.failed == 0) {
                snackbarHostState.showSnackbar(context.getString(R.string.queued_success, state.successful))
                linkText = ""
                selectedFiles = emptyList()
                password = ""
                viewModel.resetState()
            } else if (state.successful == 0) {
                snackbarHostState.showSnackbar(context.getString(R.string.queued_failed, state.errors.firstOrNull().orEmpty()))
            } else {
                snackbarHostState.showSnackbar(context.getString(R.string.queued_partial, state.successful, state.failed))
            }
        }
    }

    if (uploadState is UploadState.Finished && (uploadState as UploadState.Finished).failed > 0) {
        val state = uploadState as UploadState.Finished
        AlertDialog(
            onDismissRequest = { viewModel.resetState() },
            title = { Text(stringResource(R.string.upload_report_title)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.upload_report_successful, state.successful))
                    Text(
                        stringResource(R.string.upload_report_failed, state.failed),
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    state.errors.forEach { err ->
                        Text(err, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.resetState() }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onNavigateBack) {
                        Text(stringResource(id = R.string.action_cancel))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            val validLinks =
                                linkText.split("\\s+".toRegex()).filter { isValidDownloadLink(it) }
                            val passwordValue = password.takeIf { it.isNotBlank() }
                            val seedingInt = when (selectedSeedingOption) {
                                R.string.seeding_seed -> 2
                                R.string.seeding_dont_seed -> 3
                                else -> 1
                            }
                            val usenetInt = when (selectedUsenetOption) {
                                R.string.usenet_none -> 0
                                R.string.usenet_repair -> 1
                                R.string.usenet_repair_unpack -> 2
                                R.string.usenet_repair_unpack_delete -> 3
                                else -> -1
                            }

                            coroutineScope.launch(Dispatchers.IO) {
                                val fileData = selectedFiles.mapNotNull { uri ->
                                    val name = context.getFileName(uri)
                                    val bytes = context.contentResolver.openInputStream(uri)
                                        ?.use { it.readBytes() }
                                    if (bytes != null) name to bytes else null
                                }
                                viewModel.queueDownloads(
                                    links = validLinks,
                                    files = fileData,
                                    password = passwordValue,
                                    seedingOption = seedingInt,
                                    usenetOption = usenetInt,
                                    allowZipped = allowZipped
                                )
                            }
                        },
                        enabled = totalDownloads > 0 && uploadState !is UploadState.Uploading
                    ) {
                        if (uploadState is UploadState.Uploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.action_add_downloads, totalDownloads))
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // Layout depending on screen size
            if (contentType == NullContentType.DUAL_PANE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LinkInputSection(
                        linkText = linkText,
                        onLinkChange = { linkText = it },
                        modifier = Modifier.weight(1f)
                    )
                    FileInputSection(
                        selectedFiles = selectedFiles,
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                LinkInputSection(
                    linkText = linkText,
                    onLinkChange = { linkText = it }
                )
                FileInputSection(
                    selectedFiles = selectedFiles,
                    onClick = { filePickerLauncher.launch(arrayOf("*/*")) }
                )
            }

            // Password Section
            Column {
                val labelString = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(stringResource(id = R.string.password_label))
                    }
                    append(" ")
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                        append(stringResource(id = R.string.optional_label))
                    }
                }
                Text(
                    text = labelString,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text(stringResource(id = R.string.password_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            HorizontalDivider()

            // Advanced Settings Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { advancedExpanded = !advancedExpanded }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.advanced_settings),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        painter = painterResource(id = if (advancedExpanded) R.drawable.ic_arrow_drop_up else R.drawable.ic_arrow_drop_down),
                        contentDescription = "Expand Advanced Settings"
                    )
                }

                AnimatedVisibility(visible = advancedExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Seeding Settings
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(id = R.string.seeding_settings),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                seedingOptions.forEach { optionRes ->
                                    SelectableOptionCard(
                                        text = stringResource(id = optionRes),
                                        selected = selectedSeedingOption == optionRes,
                                        onClick = { selectedSeedingOption = optionRes }
                                    )
                                }
                            }
                        }

                        // Usenet Post-Processing
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(id = R.string.usenet_post_processing),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                usenetOptions.forEach { optionRes ->
                                    SelectableOptionCard(
                                        text = stringResource(id = optionRes),
                                        selected = selectedUsenetOption == optionRes,
                                        onClick = { selectedUsenetOption = optionRes }
                                    )
                                }
                            }
                        }

                        // Allow Zipped Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.allow_zipped_downloads),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = allowZipped,
                                onCheckedChange = { allowZipped = it }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LinkInputSection(
    linkText: String,
    onLinkChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(id = R.string.link_label),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = linkText,
            onValueChange = onLinkChange,
            placeholder = { Text(stringResource(id = R.string.link_hint)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp),
            maxLines = 10
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileInputSection(
    selectedFiles: List<Uri>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        Text(
            text = stringResource(id = R.string.file_label),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedCard(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.file_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.file_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (selectedFiles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            selectedFiles.forEach { uri ->
                val fileName = remember(uri) { context.getFileName(uri) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_file), // Using standard ic_file found earlier
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectableOptionCard(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

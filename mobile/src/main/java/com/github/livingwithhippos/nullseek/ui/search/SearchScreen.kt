package com.github.livingwithhippos.nullseek.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.livingwithhippos.nullseek.NullRootUiState
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.data.model.Indexer
import com.github.livingwithhippos.nullseek.data.model.IndexerType
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.ui.utils.ExtendableFabMenu
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SearchScreen(
    contentType: NullContentType,
    uiState: NullRootUiState,
    navigationType: NullNavigationType,
    modifier: Modifier = Modifier,
    onNewDownload: () -> Unit = {},
    showSettings: () -> Unit = {},
    onNavigateToExternalServices: () -> Unit = {},
    viewModel: SearchViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SearchContent(
        state = state,
        navigationType = navigationType,
        modifier = modifier,
        onSearch = viewModel::onSearch,
        onSortOption = viewModel::setSortOption,
        onAscending = viewModel::setAscending,
        onNewDownload = onNewDownload,
        showSettings = showSettings,
        onNavigateToExternalServices = onNavigateToExternalServices,
    )
}

@Composable
fun SearchContent(
    state: SearchUiState,
    navigationType: NullNavigationType,
    modifier: Modifier = Modifier,
    onSearch: (query: String) -> Unit = {},
    onSortOption: (SearchSortOption) -> Unit = {},
    onAscending: (Boolean) -> Unit = {},
    onNewDownload: () -> Unit = {},
    showSettings: () -> Unit = {},
    onNavigateToExternalServices: () -> Unit = {},
    initialSheetVisible: Boolean = false,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager: FocusManager = LocalFocusManager.current
    var showSheet by rememberSaveable { mutableStateOf(initialSheetVisible) }
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    val scope = rememberCoroutineScope()


    val startSearch = {
        scope.launch {
            searchBarState.animateToCollapsed()
            focusManager.clearFocus()
            keyboard?.hide()
            val query = textFieldState.text.toString().trim()
            if (query.isNotEmpty())
                onSearch(query)
        }
    }

    val inputField =
        @Composable {
            SearchBarDefaults.InputField(
                textFieldState = textFieldState,
                searchBarState = searchBarState,
                onSearch = { startSearch() },
                enabled = state.hasActiveIndexers,
                placeholder = { Text(modifier = Modifier.clearAndSetSemantics {}, text = stringResource(R.string.search_hint))},
                leadingIcon = {
                    IconButton(
                        onClick = { startSearch() },
                        enabled = state.hasActiveIndexers,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = stringResource(R.string.search_action),
                        )
                    }
                },
                trailingIcon = {
                    if (textFieldState.text.isNotEmpty()) {
                        IconButton(onClick = { textFieldState.clearText()}) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.search_clear),
                            )
                        }
                    }
                },
            )
        }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchBar(
                    modifier = Modifier.weight(1f),
                    state = searchBarState,
                    inputField = inputField,
                )
                IconButton(onClick = { showSheet = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tune),
                        contentDescription = stringResource(R.string.search_options),
                    )
                }
            }

            if (state.hasActiveIndexers) {
                Text(
                    text = pluralStringResource(
                        R.plurals.search_active_indexers,
                        state.activeIndexers.size,
                        state.activeIndexers.size,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable(onClick = onNavigateToExternalServices)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (state.hasActiveIndexers) {
                    Text(
                        text = stringResource(R.string.search_prompt),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.search_no_indexers),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = onNavigateToExternalServices,
                        modifier = Modifier.padding(top = 16.dp),
                    ) {
                        Text(stringResource(R.string.search_open_external_services))
                    }
                }
            }
        }

        if (navigationType == NullNavigationType.BOTTOM_NAVIGATION) {
            ExtendableFabMenu(
                onNewDownload = onNewDownload,
                showSettings = showSettings,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }

    if (showSheet) {
        SearchSortSheet(
            sort = state.sort,
            onSortOption = onSortOption,
            onAscending = onAscending,
            onDismiss = { showSheet = false },
        )
    }
}

@Composable
private fun SearchSortOption.label(): String = stringResource(
    when (this) {
        SearchSortOption.RELEVANCE -> R.string.search_sort_relevance
        SearchSortOption.SIZE -> R.string.search_sort_size
        SearchSortOption.SEEDERS -> R.string.search_sort_seeders
        SearchSortOption.PEERS -> R.string.search_sort_peers
        SearchSortOption.AGE -> R.string.search_sort_age
        SearchSortOption.NAME -> R.string.search_sort_name
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSortSheet(
    sort: SearchSort,
    onSortOption: (SearchSortOption) -> Unit,
    onAscending: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.search_sort_by),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            SearchSortOption.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = sort.option == option,
                            onClick = { onSortOption(option) },
                            role = Role.RadioButton,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = sort.option == option, onClick = null)
                    Text(
                        option.label(),
                        modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp)
                    )
                }
            }
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                SegmentedButton(
                    selected = sort.ascending,
                    onClick = { onAscending(true) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text(stringResource(R.string.search_order_ascending)) }
                SegmentedButton(
                    selected = !sort.ascending,
                    onClick = { onAscending(false) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text(stringResource(R.string.search_order_descending)) }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp),
            ) { Text(stringResource(R.string.search_done)) }
        }
    }
}

private val previewIndexers = listOf(
    Indexer(1, "Jackett", IndexerType.JACKETT, "http://localhost:9117"),
    Indexer(2, "Prowlarr", IndexerType.PROWLARR, "http://localhost:9696"),
)

@Preview
@Composable
fun SearchScreenNoIndexersPreview() {
    NullSeekTheme {
        Surface {
            SearchContent(SearchUiState(), NullNavigationType.BOTTOM_NAVIGATION)
        }
    }
}

@Preview
@Composable
fun SearchScreenIndexersPreview() {
    NullSeekTheme {
        Surface {
            SearchContent(
                SearchUiState(activeIndexers = previewIndexers),
                NullNavigationType.BOTTOM_NAVIGATION,
            )
        }
    }
}

@Preview
@Composable
fun SearchSortSheetPreview() {
    NullSeekTheme {
        SearchSortSheet(SearchSort(SearchSortOption.SEEDERS, false), {}, {}, {})
    }
}

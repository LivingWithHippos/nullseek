package com.github.livingwithhippos.nullseek.ui.lists.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.github.livingwithhippos.nullseek.R
import kotlinx.coroutines.launch

/** Indices of the items whose name contains [query] (case-insensitive). Null items are skipped. */
fun findMatchIndices(names: List<String?>, query: String): List<Int> {
    val q = query.trim()
    if (q.isEmpty()) return emptyList()
    return names.mapIndexedNotNull { index, name ->
        if (name != null && name.contains(q, ignoreCase = true)) index else null
    }
}

/** Moves [current] by [step] among [count] matches, wrapping around. Returns 0 if there are none. */
fun wrapMatchPosition(current: Int, step: Int, count: Int): Int {
    if (count <= 0) return 0
    return ((current + step) % count + count) % count
}

enum class MatchHighlight { NONE, MATCH, CURRENT }

/** Find-in-list state for a paged list: matches, current position and navigation. */
class FindInListState(
    val matches: List<Int>,
    val position: Int,
    private val currentMatch: Int?,
    private val matchSet: Set<Int>,
    val onMove: (Int) -> Unit,
) {
    fun highlightFor(index: Int): MatchHighlight =
        when {
            index == currentMatch -> MatchHighlight.CURRENT
            index in matchSet -> MatchHighlight.MATCH
            else -> MatchHighlight.NONE
        }
}

/** Only the currently loaded items are searched. */
@Composable
fun <T : Any> rememberFindInListState(
    items: LazyPagingItems<T>,
    query: String,
    listState: LazyListState,
    name: (T) -> String?,
): FindInListState {
    val scope = rememberCoroutineScope()
    val matches by remember(items, query) {
        derivedStateOf { findMatchIndices(items.itemSnapshotList.map { it?.let(name) }, query) }
    }
    var position by remember { mutableIntStateOf(0) }

    LaunchedEffect(query, items.loadState.refresh is LoadState.Loading) { position = 0 }
    LaunchedEffect(matches.size) { if (position >= matches.size) position = 0 }

    val matchSet = remember(matches) { matches.toSet() }
    return FindInListState(
        matches = matches,
        position = position,
        currentMatch = matches.getOrNull(position),
        matchSet = matchSet,
        onMove = { step ->
            if (matches.isNotEmpty()) {
                position = wrapMatchPosition(position, step, matches.size)
                val target = matches[position]
                scope.launch { listState.animateScrollToItem(target) }
            }
        },
    )
}

@Composable
fun FindBar(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    matchCount: Int,
    position: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val hasQuery = query.isNotBlank()
    val canMove = matchCount > 0
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.find_in_list_hint)) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onNext() }),
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasQuery) {
                    Text(
                        text =
                            if (canMove)
                                stringResource(
                                    R.string.find_in_list_counter,
                                    position + 1,
                                    matchCount,
                                )
                            else stringResource(R.string.find_in_list_no_matches),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                IconButton(onClick = onPrevious, enabled = canMove) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_drop_up),
                        contentDescription = stringResource(R.string.find_in_list_previous),
                    )
                }
                IconButton(onClick = onNext, enabled = canMove) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_drop_down),
                        contentDescription = stringResource(R.string.find_in_list_next),
                    )
                }
                if (hasQuery) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.find_in_list_clear),
                        )
                    }
                }
            }
        },
    )
}

@Composable
fun matchCardColors(highlight: MatchHighlight): CardColors =
    when (highlight) {
        MatchHighlight.CURRENT ->
            CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        MatchHighlight.MATCH ->
            CardDefaults.outlinedCardColors(
                containerColor =
                    MaterialTheme.colorScheme.primary
                        .copy(alpha = 0.08f)
                        .compositeOver(MaterialTheme.colorScheme.surface)
            )
        MatchHighlight.NONE -> CardDefaults.outlinedCardColors()
    }

@Composable
fun matchCardBorder(highlight: MatchHighlight): BorderStroke {
    val primary = MaterialTheme.colorScheme.primary
    return when (highlight) {
        MatchHighlight.CURRENT -> BorderStroke(2.dp, primary)
        MatchHighlight.MATCH -> BorderStroke(1.dp, primary.copy(alpha = 0.5f))
        MatchHighlight.NONE -> CardDefaults.outlinedCardBorder()
    }
}

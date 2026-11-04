package com.github.livingwithhippos.nullseek.ui.newdownload

import androidx.compose.runtime.Immutable
import com.github.livingwithhippos.nullseek.core.domain.DebridFile
import com.github.livingwithhippos.nullseek.core.domain.DebridResult
import com.github.livingwithhippos.nullseek.core.domain.describe
import com.github.livingwithhippos.nullseek.core.domain.selectionToRequest

/** A torrent waiting for its files to be picked. */
@Immutable
data class PendingSelection(
    val torrentId: String,
    val name: String,
    val files: List<DebridFile>,
)

sealed interface SelectionDecision {
    data class Confirm(
        val fileIds: Set<Int>,
    ) : SelectionDecision

    /** leave it waiting on the provider, it can be finished from the torrent page */
    data object Skip : SelectionDecision

    /** select every file of this torrent and of the next ones without asking */
    data object SelectAllRemaining : SelectionDecision

    data object Delete : SelectionDecision
}

data class SelectionSummary(
    val selected: Int = 0,
    val skipped: Int = 0,
    val deleted: Int = 0,
    val errors: List<String> = emptyList(),
)

/**
 * Asks for the files of each torrent in turn. Torrents with a single file are selected without asking.
 */
class FileSelectionQueue(
    private val select: suspend (torrentId: String, fileIds: List<Int>?) -> DebridResult<Unit>,
    private val delete: suspend (torrentId: String) -> DebridResult<Unit>,
    private val ask: suspend (item: PendingSelection, remaining: Int) -> SelectionDecision,
) {
    suspend fun process(items: List<PendingSelection>): SelectionSummary {
        var selected = 0
        var skipped = 0
        var deleted = 0
        val errors = mutableListOf<String>()
        var selectAll = false

        suspend fun runSelect(
            item: PendingSelection,
            fileIds: List<Int>?,
        ) {
            when (val result = select(item.torrentId, fileIds)) {
                is DebridResult.Ok -> selected++
                is DebridResult.Err -> errors.add(result.error.describe("Selecting files of ${item.name}"))
            }
        }

        items.forEachIndexed { index, item ->
            if (selectAll || item.files.size <= 1) {
                runSelect(item, null)
                return@forEachIndexed
            }
            when (val decision = ask(item, items.size - index - 1)) {
                is SelectionDecision.Confirm -> {
                    if (decision.fileIds.isEmpty()) {
                        skipped++
                    } else {
                        runSelect(item, selectionToRequest(item.files.map { it.id }, decision.fileIds))
                    }
                }

                SelectionDecision.Skip -> {
                    skipped++
                }

                SelectionDecision.SelectAllRemaining -> {
                    selectAll = true
                    runSelect(item, null)
                }

                SelectionDecision.Delete -> {
                    when (val result = delete(item.torrentId)) {
                        is DebridResult.Ok -> deleted++
                        is DebridResult.Err -> errors.add(result.error.describe("Deleting ${item.name}"))
                    }
                }
            }
        }
        return SelectionSummary(selected, skipped, deleted, errors)
    }
}

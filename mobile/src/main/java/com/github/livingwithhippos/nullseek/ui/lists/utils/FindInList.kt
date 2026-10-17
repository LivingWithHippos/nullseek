package com.github.livingwithhippos.nullseek.ui.lists.utils

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

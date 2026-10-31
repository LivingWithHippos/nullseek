package com.github.livingwithhippos.nullseek.core.utils

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Month label for a TorBox bandwidth period start, e.g. "2026-10-01T00:00:00Z" -> "October 2026".
 * Falls back to the raw value if it can't be parsed.
 */
fun formatBandwidthMonth(
    isoDate: String,
    locale: Locale = Locale.getDefault(),
): String =
    runCatching {
        // "LLLL" is the stand-alone month name, the right form without a day number
        OffsetDateTime.parse(isoDate).format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
    }.getOrDefault(isoDate)

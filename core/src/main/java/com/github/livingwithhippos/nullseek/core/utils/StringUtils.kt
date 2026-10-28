package com.github.livingwithhippos.nullseek.core.utils

import logcat.asLog
import logcat.logcat
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.time.Clock
import kotlin.time.Instant

const val MAGNET_PATTERN: String = "magnet:\\?xt=urn:btih:([a-zA-Z0-9]{32,})"
const val TORRENT_PATTERN: String = "https?://[^\\s]{7,}\\.torrent"

const val FREE_PLAN = 0

// The download cooldown only applies to free accounts
fun isFreePlan(plan: Int): Boolean = plan == FREE_PLAN

fun getPlanName(plan: Int): String =
    when (plan) {
        FREE_PLAN -> "Free"
        1 -> "Essential"
        2 -> "Pro"
        3 -> "Standard"
        else -> "Unknown"
    }

fun convertToDays(datetime: String?): String {
    // todo: move to utils
    // todo: manage translational and no premium available
    if (datetime == null) {
        return "No Premium"
    }
    try {
        val dateInstant = Instant.parse(datetime)
        val now = Clock.System.now()
        val days = (dateInstant - now).inWholeDays
        if (days < 0) {
            return "Expired"
        }
        return "$days days"
    } catch (e: Exception) {
        logcat("UserScreen") { e.asLog() }
    }
    return datetime
}

fun convertToSize(bytes: Long): String =
    when {
        // todo: add a single digit to MB and GB
        bytes < MathConst.BYTES_IN_KB -> "$bytes B"

        bytes < MathConst.BYTES_IN_MB -> "${(bytes / MathConst.BYTES_IN_KB).toInt()} KB"

        bytes < MathConst.BYTES_IN_GB -> "${(bytes / MathConst.BYTES_IN_MB).toInt()} MB"

        else -> "${(bytes / MathConst.BYTES_IN_GB).toInt()} GB"
    }

fun formatToLocalDate(datetime: String?): String {
    if (datetime.isNullOrBlank()) return ""
    return try {
        val instant = java.time.Instant.parse(datetime)
        val formatter =
            DateTimeFormatter
                .ofLocalizedDate(FormatStyle.MEDIUM)
                .withZone(ZoneId.systemDefault())
                .withLocale(Locale.getDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        try {
            val zonedDateTime = ZonedDateTime.parse(datetime)
            val formatter =
                DateTimeFormatter
                    .ofLocalizedDate(FormatStyle.MEDIUM)
                    .withZone(ZoneId.systemDefault())
                    .withLocale(Locale.getDefault())
            formatter.format(zonedDateTime)
        } catch (e2: Exception) {
            logcat("StringUtils") { "Failed to parse datetime: $datetime, error: ${e2.asLog()}" }
            datetime
        }
    }
}

fun isDateNotInPast(datetime: String?): Boolean {
    if (datetime.isNullOrBlank()) return false
    return try {
        val instant = java.time.Instant.parse(datetime)
        val targetDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now(ZoneId.systemDefault())
        !targetDate.isBefore(today)
    } catch (e: Exception) {
        try {
            val zonedDateTime = ZonedDateTime.parse(datetime)
            val targetDate =
                zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDate()
            val today = LocalDate.now(ZoneId.systemDefault())
            !targetDate.isBefore(today)
        } catch (e2: Exception) {
            logcat("StringUtils") { "Failed to check if date in past: $datetime, error: ${e2.asLog()}" }
            false
        }
    }
}

val kbPattern = "\\s*(\\d+\\.?\\d*)\\s*[kK]".toRegex()
val mbPattern = "\\s*(\\d+\\.?\\d*)\\s*[mM]".toRegex()
val gbPattern = "\\s*(\\d+\\.?\\d*)\\s*[gG]".toRegex()
val genericPatter = "\\d+\\.?\\d*".toRegex()

fun parseCommonSize(rawSize: String?): Double? {
    try {
        if (rawSize.isNullOrBlank()) return null
        var match = kbPattern.find(rawSize)?.groupValues?.get(1)
        if (match != null) return match.toDouble() / 1024
        match = mbPattern.find(rawSize)?.groupValues?.get(1)
        if (match != null) return match.toDouble()
        match = gbPattern.find(rawSize)?.groupValues?.get(1)
        if (match != null) return match.toDouble() * 1024

        match = genericPatter.find(rawSize)?.value
        return match?.toDouble()
    } catch (e: NumberFormatException) {
        return null
    }
}

/** check if a String is a magnet link */
fun String?.isMagnet(): Boolean {
    if (this == null) return false
    val m: Matcher = Pattern.compile(MAGNET_PATTERN).matcher(this)
    return m.lookingAt()
}

/** check if a String is a torrent link */
fun String?.isTorrent(): Boolean {
    if (this == null) return false
    val m: Matcher = Pattern.compile(TORRENT_PATTERN).matcher(this)
    return m.matches()
}

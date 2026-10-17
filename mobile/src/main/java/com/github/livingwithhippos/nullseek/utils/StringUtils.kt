package com.github.livingwithhippos.nullseek.utils

import com.github.livingwithhippos.nullseek.core.utils.MathConst
import logcat.asLog
import logcat.logcat
import kotlin.time.Clock
import kotlin.time.Instant

fun getPlanName(plan: Int): String =
    when (plan) {
        0 -> "Free"
        1 -> "Essential"
        2 -> "Pro"
        3 -> "Standard"
        else -> "Unknown"
    }

fun convertToDays(datetime: String?): String {
    // todo: move to utils
    // todo: manage trnalsation and no premium avilable
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
            java.time.format.DateTimeFormatter
                .ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
                .withZone(java.time.ZoneId.systemDefault())
                .withLocale(java.util.Locale.getDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        try {
            val zonedDateTime = java.time.ZonedDateTime.parse(datetime)
            val formatter =
                java.time.format.DateTimeFormatter
                    .ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
                    .withZone(java.time.ZoneId.systemDefault())
                    .withLocale(java.util.Locale.getDefault())
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
        val targetDate = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val today = java.time.LocalDate.now(java.time.ZoneId.systemDefault())
        !targetDate.isBefore(today)
    } catch (e: Exception) {
        try {
            val zonedDateTime = java.time.ZonedDateTime.parse(datetime)
            val targetDate =
                zonedDateTime.withZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDate()
            val today = java.time.LocalDate.now(java.time.ZoneId.systemDefault())
            !targetDate.isBefore(today)
        } catch (e2: Exception) {
            logcat("StringUtils") { "Failed to check if date in past: $datetime, error: ${e2.asLog()}" }
            false
        }
    }
}

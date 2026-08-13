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
        bytes < MathConst.BYTES_IN_KB -> "$bytes B"
        bytes < MathConst.BYTES_IN_MB -> "${(bytes / MathConst.BYTES_IN_KB).toInt()} KB"
        bytes < MathConst.BYTES_IN_GB -> "${(bytes / MathConst.BYTES_IN_MB).toInt()} MB"
        else -> "${(bytes / MathConst.BYTES_IN_GB).toInt()} GB"
    }

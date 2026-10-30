package com.github.livingwithhippos.nullseek.core.utils

import java.util.Locale

private val BTIH_REGEX = Regex("urn:btih:([a-zA-Z0-9]+)", RegexOption.IGNORE_CASE)
private val HEX_HASH_REGEX = Regex("^[a-fA-F0-9]{40}$")
private val BASE32_HASH_REGEX = Regex("^[a-zA-Z2-7]{32}$")
private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

/** The torrent info hash in a magnet link, as lowercase hex, or null. */
fun extractInfoHash(magnet: String?): String? {
    if (magnet.isNullOrBlank()) return null
    val raw = BTIH_REGEX.find(magnet)?.groupValues?.get(1) ?: return null
    return normalizeInfoHash(raw)
}

/** Lowercase 40-char hex for a hex or base32 info hash, null for anything else. */
fun normalizeInfoHash(hash: String?): String? {
    val trimmed = hash?.trim() ?: return null
    return when {
        HEX_HASH_REGEX.matches(trimmed) -> trimmed.lowercase(Locale.ROOT)
        BASE32_HASH_REGEX.matches(trimmed) -> base32ToHex(trimmed.uppercase(Locale.ROOT))
        else -> null
    }
}

private fun base32ToHex(base32: String): String {
    val bytes = ByteArray(base32.length * 5 / 8)
    var buffer = 0
    var bits = 0
    var index = 0
    for (char in base32) {
        buffer = (buffer shl 5) or BASE32_ALPHABET.indexOf(char)
        bits += 5
        if (bits >= 8) {
            bits -= 8
            bytes[index++] = (buffer shr bits and 0xFF).toByte()
        }
    }
    return bytes.toHex()
}

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

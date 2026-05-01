package com.soulmatch.app.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val shortDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
private val chatTimeFormatter = DateTimeFormatter.ofPattern("dd MMM, hh:mm a", Locale.ENGLISH)

fun titleCase(value: String): String {
    return value.split("_", " ").filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            token.lowercase(Locale.ENGLISH).replaceFirstChar { char -> char.titlecase(Locale.ENGLISH) }
        }
}

fun formatCurrency(amount: Int): String = if (amount == 0) "Free" else "Rs $amount"

fun formatDate(value: String?): String {
    if (value.isNullOrBlank()) return "Not available"
    return runCatching {
        shortDateFormatter.format(Instant.parse(value).atZone(ZoneId.systemDefault()))
    }.getOrElse { value }
}

fun formatChatTime(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return runCatching {
        chatTimeFormatter.format(Instant.parse(value).atZone(ZoneId.systemDefault()))
    }.getOrElse { value }
}

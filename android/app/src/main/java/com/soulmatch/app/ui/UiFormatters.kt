package com.soulmatch.app.ui

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val shortDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
private val chatTimeFormatter = DateTimeFormatter.ofPattern("dd MMM, hh:mm a", Locale.ENGLISH)
private val supportedLocalDateFormatters = listOf(
    DateTimeFormatter.ISO_LOCAL_DATE,
    DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH),
    DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
    DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.ENGLISH)
)

fun titleCase(value: String): String {
    return value.split("_", " ").filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            token.lowercase(Locale.ENGLISH).replaceFirstChar { char -> char.titlecase(Locale.ENGLISH) }
        }
}

fun formatCurrency(amount: Int): String = if (amount == 0) "Free" else "Rs $amount"

fun formatDate(value: String?): String {
    if (value.isNullOrBlank()) return "Not available"
    val trimmed = value.trim()
    runCatching {
        return shortDateFormatter.format(Instant.parse(trimmed).atZone(ZoneId.systemDefault()))
    }
    supportedLocalDateFormatters.forEach { formatter ->
        runCatching {
            return shortDateFormatter.format(LocalDate.parse(trimmed, formatter))
        }
    }
    runCatching {
        return shortDateFormatter.format(LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate())
    }
    return trimmed
}

fun formatDateMillis(value: Long): String {
    if (value <= 0L) return "Not available"
    return runCatching {
        shortDateFormatter.format(Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()))
    }.getOrElse { "Not available" }
}

fun formatChatTime(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return runCatching {
        chatTimeFormatter.format(Instant.parse(value).atZone(ZoneId.systemDefault()))
    }.getOrElse { value }
}

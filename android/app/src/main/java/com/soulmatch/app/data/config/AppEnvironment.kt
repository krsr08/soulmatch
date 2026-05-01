package com.soulmatch.app.data.config

import com.soulmatch.app.BuildConfig

object AppEnvironment {
    private val backendUrls = listOf(
        BuildConfig.AUTH_BASE_URL,
        BuildConfig.PROFILE_BASE_URL,
        BuildConfig.MATCHING_BASE_URL,
        BuildConfig.SEARCH_BASE_URL,
        BuildConfig.CHAT_BASE_URL,
        BuildConfig.PAYMENT_BASE_URL
    )

    val allowDemoFallback: Boolean = BuildConfig.DEBUG && backendUrls.any { url ->
        url.contains("10.0.2.2") ||
            url.contains("localhost", ignoreCase = true) ||
            url.contains("127.0.0.1") ||
            url.contains("192.168.")
    }

    val publicMediaBaseUrl: String = BuildConfig.PROFILE_BASE_URL
        .substringBefore("/api/v1")
        .trimEnd('/')
}

fun mediaUrl(url: String?): String? {
    val value = url?.trim().orEmpty()
    if (value.isBlank()) return null
    if (
        value.startsWith("http://", ignoreCase = true) ||
        value.startsWith("https://", ignoreCase = true) ||
        value.startsWith("content://", ignoreCase = true) ||
        value.startsWith("file://", ignoreCase = true)
    ) {
        return value
    }
    if (value.startsWith("/")) return AppEnvironment.publicMediaBaseUrl + value
    return value
}

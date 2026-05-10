package com.soulmatch.app.util

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.soulmatch.app.BuildConfig

object CrashReporter {
    private val crashlytics: FirebaseCrashlytics
        get() = FirebaseCrashlytics.getInstance()

    fun initialize() {
        crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
        crashlytics.setCustomKey("version_name", BuildConfig.VERSION_NAME)
        crashlytics.setCustomKey("version_code", BuildConfig.VERSION_CODE)
        breadcrumb("app_start:${BuildConfig.BUILD_TYPE}:${BuildConfig.VERSION_NAME}")
    }

    fun identify(
        userId: String?,
        userType: String?,
        profileId: String? = null,
        advisorId: String? = null
    ) {
        crashlytics.setUserId(userId?.takeIf { it.isNotBlank() } ?: "anonymous")
        crashlytics.setCustomKey("user_type", userType?.takeIf { it.isNotBlank() } ?: "unknown")
        crashlytics.setCustomKey("has_profile_id", !profileId.isNullOrBlank())
        crashlytics.setCustomKey("has_advisor_id", !advisorId.isNullOrBlank())
    }

    fun clearUser() {
        crashlytics.setUserId("anonymous")
        crashlytics.setCustomKey("user_type", "anonymous")
        crashlytics.setCustomKey("has_profile_id", false)
        crashlytics.setCustomKey("has_advisor_id", false)
    }

    fun breadcrumb(message: String) {
        if (message.isBlank()) return
        crashlytics.log(message.take(500))
    }

    fun recordNonFatal(error: Throwable, context: String? = null) {
        context?.takeIf { it.isNotBlank() }?.let { breadcrumb("nonfatal:$it") }
        crashlytics.recordException(error)
    }
}

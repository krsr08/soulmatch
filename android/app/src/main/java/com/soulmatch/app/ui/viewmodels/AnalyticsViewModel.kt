package com.soulmatch.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmatch.app.BuildConfig
import com.soulmatch.app.data.api.ControlPlaneApiService
import com.soulmatch.app.data.local.UserPreferences
import com.soulmatch.app.data.models.AnalyticsEventRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val controlPlaneApi: ControlPlaneApiService,
    private val prefs: UserPreferences
) : ViewModel() {
    private val sessionId = UUID.randomUUID().toString()
    private var lastTrackedPage: String? = null

    fun trackPage(page: String) {
        if (page.isBlank() || page == lastTrackedPage) return
        lastTrackedPage = page
        track("page_view", page = page)
    }

    fun trackClick(target: String, page: String) {
        if (target.isBlank()) return
        track("click", page = page, target = target)
    }

    private fun track(eventType: String, page: String? = null, target: String? = null) {
        viewModelScope.launch {
            runCatching {
                controlPlaneApi.trackAnalytics(
                    AnalyticsEventRequest(
                        eventType = eventType,
                        userId = prefs.userId.first(),
                        sessionId = sessionId,
                        page = page,
                        target = target,
                        appVersion = BuildConfig.VERSION_NAME,
                        payload = mapOf("route" to (page ?: ""), "target" to (target ?: ""))
                    )
                )
            }
        }
    }
}

package com.soulmatch.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmatch.app.BuildConfig
import com.soulmatch.app.data.api.ControlPlaneApiService
import com.soulmatch.app.data.local.UserPreferences
import com.soulmatch.app.data.models.AnalyticsBatchRequest
import com.soulmatch.app.data.models.AnalyticsEventRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val controlPlaneApi: ControlPlaneApiService,
    private val prefs: UserPreferences
) : ViewModel() {
    private val sessionId = UUID.randomUUID().toString()
    private val queueMutex = Mutex()
    private val pendingEvents = mutableListOf<AnalyticsEventRequest>()
    private var lastTrackedPage: String? = null
    private var flushJob: Job? = null

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
            val event = AnalyticsEventRequest(
                eventType = eventType,
                userId = prefs.userId.first(),
                sessionId = sessionId,
                page = page,
                target = target,
                appVersion = BuildConfig.VERSION_NAME,
                payload = mapOf(
                    "route" to (page ?: ""),
                    "target" to (target ?: ""),
                    "clientEventId" to UUID.randomUUID().toString()
                )
            )
            enqueue(event)
        }
    }

    private suspend fun enqueue(event: AnalyticsEventRequest) {
        val shouldFlushNow = queueMutex.withLock {
            pendingEvents.add(event)
            pendingEvents.size >= MAX_BATCH_SIZE
        }
        if (shouldFlushNow) {
            flushJob?.cancel()
            flushPending()
        } else {
            scheduleFlush()
        }
    }

    private fun scheduleFlush() {
        if (flushJob?.isActive == true) return
        flushJob = viewModelScope.launch {
            delay(FLUSH_DELAY_MS)
            flushPending()
        }
    }

    private suspend fun flushPending() {
        val batch = queueMutex.withLock {
            if (pendingEvents.isEmpty()) {
                emptyList()
            } else {
                pendingEvents.take(MAX_BATCH_SIZE).also { pendingEvents.subList(0, it.size).clear() }
            }
        }
        if (batch.isEmpty()) return
        runCatching {
            controlPlaneApi.trackAnalyticsBatch(AnalyticsBatchRequest(batch))
        }.onFailure {
            queueMutex.withLock {
                pendingEvents.addAll(0, batch.take(MAX_REQUEUE_SIZE))
            }
        }
    }

    companion object {
        private const val MAX_BATCH_SIZE = 10
        private const val MAX_REQUEUE_SIZE = 20
        private const val FLUSH_DELAY_MS = 5_000L
    }
}

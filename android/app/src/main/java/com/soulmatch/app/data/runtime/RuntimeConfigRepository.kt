package com.soulmatch.app.data.runtime

import com.soulmatch.app.data.api.ControlPlaneApiService
import com.soulmatch.app.data.models.RuntimeConfigData
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class RuntimeConfigRepository @Inject constructor(
    private val controlPlaneApi: ControlPlaneApiService
) {
    private val _config = MutableStateFlow(RuntimeConfigData())
    val config: StateFlow<RuntimeConfigData> = _config.asStateFlow()
    private var etag: String? = null
    private var failureCount = 0

    suspend fun refresh(): Long {
        val response = runCatching { controlPlaneApi.getRuntimeConfig(etag) }.getOrElse {
            failureCount += 1
            return nextDelayMs()
        }
        if (response.code() == 304) {
            failureCount = 0
            return DEFAULT_POLL_MS
        }
        if (response.isSuccessful) {
            response.headers()["ETag"]?.let { etag = it }
            response.body()
                ?.takeIf { it.success }
                ?.data
                ?.let { _config.value = it }
            failureCount = 0
            return DEFAULT_POLL_MS
        }
        failureCount += 1
        return nextDelayMs()
    }

    private fun nextDelayMs(): Long {
        val factor = 1L shl failureCount.coerceIn(0, 4)
        return (DEFAULT_POLL_MS * factor).coerceAtMost(MAX_POLL_MS)
    }

    companion object {
        private const val DEFAULT_POLL_MS = 45_000L
        private const val MAX_POLL_MS = 5 * 60_000L
    }
}

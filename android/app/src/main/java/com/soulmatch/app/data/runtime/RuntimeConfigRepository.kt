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

    suspend fun refresh() {
        runCatching { controlPlaneApi.getRuntimeConfig() }
            .getOrNull()
            ?.body()
            ?.takeIf { it.success }
            ?.data
            ?.let { _config.value = it }
    }
}

package com.soulmatch.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmatch.app.data.api.NotificationApiService
import com.soulmatch.app.data.models.NotificationData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationApi: NotificationApiService
) : ViewModel() {
    private val _notifications = MutableStateFlow<List<NotificationData>>(emptyList())
    val notifications: StateFlow<List<NotificationData>> = _notifications

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val response = runCatching { notificationApi.getNotifications() }.getOrNull()
            val body = response?.body()
            if (response?.isSuccessful == true && body?.success == true) {
                _notifications.value = body.data.orEmpty()
            } else {
                _error.value = body?.error?.message ?: "Notifications could not be loaded."
            }
            _isLoading.value = false
        }
    }

    fun markRead(notificationId: String) {
        if (notificationId.isBlank()) return
        viewModelScope.launch {
            runCatching { notificationApi.markRead(notificationId) }
            _notifications.value = _notifications.value.map {
                if (it.notificationId == notificationId) it.copy(status = "read") else it
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            runCatching { notificationApi.markAllRead() }
            _notifications.value = _notifications.value.map { it.copy(status = "read") }
        }
    }
}

package com.soulmatch.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmatch.app.data.api.ChatApiService
import com.soulmatch.app.data.config.AppEnvironment
import com.soulmatch.app.data.mock.MarketFixtures
import com.soulmatch.app.data.models.ConversationItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatApi: ChatApiService
) : ViewModel() {
    private val _conversations = MutableStateFlow<List<ConversationItem>>(emptyList())
    private val _isLoading = MutableStateFlow(false)

    val conversations: StateFlow<List<ConversationItem>> = _conversations.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _conversations.value = runCatching { chatApi.getConversations() }
                .getOrNull()
                ?.body()
                ?.takeIf { it.success }
                ?.data
                ?.takeIf { it.isNotEmpty() }
                ?: if (AppEnvironment.allowDemoFallback) MarketFixtures.conversations else emptyList()
            _isLoading.value = false
        }
    }
}

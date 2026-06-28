package com.soulmatch.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmatch.app.BuildConfig
import com.soulmatch.app.data.api.ChatApiService
import com.soulmatch.app.data.chatbot.BotFlowEngine
import com.soulmatch.app.data.config.AppEnvironment
import com.soulmatch.app.data.local.UserPreferences
import com.soulmatch.app.data.mock.MarketFixtures
import com.soulmatch.app.data.models.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

@HiltViewModel
class ChatThreadViewModel @Inject constructor(
    private val chatApi: ChatApiService,
    private val prefs: UserPreferences
) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _currentUserId = MutableStateFlow(if (AppEnvironment.allowDemoFallback) MarketFixtures.currentUserId else "")
    private val _isLoading = MutableStateFlow(false)
    private val _sendStatus = MutableStateFlow<String?>(null)
    private var socket: Socket? = null
    private var activePartnerId: String? = null
    private var socketToken: String? = null

    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val sendStatus: StateFlow<String?> = _sendStatus.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.authToken.distinctUntilChanged().collect { token ->
                val partnerId = activePartnerId ?: return@collect
                if (!token.isNullOrBlank() && token != socketToken) {
                    connectSocket(partnerId)
                }
            }
        }
    }

    fun load(participantUserId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _sendStatus.value = null
            _currentUserId.value = prefs.userId.first() ?: if (AppEnvironment.allowDemoFallback) MarketFixtures.currentUserId else ""
            activePartnerId = participantUserId
            val chatId = buildChatId(_currentUserId.value, participantUserId)
            val channel = if (AppEnvironment.allowDemoFallback) MarketFixtures.conversationForParticipant(participantUserId) else null
            val loadedMessages = runCatching { chatApi.getMessages(chatId) }
                .getOrNull()
                ?.body()
                ?.takeIf { it.success }
                ?.data
                ?.messages
                ?.takeIf { it.isNotEmpty() }
                ?: if (AppEnvironment.allowDemoFallback) MarketFixtures.conversationMessages(participantUserId) else emptyList()
            _messages.value = BotFlowEngine.mergeFlowMessages(
                channel = channel,
                messages = loadedMessages,
                currentUserId = _currentUserId.value
            )
            connectSocket(participantUserId)
            socket?.emit("message:read", JSONObject().put("chatId", chatId))
            _isLoading.value = false
        }
    }

    fun sendMessage(participantUserId: String, content: String) {
        val trimmed = content.trim()
        if (trimmed.isBlank()) return
        _sendStatus.value = null
        val chatId = buildChatId(_currentUserId.value, participantUserId)
        val fallbackMessage = ChatMessage(
            messageId = "local-${System.currentTimeMillis()}",
            chatId = chatId,
            senderId = _currentUserId.value,
            receiverId = participantUserId,
            content = trimmed,
            status = "sent",
            sentAt = "Now"
        )
        val payload = JSONObject()
            .put("receiverId", participantUserId)
            .put("type", "text")
            .put("content", trimmed)
        val activeSocket = socket
        if (activeSocket == null || !activeSocket.connected()) {
            if (AppEnvironment.allowDemoFallback) {
                _messages.update { it + fallbackMessage }
            } else {
                _sendStatus.value = "Chat is still connecting. Please try again in a moment."
            }
            return
        }
        activeSocket.emit("message:send", payload, Ack { args ->
            val ack = args.firstOrNull() as? JSONObject
            val error = ack?.optString("error")?.takeIf { it.isNotBlank() }
            if (error != null) {
                _sendStatus.value = error
                return@Ack
            }
            val message = ack?.optJSONObject("message")?.toChatMessage() ?: fallbackMessage
            _messages.update { list ->
                if (list.any { it.messageId == message.messageId }) list else list + message
            }
        })
    }

    fun clearSendStatus() {
        _sendStatus.value = null
    }

    private suspend fun connectSocket(participantUserId: String) {
        val token = prefs.authToken.first()
        if (token.isNullOrBlank()) return
        if (socket?.connected() == true && activePartnerId == participantUserId && socketToken == token) return
        socket?.off()
        socket?.disconnect()
        socketToken = token

        val options = IO.Options()
        options.forceNew = true
        options.reconnection = true
        options.auth = mapOf("token" to token)
        socket = IO.socket(BuildConfig.CHAT_BASE_URL.substringBefore("/api/v1"), options).apply {
            on("message:received") { args ->
                val payload = args.firstOrNull() as? JSONObject ?: return@on
                val message = payload.toChatMessage()
                if (message.senderId == participantUserId) {
                    _messages.update { list ->
                        if (list.any { it.messageId == message.messageId }) list else list + message
                    }
                    emit("message:read", JSONObject().put("chatId", message.chatId))
                }
            }
            connect()
        }
    }

    override fun onCleared() {
        socket?.off()
        socket?.disconnect()
        socketToken = null
        super.onCleared()
    }
}

private fun buildChatId(currentUserId: String, participantUserId: String): String {
    return listOf(currentUserId, participantUserId)
        .filter { it.isNotBlank() }
        .sorted()
        .joinToString(":")
}

private fun JSONObject.toChatMessage(): ChatMessage {
    return ChatMessage(
        messageId = optString("messageId"),
        chatId = optString("chatId"),
        senderId = optString("senderId"),
        receiverId = optString("receiverId"),
        type = optString("type", "text"),
        content = optString("content"),
        status = optString("status", "sent"),
        sentAt = optString("sentAt"),
        flowStepId = optString("flowStepId").takeIf { it.isNotBlank() },
        alias = optString("alias").takeIf { it.isNotBlank() },
        messageType = optString("messageType").takeIf { it.isNotBlank() },
        messageUserType = optString("messageUserType").takeIf { it.isNotBlank() },
        messageUserAlias = optString("messageUserAlias").takeIf { it.isNotBlank() },
        createdMillis = optLong("createdMillis").takeIf { it > 0L }
    )
}

package com.soulmatch.app.data.chatbot

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.soulmatch.app.data.models.BotFlowMessage
import com.soulmatch.app.data.models.ChatMessage
import com.soulmatch.app.data.models.ConversationItem
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object BotFlowEngine {
    private val gson = Gson()
    private val flowMessageListType = object : TypeToken<List<BotFlowMessage>>() {}.type

    fun mergeFlowMessages(
        channel: ConversationItem?,
        messages: List<ChatMessage>,
        currentUserId: String,
        now: ZonedDateTime = ZonedDateTime.now()
    ): List<ChatMessage> {
        if (channel == null || !businessHoursAllow(channel.flowBusinessHourType, now)) return messages
        val flowMessages = channel.flowMessagesOrJson()
            .filter { it.flowStepId.isNotBlank() }
            .distinctBy { it.flowStepId }
        if (flowMessages.isEmpty()) return messages

        val existingFlowSteps = messages.mapNotNull { it.flowStepId }.toSet()
        val rendered = flowMessages
            .filterNot { it.flowStepId in existingFlowSteps }
            .map { it.toChatMessage(channel, currentUserId) }
        return rendered + messages
    }

    fun businessHoursAllow(flowBusinessHourType: String?, now: ZonedDateTime): Boolean {
        return when (flowBusinessHourType?.trim()?.lowercase()) {
            null, "", "always", "all_hours" -> true
            "disabled", "never" -> false
            "business_hours" -> {
                val hour = now.hour
                val day = now.dayOfWeek.value
                day in 1..5 && hour in 9..17
            }
            "after_hours" -> !businessHoursAllow("business_hours", now)
            else -> true
        }
    }

    private fun ConversationItem.flowMessagesOrJson(): List<BotFlowMessage> {
        if (flowMessages.isNotEmpty()) return flowMessages
        if (flowMessagesJson.isNullOrBlank()) return emptyList()
        return runCatching {
            gson.fromJson<List<BotFlowMessage>>(flowMessagesJson, flowMessageListType)
        }.getOrDefault(emptyList())
    }

    private fun BotFlowMessage.toChatMessage(channel: ConversationItem, currentUserId: String): ChatMessage {
        val timestamp = createdMillis.takeIf { it > 0L }
            ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toInstant().toString() }
            .orEmpty()
        return ChatMessage(
            messageId = "flow-${channel.chatId}-$flowStepId",
            chatId = channel.chatId,
            senderId = channel.serviceAccountId ?: "soulmatch-bot",
            receiverId = currentUserId,
            type = messageType.ifBlank { "text" },
            content = content,
            status = "delivered",
            sentAt = timestamp,
            flowStepId = flowStepId,
            alias = alias.ifBlank { null },
            messageType = messageType,
            messageUserType = messageUserType,
            messageUserAlias = messageUserAlias,
            createdMillis = createdMillis.takeIf { it > 0L }
        )
    }
}

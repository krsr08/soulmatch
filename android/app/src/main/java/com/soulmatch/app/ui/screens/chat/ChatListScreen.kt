package com.soulmatch.app.ui.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.data.models.ConversationItem
import com.soulmatch.app.ui.components.dialogs.CallActionDialog
import com.soulmatch.app.ui.components.media.MemberPhoto
import com.soulmatch.app.ui.components.premium.MetricPill
import com.soulmatch.app.ui.components.premium.PremiumCard
import com.soulmatch.app.ui.components.premium.PremiumHeader
import com.soulmatch.app.ui.components.premium.PremiumScreen
import com.soulmatch.app.ui.components.premium.SectionTitle
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.PrimaryDark
import com.soulmatch.app.ui.theme.Success
import com.soulmatch.app.ui.theme.SuccessSoft
import com.soulmatch.app.ui.theme.SurfaceSoft
import com.soulmatch.app.ui.theme.SurfaceWarm
import com.soulmatch.app.ui.theme.TextSecondary
import com.soulmatch.app.ui.formatChatTime
import com.soulmatch.app.ui.viewmodels.ChatListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onBack: () -> Unit = {},
    onViewProfile: ((String) -> Unit)? = null,
    onOpenChat: ((String, String) -> Unit)? = null,
    onSubscribe: (() -> Unit)? = null,
    onEditSection: ((Int) -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    profileId: String = "",
    chatId: String = "",
    participantName: String = "",
    vm: ChatListViewModel = hiltViewModel()
) {
    val conversations by vm.conversations.collectAsStateWithLifecycle()
    val loading by vm.isLoading.collectAsStateWithLifecycle()
    val openChat: (String, String) -> Unit = onOpenChat ?: { _, _ -> }
    val unreadCount = conversations.sumOf { it.unreadCounts.values.sum() }
    var callTarget by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    callTarget?.let { target ->
        CallActionDialog(
            memberName = target.first,
            isVideo = target.second,
            onDismiss = { callTarget = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        PremiumScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (loading && conversations.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        PremiumHeader(
                            eyebrow = "Mutual-interest chat",
                            title = "Conversations with context",
                            subtitle = "Chat opens after mutual interest, with room for safety cues and future call controls."
                        )
                    }
                    item {
                        PremiumCard(modifier = Modifier.padding(horizontal = 16.dp), containerColor = MaterialTheme.colorScheme.surface) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    MetricPill("Chats", conversations.size.toString(), modifier = Modifier.weight(1f), background = SurfaceWarm)
                                    MetricPill("Unread", unreadCount.toString(), modifier = Modifier.weight(1f), accent = Success, background = SuccessSoft)
                                    MetricPill("Mutual", conversations.count { it.lastMessage != null }.toString(), modifier = Modifier.weight(1f), background = SurfaceSoft)
                                }
                            }
                        }
                    }
                    item {
                        SectionTitle(
                            title = "Active conversations",
                            subtitle = "Sorted by newest reply and unread signals",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                        )
                    }
                    items(conversations, key = { it.chatId }) { conversation ->
                        ConversationRow(
                            item = conversation,
                            onOpen = { openChat(conversation.participantUserId, conversation.participantName) },
                            onCall = { callTarget = conversation.participantName to false },
                            onVideo = { callTarget = conversation.participantName to true }
                        )
                    }
                    if (conversations.isEmpty()) {
                        item {
                            PremiumCard(modifier = Modifier.padding(16.dp), containerColor = SurfaceWarm) {
                                Column(Modifier.padding(2.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("No active conversations yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Once a mutual interest happens, chats will show up here with the profile context still visible.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    item: ConversationItem,
    onOpen: () -> Unit,
    onCall: () -> Unit,
    onVideo: () -> Unit
) {
    val unread = item.unreadCounts.values.firstOrNull { it > 0 } ?: 0
    PremiumCard(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onOpen),
        contentPadding = PaddingValues(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            MemberPhoto(
                photoUrl = item.participantPhotoUrl,
                contentDescription = item.participantName,
                modifier = Modifier
                    .size(56.dp),
                shape = RoundedCornerShape(14.dp)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.participantName.ifBlank { "Member" },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(formatChatTime(item.lastMessage?.sentAt).ifBlank { "New" }, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.lastMessage?.content ?: "Mutual interest unlocked. Start with a thoughtful note.",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (unread > 0) {
                        Surface(shape = RoundedCornerShape(99.dp), color = MaterialTheme.colorScheme.primary) {
                            Text(
                                unread.toString(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(8.dp), color = SurfaceWarm, border = BorderStroke(1.dp, Divider)) {
                        Text(
                            "Mutual",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onCall, modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp))) {
                            Icon(Icons.Filled.Call, contentDescription = "Voice call", tint = PrimaryDark, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onVideo, modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp))) {
                            Icon(Icons.Filled.Videocam, contentDescription = "Video call", tint = PrimaryDark, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

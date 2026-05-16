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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
    var tab by remember { mutableStateOf(0) }
    var callTarget by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    val visibleConversations = when (tab) {
        1 -> conversations.filter { it.unreadCounts.values.any { unread -> unread > 0 } }
        else -> conversations
    }

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
                title = { Text("Messenger", fontWeight = FontWeight.Bold) },
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
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, Divider)
                        ) {
                            TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface) {
                                listOf("Mutual", "Interested in Me").forEachIndexed { index, label ->
                                    Tab(
                                        selected = tab == index,
                                        onClick = { tab = index },
                                        text = { Text(label, fontWeight = if (tab == index) FontWeight.ExtraBold else FontWeight.SemiBold) }
                                    )
                                }
                            }
                        }
                    }
                    items(visibleConversations, key = { it.chatId }) { conversation ->
                        ConversationRow(
                            item = conversation,
                            onOpen = { openChat(conversation.participantUserId, conversation.participantName) },
                            onCall = { callTarget = conversation.participantName to false },
                            onVideo = { callTarget = conversation.participantName to true }
                        )
                    }
                    if (visibleConversations.isEmpty()) {
                        item {
                            PremiumCard(modifier = Modifier.padding(16.dp), containerColor = SurfaceWarm) {
                                Column(Modifier.padding(2.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(if (tab == 0) "No mutual conversations yet" else "No profiles interested in you yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(if (tab == 0) "Once a mutual interest happens, conversations will show up here." else "Unread or newly interested profiles will appear in this tab.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
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

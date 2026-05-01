package com.soulmatch.app.ui.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.data.models.ChatMessage
import com.soulmatch.app.ui.components.AvatarInitial
import com.soulmatch.app.ui.components.CallActionDialog
import com.soulmatch.app.ui.components.ChipTone
import com.soulmatch.app.ui.components.PremiumCard
import com.soulmatch.app.ui.components.PremiumScreen
import com.soulmatch.app.ui.components.SignalChip
import com.soulmatch.app.ui.components.UpgradePlanGate
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.Info
import com.soulmatch.app.ui.theme.InfoSoft
import com.soulmatch.app.ui.theme.PrimaryDark
import com.soulmatch.app.ui.theme.Success
import com.soulmatch.app.ui.theme.SuccessSoft
import com.soulmatch.app.ui.theme.SurfaceSoft
import com.soulmatch.app.ui.theme.SurfaceWarm
import com.soulmatch.app.ui.theme.TextSecondary
import com.soulmatch.app.ui.formatChatTime
import com.soulmatch.app.ui.viewmodels.ChatThreadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit = {},
    onViewProfile: ((String) -> Unit)? = null,
    onOpenChat: ((String, String) -> Unit)? = null,
    onSubscribe: (() -> Unit)? = null,
    onEditSection: ((Int) -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    profileId: String = "",
    chatId: String = "",
    participantName: String = "",
    vm: ChatThreadViewModel = hiltViewModel()
) {
    val messages by vm.messages.collectAsStateWithLifecycle()
    val currentUserId by vm.currentUserId.collectAsStateWithLifecycle()
    val loading by vm.isLoading.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }
    var callAction by remember { mutableStateOf<Boolean?>(null) }
    val openSubscription: (() -> Unit)? = onSubscribe
    val prompts = listOf(
        "What does a good weekend with family look like for you?",
        "I liked your profile. What values matter most to you in marriage?",
        "Would you be open to a family introduction after we speak a little?"
    )

    LaunchedEffect(chatId) {
        vm.load(chatId)
    }

    callAction?.let { isVideo ->
        CallActionDialog(
            memberName = participantName,
            isVideo = isVideo,
            onDismiss = { callAction = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        AvatarInitial(participantName.ifBlank { "S" }, modifier = Modifier.size(42.dp), background = MaterialTheme.colorScheme.primary)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(participantName.ifBlank { "Conversation" }, fontWeight = FontWeight.Bold)
                            Text("Mutual-interest chat", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { callAction = false }) {
                        Icon(Icons.Filled.Call, contentDescription = "Voice call")
                    }
                    IconButton(onClick = { callAction = true }) {
                        Icon(Icons.Filled.Videocam, contentDescription = "Video call")
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp, border = BorderStroke(1.dp, Divider.copy(alpha = 0.7f))) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Write a thoughtful message") },
                        singleLine = false,
                        minLines = 1,
                        maxLines = 3
                    )
                    FilledTonalIconButton(
                        onClick = {
                            vm.sendMessage(chatId, draft)
                            draft = ""
                        },
                        enabled = draft.isNotBlank(),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    ) { padding ->
        PremiumScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (loading && messages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 18.dp)
                ) {
                    item {
                        SafetyBanner()
                    }
                    item {
                        UpgradePlanGate(
                            title = "Upgrade for richer conversations",
                            detail = "Premium unlocks contact views, better response signals, and assisted conversation support.",
                            onUpgrade = openSubscription,
                            compact = true,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    if (messages.isEmpty()) {
                        item {
                            PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = SurfaceWarm) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Start gently", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Ask a specific question from their profile instead of sending a generic hello.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                            }
                        }
                    } else {
                        items(messages, key = { it.messageId.ifBlank { "${it.senderId}-${it.sentAt}-${it.content}" } }) { message ->
                            ChatBubble(
                                message = message,
                                isMine = message.senderId == currentUserId && message.flowStepId.isNullOrBlank()
                            )
                        }
                    }
                    item {
                        AssistPanel(prompts = prompts, onPrompt = { draft = it })
                    }
                }
            }
        }
    }
}

@Composable
private fun SafetyBanner() {
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = SurfaceWarm) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(16.dp), color = SuccessSoft, modifier = Modifier.size(46.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = Success)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Stay safe", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Keep personal contact details inside the app until both sides are comfortable.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, isMine: Boolean) {
    val isFlowBot = !message.flowStepId.isNullOrBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 292.dp),
            shape = RoundedCornerShape(
                topStart = 22.dp,
                topEnd = 22.dp,
                bottomStart = if (isMine) 22.dp else 6.dp,
                bottomEnd = if (isMine) 6.dp else 22.dp
            ),
            color = when {
                isMine -> MaterialTheme.colorScheme.primary
                isFlowBot -> InfoSoft
                else -> MaterialTheme.colorScheme.surface
            },
            border = if (isMine) null else BorderStroke(1.dp, Divider)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isFlowBot) {
                    Text(
                        message.messageUserAlias ?: message.alias ?: "SoulMatch Assist",
                        style = MaterialTheme.typography.labelMedium,
                        color = Info,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Text(
                    message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    formatChatTime(message.sentAt).ifBlank { message.status },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.76f) else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun AssistPanel(prompts: List<String>, onPrompt: (String) -> Unit) {
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Conversation assists", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Context-aware prompts for higher quality chats", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                SignalChip("Optional", tone = ChipTone.Info)
            }
            prompts.forEach { prompt ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onPrompt(prompt) },
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceSoft,
                    border = BorderStroke(1.dp, Divider)
                ) {
                    Text(
                        prompt,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryDark
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SignalChip("Family intro", tone = ChipTone.Warm)
                SignalChip("Values", tone = ChipTone.Success)
                SignalChip("Next step", tone = ChipTone.Neutral)
            }
        }
    }
}

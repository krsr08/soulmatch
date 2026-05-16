package com.soulmatch.app.ui.screens.interests

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.soulmatch.app.data.local.ProfileInteractionStore
import com.soulmatch.app.data.mock.MarketFixtures
import com.soulmatch.app.data.models.ProfileSummary
import com.soulmatch.app.data.local.ReportedConcern
import com.soulmatch.app.data.models.InterestListItem
import com.soulmatch.app.data.models.ShortlistItem
import com.soulmatch.app.data.models.ViewerData
import com.soulmatch.app.data.models.fullName
import com.soulmatch.app.ui.components.premium.ChipTone
import com.soulmatch.app.ui.components.media.MemberPhoto
import com.soulmatch.app.ui.components.premium.PremiumCard
import com.soulmatch.app.ui.components.premium.PremiumScreen
import com.soulmatch.app.ui.components.premium.SignalChip
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.Error
import com.soulmatch.app.ui.theme.ErrorSoft
import com.soulmatch.app.ui.theme.PrimaryDark
import com.soulmatch.app.ui.theme.Success
import com.soulmatch.app.ui.theme.SuccessSoft
import com.soulmatch.app.ui.theme.SurfaceSoft
import com.soulmatch.app.ui.theme.SurfaceWarm
import com.soulmatch.app.ui.theme.TextSecondary
import com.soulmatch.app.ui.formatDate
import com.soulmatch.app.ui.formatDateMillis
import com.soulmatch.app.ui.titleCase
import com.soulmatch.app.ui.viewmodels.InterestsViewModel

private data class InterestActivityItem(
    val item: InterestListItem,
    val directionLabel: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterestsScreen(
    onBack: () -> Unit = {},
    onViewProfile: ((String) -> Unit)? = null,
    onOpenChat: ((String, String) -> Unit)? = null,
    onSubscribe: (() -> Unit)? = null,
    onEditSection: ((Int) -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    profileId: String = "",
    chatId: String = "",
    participantName: String = "",
    initialTab: String = "",
    vm: InterestsViewModel = hiltViewModel()
) {
    val received by vm.received.collectAsStateWithLifecycle()
    val sent by vm.sent.collectAsStateWithLifecycle()
    val shortlisted by vm.shortlisted.collectAsStateWithLifecycle()
    val viewers by vm.viewers.collectAsStateWithLifecycle()
    val loading by vm.isLoading.collectAsStateWithLifecycle()
    val interactions by ProfileInteractionStore.state.collectAsStateWithLifecycle()
    var tab by remember(initialTab) { mutableIntStateOf(activityTabIndex(initialTab)) }
    val viewProfile: (String) -> Unit = onViewProfile ?: { _ -> }
    val openChat: (String, String) -> Unit = onOpenChat ?: { _, _ -> }
    val accepted = remember(received, sent) { buildInterestBucket(received, sent, "accepted") }
    val declined = remember(received, sent) { buildInterestBucket(received, sent, "declined") }
    val tabs = listOf("Received", "Sent", "Accepted", "Declined", "Shortlist", "Visitors", "Hidden", "Blocked", "Reported")
    val hiddenProfiles = remember(interactions.hiddenProfileIds) {
        interactions.hiddenProfileIds.mapNotNull { MarketFixtures.matchSeed(it) }
    }
    val blockedProfiles = remember(interactions.blockedProfileIds) {
        interactions.blockedProfileIds.mapNotNull { MarketFixtures.matchSeed(it) }
    }
    val reportedConcerns = remember(interactions.reportedConcerns) {
        interactions.reportedConcerns.values.sortedByDescending { it.updatedMillis }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity", fontWeight = FontWeight.Bold) },
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
            Column(Modifier.fillMaxSize()) {
                ActivitySummary(
                    received = received.count { it.status.equals("pending", true) },
                    sent = sent.size,
                    accepted = accepted.size,
                    shortlisted = shortlisted.size,
                    visitors = viewers.size,
                    onOpenTab = { tab = it }
                )
                ScrollableTabRow(selectedTabIndex = tab, edgePadding = 16.dp, containerColor = MaterialTheme.colorScheme.background) {
                    tabs.forEachIndexed { index, label ->
                        Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
                    }
                }
                if (loading && received.isEmpty() && sent.isEmpty() && shortlisted.isEmpty() && viewers.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        when (tab) {
                            0 -> {
                                val pending = received.filter { it.status.equals("pending", ignoreCase = true) }
                                if (pending.isEmpty()) {
                                    item { EmptyStateCard("No pending received interests right now.", "When someone sends interest, you can accept, decline, or open their full profile here.") }
                                } else {
                                    items(pending, key = { it.interestId }) { item ->
                                        InterestRow(
                                            title = item.fullName(),
                                            subtitle = "Received ${formatDate(item.sentAt)}",
                                            status = "Waiting",
                                            photoUrl = item.primaryPhotoUrl,
                                            primaryLabel = "Accept",
                                            secondaryLabel = "Decline",
                                            onPrimary = { vm.respond(item.interestId, "accepted") },
                                            onSecondary = { vm.respond(item.interestId, "declined") },
                                            onOpen = { viewProfile(item.profileId) }
                                        )
                                    }
                                }
                            }
                            1 -> {
                                if (sent.isEmpty()) {
                                    item { EmptyStateCard("No sent interests yet.", "Send interest from match cards to start a high-intent conversation path.") }
                                } else {
                                    items(sent, key = { it.interestId }) { item ->
                                        InterestRow(
                                            title = item.fullName(),
                                            subtitle = "Sent ${formatDate(item.sentAt)}",
                                            status = titleCase(item.status),
                                            photoUrl = item.primaryPhotoUrl,
                                            primaryLabel = null,
                                            secondaryLabel = null,
                                            onPrimary = null,
                                            onSecondary = null,
                                            onOpen = { viewProfile(item.profileId) }
                                        )
                                    }
                                }
                            }
                            2 -> {
                                if (accepted.isEmpty()) {
                                    item { EmptyStateCard("No accepted interests yet.", "Accepted interests will become your warmest profiles and can unlock chat.") }
                                } else {
                                    items(accepted, key = { "${it.directionLabel}-${it.item.interestId}" }) { activity ->
                                        InterestRow(
                                            title = activity.item.fullName(),
                                            subtitle = "${activity.directionLabel} ${formatDate(activity.item.sentAt)} | Accepted",
                                            status = "Mutual path ready",
                                            photoUrl = activity.item.primaryPhotoUrl,
                                            primaryLabel = "Chat",
                                            secondaryLabel = null,
                                            onPrimary = { openChat(activity.item.userId, activity.item.fullName()) },
                                            onSecondary = null,
                                            onOpen = { viewProfile(activity.item.profileId) }
                                        )
                                    }
                                }
                            }
                            3 -> {
                                if (declined.isEmpty()) {
                                    item { EmptyStateCard("No declined interests.", "Declined requests will remain visible here for transparency.") }
                                } else {
                                    items(declined, key = { "${it.directionLabel}-${it.item.interestId}" }) { activity ->
                                        InterestRow(
                                            title = activity.item.fullName(),
                                            subtitle = "${activity.directionLabel} ${formatDate(activity.item.sentAt)} | Declined",
                                            status = "Closed",
                                            photoUrl = activity.item.primaryPhotoUrl,
                                            primaryLabel = null,
                                            secondaryLabel = null,
                                            onPrimary = null,
                                            onSecondary = null,
                                            onOpen = { viewProfile(activity.item.profileId) }
                                        )
                                    }
                                }
                            }
                            4 -> {
                                if (shortlisted.isEmpty()) {
                                    item { EmptyStateCard("No shortlisted profiles yet.", "Shortlisted profiles give families a simple follow-up list.") }
                                } else {
                                    items(shortlisted, key = { it.profileId }) { item ->
                                        ShortlistRow(item = item, onOpen = { viewProfile(item.profileId) })
                                    }
                                }
                            }
                            5 -> {
                                if (viewers.isEmpty()) {
                                    item { EmptyStateCard("No recent visitors yet.", "Recent visitors will help you spot members already showing intent.") }
                                } else {
                                    items(viewers, key = { "${it.profileId}-${it.viewedAt}" }) { viewer ->
                                        ViewerRow(viewer = viewer, onOpen = { viewProfile(viewer.profileId) })
                                    }
                                }
                            }
                            6 -> item {
                                if (hiddenProfiles.isEmpty()) {
                                    EmptyStateCard("No hidden members.", "When you hide a member from a profile card, they will appear here.")
                                } else {
                                    Column {
                                        hiddenProfiles.forEach { profile ->
                                            ManagementProfileRow(
                                                profile = profile,
                                                icon = Icons.Filled.Visibility,
                                                label = "Hidden",
                                                dateLabel = "Profile added ${formatDate(profile.createdAt)}"
                                            )
                                        }
                                    }
                                }
                            }
                            7 -> item {
                                if (blockedProfiles.isEmpty()) {
                                    EmptyStateCard("No blocked members.", "Blocked members will be listed here.")
                                } else {
                                    Column {
                                        blockedProfiles.forEach { profile ->
                                            ManagementProfileRow(
                                                profile = profile,
                                                icon = Icons.Filled.Block,
                                                label = "Blocked",
                                                dateLabel = "Profile added ${formatDate(profile.createdAt)}",
                                                danger = true
                                            )
                                        }
                                    }
                                }
                            }
                            else -> item {
                                if (reportedConcerns.isEmpty()) {
                                    EmptyStateCard("No reported concerns.", "Reports you save from profile cards are visible in this tab and editable from Privacy.")
                                } else {
                                    Column {
                                        reportedConcerns.forEach { concern ->
                                            ReportedActivityRow(concern = concern)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun activityTabIndex(tab: String): Int {
    return when (tab.trim().lowercase()) {
        "received" -> 0
        "sent" -> 1
        "accepted" -> 2
        "declined" -> 3
        "shortlist", "shortlisted" -> 4
        "visitors", "viewers" -> 5
        "hidden", "hidden_members" -> 6
        "blocked", "blocked_members" -> 7
        "reported", "reports", "reported_members" -> 8
        else -> 0
    }
}

@Composable
private fun ActivitySummary(
    received: Int,
    sent: Int,
    accepted: Int,
    shortlisted: Int,
    visitors: Int,
    onOpenTab: (Int) -> Unit
) {
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), containerColor = MaterialTheme.colorScheme.surface) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ActivityStatTile("Received", received.toString(), modifier = Modifier.weight(1f), background = SurfaceWarm, onClick = { onOpenTab(0) })
                ActivityStatTile("Sent interests", sent.toString(), modifier = Modifier.weight(1f), background = SurfaceSoft, onClick = { onOpenTab(1) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ActivityStatTile("Accepted", accepted.toString(), modifier = Modifier.weight(1f), content = Success, background = SuccessSoft, onClick = { onOpenTab(2) })
                ActivityStatTile("Shortlist", shortlisted.toString(), modifier = Modifier.weight(1f), background = SurfaceWarm, onClick = { onOpenTab(4) })
                ActivityStatTile("Visitors", visitors.toString(), modifier = Modifier.weight(1f), background = SurfaceSoft, onClick = { onOpenTab(5) })
            }
        }
    }
}

@Composable
private fun ActivityStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    content: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    background: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(76.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = background,
        border = BorderStroke(1.dp, Divider)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = content)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun InterestRow(
    title: String,
    subtitle: String,
    status: String,
    photoUrl: String?,
    primaryLabel: String?,
    secondaryLabel: String?,
    onPrimary: (() -> Unit)?,
    onSecondary: (() -> Unit)?,
    onOpen: () -> Unit
) {
    PremiumCard(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onOpen),
        contentPadding = PaddingValues(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            MemberPhoto(
                photoUrl = photoUrl,
                contentDescription = title,
                modifier = Modifier
                    .size(52.dp),
                shape = RoundedCornerShape(12.dp)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    SignalChip(status, tone = ChipTone.Warm)
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                if (primaryLabel != null || secondaryLabel != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (primaryLabel != null && onPrimary != null) {
                            Button(onClick = onPrimary, modifier = Modifier.weight(1f).height(34.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                Icon(
                                    if (primaryLabel == "Chat") Icons.Filled.Chat else Icons.Filled.Favorite,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.size(5.dp))
                                Text(primaryLabel, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        if (secondaryLabel != null && onSecondary != null) {
                            OutlinedButton(onClick = onSecondary, modifier = Modifier.weight(1f).height(34.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                Text(secondaryLabel, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortlistRow(item: ShortlistItem, onOpen: () -> Unit) {
    PremiumCard(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onOpen),
        contentPadding = PaddingValues(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            MemberPhoto(
                photoUrl = item.primaryPhotoUrl,
                contentDescription = item.fullName(),
                modifier = Modifier
                    .size(52.dp),
                shape = RoundedCornerShape(12.dp)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.fullName(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    SignalChip("Shortlist", tone = ChipTone.Info)
                }
                Text("Shortlisted ${formatDate(item.addedAt)} for family follow-up", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun ViewerRow(viewer: ViewerData, onOpen: () -> Unit) {
    PremiumCard(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onOpen),
        contentPadding = PaddingValues(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            MemberPhoto(
                photoUrl = viewer.primaryPhotoUrl,
                contentDescription = viewer.fullName(),
                modifier = Modifier
                    .size(52.dp),
                shape = RoundedCornerShape(12.dp)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        viewer.fullName(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    SignalChip("Visitor", tone = ChipTone.Info)
                }
                Text("Viewed ${formatDate(viewer.viewedAt)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun EmptyStateCard(message: String, detail: String) {
    PremiumCard(modifier = Modifier.padding(16.dp), containerColor = SurfaceWarm) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun ManagementProfileRow(
    profile: ProfileSummary,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    dateLabel: String,
    danger: Boolean = false
) {
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), containerColor = if (danger) ErrorSoft else SurfaceWarm, contentPadding = PaddingValues(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, Divider), modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = if (danger) Error else PrimaryDark)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        profile.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    SignalChip(label, tone = if (danger) ChipTone.Warm else ChipTone.Info)
                }
                Text("${profile.location} | ${profile.community}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(dateLabel, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun ReportedActivityRow(concern: ReportedConcern) {
    val profile = MarketFixtures.matchSeed(concern.profileId)
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), containerColor = SurfaceWarm, contentPadding = PaddingValues(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, Divider), modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Report, contentDescription = null, tint = PrimaryDark)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        profile?.name ?: "Reported member",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    SignalChip("Reported", tone = ChipTone.Warm)
                }
                Text(profile?.let { "${it.location} | ${it.community}" } ?: concern.profileId, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("Reported ${formatDateMillis(concern.updatedMillis)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(concern.concern, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun buildInterestBucket(
    received: List<InterestListItem>,
    sent: List<InterestListItem>,
    status: String
): List<InterestActivityItem> {
    val normalized = status.lowercase()
    val receivedItems = received
        .filter { it.status.equals(normalized, ignoreCase = true) }
        .map { InterestActivityItem(it, "Received") }
    val sentItems = sent
        .filter { it.status.equals(normalized, ignoreCase = true) }
        .map { InterestActivityItem(it, "Sent") }
    return receivedItems + sentItems
}

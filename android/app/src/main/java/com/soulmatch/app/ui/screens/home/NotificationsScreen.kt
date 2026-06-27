package com.soulmatch.app.ui.screens.home

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.data.models.NotificationData
import com.soulmatch.app.data.models.fullName
import com.soulmatch.app.ui.components.premium.PremiumCard
import com.soulmatch.app.ui.components.premium.PremiumScreen
import com.soulmatch.app.ui.components.status.ProfileStrengthAdvisor
import com.soulmatch.app.ui.design.SoulMatchTokens
import com.soulmatch.app.ui.viewmodels.DashboardViewModel
import com.soulmatch.app.ui.viewmodels.NotificationsViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

private val NotificationsBackground = SoulMatchTokens.Bg
private val NotificationsSurface = SoulMatchTokens.Card
private val NotificationsUnread = SoulMatchTokens.TangerineSoft
private val NotificationsPrimary = SoulMatchTokens.Tangerine
private val NotificationsText = SoulMatchTokens.Text
private val NotificationsMuted = SoulMatchTokens.Muted
private val NotificationsBorder = SoulMatchTokens.GoldSoft
private val NotificationsGreyIcon = SoulMatchTokens.Muted
private val NotificationsSoftIcon = SoulMatchTokens.Ivory

private enum class NotificationSection(val title: String) {
    Today("TODAY"),
    Yesterday("YESTERDAY"),
    Earlier("EARLIER")
}

private data class NotificationUiItem(
    val key: String,
    val section: NotificationSection,
    val title: String,
    val body: String,
    val action: String,
    val icon: ImageVector,
    val iconTint: Color,
    val iconBackground: Color,
    val isUnread: Boolean,
    val onClick: () -> Unit
)

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenBestMatches: () -> Unit,
    vm: DashboardViewModel = hiltViewModel(),
    notificationsVm: NotificationsViewModel = hiltViewModel()
) {
    val myProfile by vm.myProfile.collectAsStateWithLifecycle()
    val matches by vm.matches.collectAsStateWithLifecycle()
    val pendingInvitations by vm.pendingInvitations.collectAsStateWithLifecycle()
    val notifications by notificationsVm.notifications.collectAsStateWithLifecycle()
    val notificationError by notificationsVm.error.collectAsStateWithLifecycle()
    val strongMatches = matches.count { it.compatibilityScore >= 90 }
    val hasUnreadNotifications = notifications.any { !it.isRead() }

    val timelineItems = buildList {
        notifications.forEachIndexed { index, notification ->
            val type = notification.data["type"].orEmpty()
            add(
                NotificationUiItem(
                    key = "notification-${notification.notificationId.ifBlank { "${notification.createdAt}-$index" }}",
                    section = notificationSection(notification),
                    title = notification.title.ifBlank { notificationFallbackTitle(type) },
                    body = notification.body.ifBlank { notificationFallbackBody(type) },
                    action = notificationActionLabel(notification),
                    icon = notificationIcon(type),
                    iconTint = if (notification.isRead()) NotificationsGreyIcon else NotificationsPrimary,
                    iconBackground = if (notification.isRead()) NotificationsSoftIcon else Color.White,
                    isUnread = !notification.isRead(),
                    onClick = {
                        notificationsVm.markRead(notification.notificationId)
                        when (type) {
                            "interest_received",
                            "interest_accepted",
                            "interest_declined",
                            "photo_access_requested",
                            "photo_access_approved",
                            "photo_access_declined" -> onOpenActivity()
                            "profile_strength", "verification_approved", "verification_rejected" -> onOpenProfile()
                            else -> onOpenBestMatches()
                        }
                    }
                )
            )
        }
        val profileStrengthScore = ProfileStrengthAdvisor.score(myProfile)
        if (myProfile.profileId.isNotBlank() && profileStrengthScore < 100) {
            add(
                NotificationUiItem(
                    key = "profile-strength",
                    section = NotificationSection.Yesterday,
                    title = "Profile strength ${profileStrengthScore.coerceIn(0, 100)}%",
                    body = "Next: ${ProfileStrengthAdvisor.summary(myProfile)}",
                    action = "UPDATE",
                    icon = Icons.Filled.Person,
                    iconTint = NotificationsGreyIcon,
                    iconBackground = NotificationsSoftIcon,
                    isUnread = false,
                    onClick = onOpenProfile
                )
            )
        }
        if (pendingInvitations.isNotEmpty()) {
            add(
                NotificationUiItem(
                    key = "pending-invitations",
                    section = NotificationSection.Yesterday,
                    title = "${pendingInvitations.size} invitation${if (pendingInvitations.size == 1) "" else "s"} waiting",
                    body = "Accept, decline, or review new requests from Activity.",
                    action = "OPEN ACTIVITY",
                    icon = Icons.Filled.Favorite,
                    iconTint = NotificationsPrimary,
                    iconBackground = NotificationsSoftIcon,
                    isUnread = false,
                    onClick = onOpenActivity
                )
            )
        }
        add(
            NotificationUiItem(
                key = "high-matches",
                section = NotificationSection.Earlier,
                title = "$strongMatches high-match profile${if (strongMatches == 1) "" else "s"}",
                body = "Best Matches are ranked by compatibility and recent activity.",
                action = "VIEW MATCHES",
                icon = Icons.Filled.Star,
                iconTint = NotificationsPrimary,
                iconBackground = NotificationsSoftIcon,
                isUnread = false,
                onClick = onOpenBestMatches
            )
        )
        pendingInvitations.take(5).forEach { invitation ->
            add(
                NotificationUiItem(
                    key = "invitation-${invitation.interestId}",
                    section = NotificationSection.Earlier,
                    title = "${invitation.firstName.ifBlank { invitation.fullName() }} sent an invitation",
                    body = "Tap to respond from Activity.",
                    action = "REVIEW",
                    icon = Icons.Filled.Notifications,
                    iconTint = NotificationsGreyIcon,
                    iconBackground = NotificationsSoftIcon,
                    isUnread = false,
                    onClick = onOpenActivity
                )
            )
        }
    }

    Scaffold(
        topBar = {
            NotificationsTopBar(
                canMarkAllRead = hasUnreadNotifications,
                onBack = onBack,
                onMarkAllRead = { notificationsVm.markAllRead() }
            )
        },
        containerColor = NotificationsBackground
    ) { padding ->
        PremiumScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NotificationSection.entries.forEach { section ->
                    val sectionItems = timelineItems.filter { it.section == section }
                    if (sectionItems.isNotEmpty()) {
                        item(key = "section-${section.name}") {
                            SectionHeader(section.title)
                        }
                        items(sectionItems, key = { it.key }) { item ->
                            NotificationCard(item = item)
                        }
                        item(key = "section-space-${section.name}") {
                            Spacer(Modifier.height(14.dp))
                        }
                    }
                }
                if (!notificationError.isNullOrBlank()) {
                    item {
                        PremiumCard(containerColor = NotificationsUnread) {
                            Text("Notification sync issue", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(notificationError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = SoulMatchTokens.Muted)
                        }
                    }
                }
                if (timelineItems.isEmpty()) {
                    item {
                        PremiumCard(containerColor = NotificationsSurface) {
                            Text("No updates yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Profile alerts, invitations, and best-match updates will appear here.", style = MaterialTheme.typography.bodyMedium, color = SoulMatchTokens.Muted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationsTopBar(
    canMarkAllRead: Boolean,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit
) {
    Surface(
        color = NotificationsBackground,
        border = BorderStroke(1.dp, SoulMatchTokens.Border.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = SoulMatchTokens.Tangerine, modifier = Modifier.size(31.dp))
            }
            Text(
                "Notifications",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 30.sp
                ),
                color = NotificationsText
            )
            if (canMarkAllRead) {
                TextButton(onClick = onMarkAllRead) {
                    Text(
                        "Mark all read",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = NotificationsPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 6.dp),
        style = MaterialTheme.typography.labelLarge.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp
        ),
        color = SoulMatchTokens.Muted
    )
}

@Composable
private fun NotificationCard(item: NotificationUiItem) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (item.isUnread) NotificationsUnread else NotificationsSurface,
        border = BorderStroke(1.dp, if (item.isUnread) NotificationsBorder else SoulMatchTokens.Border.copy(alpha = 0.62f)),
        shadowElevation = if (item.isUnread) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = item.iconBackground,
                shadowElevation = if (item.isUnread) 1.dp else 0.dp,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = item.iconTint,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 21.sp
                    ),
                    color = NotificationsText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.body,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Serif,
                        lineHeight = 20.sp
                    ),
                    color = NotificationsMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                item.action.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = NotificationsPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun NotificationData.isRead(): Boolean {
    return status.equals("read", ignoreCase = true) || !readAt.isNullOrBlank()
}

private fun notificationSection(notification: NotificationData): NotificationSection {
    val createdDate = parseNotificationDate(notification.createdAt) ?: return if (notification.isRead()) {
        NotificationSection.Yesterday
    } else {
        NotificationSection.Today
    }
    val today = LocalDate.now(ZoneId.systemDefault())
    return when {
        createdDate == today -> NotificationSection.Today
        createdDate == today.minusDays(1) -> NotificationSection.Yesterday
        else -> NotificationSection.Earlier
    }
}

private fun parseNotificationDate(value: String): LocalDate? {
    if (value.isBlank()) return null
    return runCatching {
        Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDate()
    }.getOrNull()
        ?: runCatching { LocalDate.parse(value.take(10)) }.getOrNull()
}

private fun notificationIcon(type: String): ImageVector {
    return when (type) {
        "interest_received", "interest_accepted", "interest_declined",
        "photo_access_requested", "photo_access_approved", "photo_access_declined" -> Icons.Filled.Favorite
        "profile_strength", "verification_approved", "verification_rejected" -> Icons.Filled.Person
        "match_recommendation", "best_match" -> Icons.Filled.Star
        else -> Icons.Filled.Notifications
    }
}

private fun notificationActionLabel(notification: NotificationData): String {
    if (!notification.isRead()) return "New"
    return when (notification.data["type"].orEmpty()) {
        "profile_strength", "verification_rejected" -> "Update"
        "interest_received", "interest_accepted", "interest_declined",
        "photo_access_requested", "photo_access_approved", "photo_access_declined" -> "Open"
        "match_recommendation", "best_match" -> "View matches"
        else -> "Open"
    }
}

private fun notificationFallbackTitle(type: String): String {
    return when (type) {
        "interest_received" -> "Someone liked you"
        "interest_accepted" -> "Interest accepted"
        "interest_declined" -> "Interest update"
        "photo_access_requested" -> "Photo request"
        "photo_access_approved" -> "Photo access approved"
        "photo_access_declined" -> "Photo access update"
        "verification_approved" -> "Profile verified"
        "verification_rejected" -> "Verification update"
        "profile_strength" -> "Profile strength update"
        "match_recommendation", "best_match" -> "New high-match profiles"
        else -> "SoulMatch update"
    }
}

private fun notificationFallbackBody(type: String): String {
    return when (type) {
        "interest_received" -> "A member liked your profile. Open SoulMatch to respond."
        "interest_accepted" -> "Your interest was accepted. You can continue from Activity."
        "interest_declined" -> "A member declined your interest. Keep exploring compatible matches."
        "photo_access_requested" -> "A member requested access to view your photos."
        "photo_access_approved" -> "Your photo access request was approved."
        "photo_access_declined" -> "Your photo access request was declined."
        "verification_approved" -> "Your profile verification has been approved."
        "verification_rejected" -> "Your verification needs an update."
        "profile_strength" -> "Review profile details to improve match quality."
        "match_recommendation", "best_match" -> "Review compatible profiles selected for you."
        else -> "Open SoulMatch to review this update."
    }
}

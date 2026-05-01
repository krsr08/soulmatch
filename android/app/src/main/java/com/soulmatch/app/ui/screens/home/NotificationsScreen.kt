package com.soulmatch.app.ui.screens.home

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.data.models.fullName
import com.soulmatch.app.ui.components.PremiumCard
import com.soulmatch.app.ui.components.PremiumScreen
import com.soulmatch.app.ui.components.ProfileStrengthAdvisor
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.PrimaryDark
import com.soulmatch.app.ui.theme.SurfaceSoft
import com.soulmatch.app.ui.theme.SurfaceWarm
import com.soulmatch.app.ui.theme.TextSecondary
import com.soulmatch.app.ui.viewmodels.DashboardViewModel
import com.soulmatch.app.ui.viewmodels.NotificationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (notifications.any { it.status != "read" }) {
                        TextButton(onClick = { notificationsVm.markAllRead() }) {
                            Text("Mark all read")
                        }
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (notifications.isNotEmpty()) {
                    items(notifications, key = { it.notificationId }) { notification ->
                        val type = notification.data["type"].orEmpty()
                        NotificationCard(
                            title = notification.title,
                            body = notification.body,
                            action = if (notification.status == "read") "Open" else "New",
                            icon = {
                                Icon(
                                    imageVector = when (type) {
                                        "interest_received", "interest_accepted", "interest_declined" -> Icons.Filled.Favorite
                                        else -> Icons.Filled.Notifications
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            containerColor = if (notification.status == "read") MaterialTheme.colorScheme.surface else SurfaceWarm,
                            onClick = {
                                notificationsVm.markRead(notification.notificationId)
                                when (type) {
                                    "interest_received", "interest_accepted", "interest_declined" -> onOpenActivity()
                                    else -> onOpenBestMatches()
                                }
                            }
                        )
                    }
                }
                if (!notificationError.isNullOrBlank()) {
                    item {
                        PremiumCard(containerColor = SurfaceWarm) {
                            Text("Notification sync issue", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(notificationError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
                item {
                    NotificationCard(
                        title = "Profile strength ${myProfile.completionScore.coerceIn(0, 100)}%",
                        body = ProfileStrengthAdvisor.summary(myProfile),
                        action = "Update profile",
                        icon = {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = onOpenProfile
                    )
                }
                if (pendingInvitations.isNotEmpty()) {
                    item {
                        NotificationCard(
                            title = "${pendingInvitations.size} invitation${if (pendingInvitations.size == 1) "" else "s"} waiting",
                            body = "Accept, decline, or review new requests from Activity.",
                            action = "Open activity",
                            icon = {
                                Icon(Icons.Filled.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            onClick = onOpenActivity
                        )
                    }
                }
                item {
                    NotificationCard(
                        title = "$strongMatches high-match profile${if (strongMatches == 1) "" else "s"}",
                        body = "Best Matches are ranked by compatibility and recent activity.",
                        action = "View matches",
                        icon = {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = onOpenBestMatches
                    )
                }
                items(pendingInvitations.take(5), key = { it.interestId }) { invitation ->
                    NotificationCard(
                        title = "${invitation.firstName.ifBlank { invitation.fullName() }} sent an invitation",
                        body = "Tap to respond from Activity.",
                        action = "Review",
                        icon = {
                            Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = onOpenActivity
                    )
                }
                if (pendingInvitations.isEmpty() && strongMatches == 0) {
                    item {
                        PremiumCard(containerColor = SurfaceWarm) {
                            Text("No urgent updates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Profile alerts, invitations, and best-match updates will appear here.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    title: String,
    body: String,
    action: String,
    icon: @Composable () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, Divider.copy(alpha = 0.68f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = SurfaceSoft, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    icon()
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(body, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(action, style = MaterialTheme.typography.labelMedium, color = PrimaryDark, fontWeight = FontWeight.Bold)
        }
    }
}

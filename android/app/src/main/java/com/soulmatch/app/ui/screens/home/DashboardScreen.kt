package com.soulmatch.app.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.data.models.HomeContentData
import com.soulmatch.app.data.models.InterestListItem
import com.soulmatch.app.data.models.ProfileSummary
import com.soulmatch.app.data.models.fullName
import com.soulmatch.app.ui.components.MemberPhoto
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

private val HomeBackground = Color(0xFFFFF9F2)
private val HomePrimary = Color(0xFFD12E5E)
private val SoftBorder = Color(0xFFE1BEC2)

@Composable
fun DashboardScreen(
    content: HomeContentData = HomeContentData(),
    onViewProfile: (String) -> Unit,
    onOpenChat: (String, String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenInterests: () -> Unit,
    onOpenBestMatches: () -> Unit = onOpenSearch,
    onOpenNotifications: () -> Unit = onOpenMessages,
    onOpenProfile: () -> Unit,
    onOpenSubscription: () -> Unit = {},
    vm: DashboardViewModel = hiltViewModel(),
    notificationsVm: NotificationsViewModel = hiltViewModel()
) {
    val matches by vm.matches.collectAsStateWithLifecycle()
    val pendingInvitations by vm.pendingInvitations.collectAsStateWithLifecycle()
    val myProfile by vm.myProfile.collectAsStateWithLifecycle()
    val loading by vm.isLoading.collectAsStateWithLifecycle()
    val notifications by notificationsVm.notifications.collectAsStateWithLifecycle()
    val unreadNotificationCount = notifications.count {
        it.readAt.isNullOrBlank() && !it.status.equals("read", ignoreCase = true)
    }
    val rankedMatches = matches.sortedWith(
        compareByDescending<ProfileSummary> { it.compatibilityScore }
            .thenBy { it.name }
    )
    val bestMatches = rankedMatches.take(2)
    val newProfiles = rankedMatches.drop(2).take(8).ifEmpty { rankedMatches.take(6) }

    Scaffold(
        topBar = {
            HomeTopBar(
                profilePhoto = myProfile.primaryPhotoUrl,
                unreadCount = unreadNotificationCount,
                onOpenProfile = onOpenProfile,
                onOpenNotifications = onOpenNotifications
            )
        },
        containerColor = HomeBackground
    ) { padding ->
        PremiumScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(HomeBackground)
        ) {
            if (loading && matches.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = HomePrimary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        WelcomeSection(firstName = myProfile.firstName.ifBlank { "Aarav" })
                    }
                    item {
                        ProfileStrengthCard(
                            score = myProfile.completionScore.coerceIn(0, 100),
                            detail = ProfileStrengthAdvisor.summary(myProfile),
                            onClick = onOpenProfile
                        )
                    }
                    item {
                        HomeSectionHeader(
                            title = content.bestMatchesTitle.ifBlank { "Best Matches" }.let {
                                if (it.equals("Best matches", ignoreCase = true)) "Best Matches" else it
                            },
                            modifier = Modifier.padding(top = 26.dp),
                            actionText = "View All",
                            onAction = onOpenBestMatches
                        )
                    }
                    if (bestMatches.isEmpty()) {
                        item {
                            EmptyHomeCard(
                                title = content.emptyTitle.ifBlank { "Your profile needs a little more detail" },
                                body = content.emptyBody.ifBlank { "Complete family, lifestyle, and preference details to unlock stronger recommendations." },
                                action = content.emptyCta.ifBlank { "Improve my profile" },
                                onAction = onOpenProfile
                            )
                        }
                    } else {
                        items(bestMatches, key = { it.profileId }) { profile ->
                            HomeMatchCard(
                                profile = profile,
                                onOpen = { onViewProfile(profile.profileId) },
                                onInterest = { vm.sendInterest(profile.profileId) },
                                onShortlist = { vm.toggleShortlist(profile.profileId) }
                            )
                        }
                    }
                    item {
                        HomeSectionHeader(
                            title = "New Profiles",
                            modifier = Modifier.padding(top = 24.dp),
                            actionText = "View All",
                            onAction = onOpenSearch
                        )
                    }
                    item {
                        NewProfilesCarousel(
                            profiles = newProfiles,
                            onOpenProfile = onViewProfile
                        )
                    }
                    item {
                        HomeSectionHeader(
                            title = "Pending Invitations (${pendingInvitations.size})",
                            modifier = Modifier.padding(top = 24.dp),
                            actionText = "Activity",
                            onAction = onOpenInterests
                        )
                    }
                    if (pendingInvitations.isEmpty()) {
                        item {
                            EmptyHomeCard(
                                title = "No pending invitations",
                                body = "New invitations will appear here with quick accept and decline actions.",
                                action = "Browse matches",
                                onAction = onOpenSearch
                            )
                        }
                    } else {
                        items(pendingInvitations.take(2), key = { it.interestId }) { invitation ->
                            PendingInvitationCard(
                                invitation = invitation,
                                onOpen = { onViewProfile(invitation.profileId) },
                                onAccept = { vm.respondToInvitation(invitation.interestId, "accepted") },
                                onDecline = { vm.respondToInvitation(invitation.interestId, "declined") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    profilePhoto: String?,
    unreadCount: Int,
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    Surface(
        color = HomeBackground,
        border = BorderStroke(1.dp, HomePrimary.copy(alpha = 0.10f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onOpenProfile),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(2.dp, HomePrimary),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    MemberPhoto(
                        photoUrl = profilePhoto,
                        contentDescription = "Open profile",
                        modifier = Modifier
                            .fillMaxSize(),
                        shape = RoundedCornerShape(999.dp)
                    )
                }
                Text(
                    "SoulMatch",
                    style = MaterialTheme.typography.titleLarge,
                    color = HomePrimary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            IconButton(onClick = onOpenNotifications) {
                Box(
                    modifier = Modifier.size(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = TextSecondary)
                    if (unreadCount > 0) {
                        val badgeText = if (unreadCount > 99) "99+" else unreadCount.toString()
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .defaultMinSize(minWidth = 16.dp)
                                .height(16.dp),
                            shape = RoundedCornerShape(999.dp),
                            color = HomePrimary,
                            border = BorderStroke(1.dp, HomeBackground)
                        ) {
                            Text(
                                badgeText,
                                modifier = Modifier.padding(horizontal = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeSection(firstName: String) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "Hello, $firstName",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Your journey to a meaningful connection continues.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun ProfileStrengthCard(score: Int, detail: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, HomePrimary.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Profile Strength", style = MaterialTheme.typography.labelLarge, color = PrimaryDark)
                Text("$score%", style = MaterialTheme.typography.labelLarge, color = HomePrimary, fontWeight = FontWeight.ExtraBold)
            }
            LinearProgressIndicator(
                progress = score / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = HomePrimary,
                trackColor = SurfaceSoft
            )
            Text(detail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        when {
            trailing != null -> trailing()
            actionText != null && onAction != null -> {
                Text(
                    actionText,
                    modifier = Modifier.clickable(onClick = onAction),
                    style = MaterialTheme.typography.labelLarge,
                    color = HomePrimary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeMatchCard(
    profile: ProfileSummary,
    onOpen: () -> Unit,
    onInterest: () -> Unit,
    onShortlist: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Divider.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)) {
                MemberPhoto(
                    photoUrl = profile.primaryPhoto,
                    contentDescription = "Photo of ${profile.name}",
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(0.dp)
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoundImageAction(
                        selected = profile.interestSent,
                        selectedContentDescription = "Interest sent",
                        unselectedContentDescription = "Send interest",
                        onClick = onInterest
                    ) {
                        Icon(
                            imageVector = if (profile.interestSent) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = HomePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    RoundImageAction(
                        selected = profile.shortlisted,
                        selectedContentDescription = "Shortlisted",
                        unselectedContentDescription = "Save profile",
                        onClick = onShortlist
                    ) {
                        Icon(
                            imageVector = if (profile.shortlisted) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = if (profile.shortlisted) HomePrimary else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = HomePrimary.copy(alpha = 0.92f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Text("${profile.compatibilityScore}% Match", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Column(
                Modifier
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${profile.name.removeSuffix(".")}, ${profile.age}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (profile.isVerified) {
                        Spacer(Modifier.width(5.dp))
                        Icon(Icons.Filled.Verified, contentDescription = "Verified", tint = HomePrimary, modifier = Modifier.size(17.dp))
                    }
                }
                Text(
                    "${profile.occupation.ifBlank { "Profession not added" }} • ${profile.location.ifBlank { "Location not added" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    profile.matchReasons.take(3).ifEmpty { listOf(profile.education, profile.community) }
                        .filter { it.isNotBlank() }
                        .forEach { label -> HomeTag(label) }
                }
            }
        }
    }
}

@Composable
private fun RoundImageAction(
    selected: Boolean,
    selectedContentDescription: String,
    unselectedContentDescription: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(40.dp)
            .semantics {
                contentDescription = if (selected) selectedContentDescription else unselectedContentDescription
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, Divider.copy(alpha = 0.6f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
private fun HomeTag(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFFFAFAFA),
        border = BorderStroke(1.dp, Divider.copy(alpha = 0.72f))
    ) {
        Text(
            label.uppercase(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NewProfilesCarousel(
    profiles: List<ProfileSummary>,
    onOpenProfile: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(profiles, key = { it.profileId }) { profile ->
            NewProfileTile(profile = profile, onClick = { onOpenProfile(profile.profileId) })
        }
    }
}

@Composable
private fun NewProfileTile(profile: ProfileSummary, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(102.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MemberPhoto(
            photoUrl = profile.primaryPhoto,
            contentDescription = profile.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "${profile.name.removeSuffix(".")}, ${profile.age}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            profile.location,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PendingInvitationCard(
    invitation: InterestListItem,
    onOpen: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Divider.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MemberPhoto(
                photoUrl = invitation.primaryPhotoUrl,
                contentDescription = invitation.fullName(),
                modifier = Modifier
                    .size(52.dp),
                shape = RoundedCornerShape(999.dp)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${invitation.firstName.ifBlank { invitation.fullName() }} sent an invitation",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text("Recently", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                InviteActionButton(
                    icon = Icons.Filled.Close,
                    contentDescription = "Decline invitation",
                    background = MaterialTheme.colorScheme.surface,
                    contentColor = TextSecondary,
                    border = Divider,
                    onClick = onDecline
                )
                InviteActionButton(
                    icon = Icons.Filled.Check,
                    contentDescription = "Accept invitation",
                    background = HomePrimary,
                    contentColor = Color.White,
                    border = HomePrimary,
                    onClick = onAccept
                )
            }
        }
    }
}

@Composable
private fun InviteActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    background: Color,
    contentColor: Color,
    border: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = background,
        border = BorderStroke(1.dp, border.copy(alpha = 0.8f))
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = contentColor, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun EmptyHomeCard(
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit
) {
    PremiumCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                Text(action)
            }
        }
    }
}

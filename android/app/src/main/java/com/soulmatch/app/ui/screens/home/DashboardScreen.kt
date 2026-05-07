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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.data.models.HomeContentData
import com.soulmatch.app.data.models.HomeBestMatchAdData
import com.soulmatch.app.data.models.InterestListItem
import com.soulmatch.app.data.models.ProfileSummary
import com.soulmatch.app.data.models.SubscriptionData
import com.soulmatch.app.data.models.defaultHomeBestMatchAds
import com.soulmatch.app.data.models.fullName
import com.soulmatch.app.data.upgrade.UpgradePackage
import com.soulmatch.app.data.upgrade.UpgradePackageGroup
import com.soulmatch.app.ui.components.MemberPhoto
import com.soulmatch.app.ui.components.PremiumCard
import com.soulmatch.app.ui.components.PremiumScreen
import com.soulmatch.app.ui.components.ProfileSideDrawer
import com.soulmatch.app.ui.components.ProfileStrengthAdvisor
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.PrimaryDark
import com.soulmatch.app.ui.theme.SurfaceSoft
import com.soulmatch.app.ui.theme.SurfaceWarm
import com.soulmatch.app.ui.theme.TextSecondary
import com.soulmatch.app.ui.viewmodels.DashboardViewModel
import com.soulmatch.app.ui.viewmodels.NotificationsViewModel
import com.soulmatch.app.ui.viewmodels.SubscriptionViewModel
import kotlinx.coroutines.launch
import java.util.Locale

private val HomeBackground = Color(0xFFFFF9F2)
private val HomePrimary = Color(0xFFD12E5E)
private val SoftBorder = Color(0xFFE1BEC2)
private const val CANONICAL_PLATINUM_RANK = 3_000

private data class BestMatchCarouselSlot(
    val key: String,
    val profile: ProfileSummary? = null,
    val insertSlot: Int = 0
)

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
    onOpenPartnerPreferences: () -> Unit = onOpenProfile,
    onProfileMenuDestination: (String) -> Unit = {},
    onOpenSubscription: () -> Unit = {},
    vm: DashboardViewModel = hiltViewModel(),
    notificationsVm: NotificationsViewModel = hiltViewModel(),
    subscriptionVm: SubscriptionViewModel = hiltViewModel()
) {
    val matches by vm.matches.collectAsStateWithLifecycle()
    val pendingInvitations by vm.pendingInvitations.collectAsStateWithLifecycle()
    val myProfile by vm.myProfile.collectAsStateWithLifecycle()
    val assistEnabled by vm.assistEnabled.collectAsStateWithLifecycle()
    val loading by vm.isLoading.collectAsStateWithLifecycle()
    val notifications by notificationsVm.notifications.collectAsStateWithLifecycle()
    val packageGroups by subscriptionVm.packageGroups.collectAsStateWithLifecycle()
    val subscription by subscriptionVm.subscription.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var partnerPromptSkipped by rememberSaveable(myProfile.profileId) { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        subscriptionVm.load()
    }
    val unreadNotificationCount = notifications.count {
        it.readAt.isNullOrBlank() && !it.status.equals("read", ignoreCase = true)
    }
    val notificationBadgeCount = (unreadNotificationCount + pendingInvitations.size).takeIf { it > 0 }
        ?: notifications.size
    val shouldPromptPartnerPreference = myProfile.profileId.isNotBlank() &&
        myProfile.completionScore >= 60 &&
        !myProfile.isPartnerPrefSet
    val rankedMatches = matches.sortedWith(
        compareByDescending<ProfileSummary> { it.compatibilityScore }
            .thenBy { it.name }
    )
    val profileStrengthScore = myProfile.completionScore.coerceIn(0, 100)
    val bestMatchProfileTarget = content.bestMatchMinimumProfiles.coerceAtLeast(5)
    val bestMatches = rankedMatches.take(bestMatchProfileTarget)
    val newProfiles = rankedMatches.drop(bestMatchProfileTarget).take(8).ifEmpty { rankedMatches.take(6) }
    val canShowUpgradeInsert = content.showBestMatchInsertCards &&
        content.showBestMatchUpgradeCards &&
        shouldShowHomeUpgrade(subscription, packageGroups)
    val adCards = homeBestMatchAdCards(content)

    ProfileSideDrawer(
        drawerState = drawerState,
        profileName = myProfile.fullName(),
        profilePhotoUrl = myProfile.primaryPhotoUrl,
        profileId = myProfile.profileId,
        isVerified = myProfile.verificationStatus.equals("verified", ignoreCase = true),
        membershipLabel = drawerMembershipLabel(subscription),
        showSoulMatchAssist = assistEnabled,
        onDestinationSelected = { destination ->
            scope.launch { drawerState.close() }
            onProfileMenuDestination(destination)
        }
    ) {
        Scaffold(
            topBar = {
                HomeTopBar(
                    unreadCount = notificationBadgeCount,
                    onOpenProfile = { scope.launch { drawerState.open() } },
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
                            WelcomeSection(firstName = myProfile.firstName.ifBlank { "Member" })
                        }
                        if (profileStrengthScore < 100) {
                            item {
                                ProfileStrengthCard(
                                    score = profileStrengthScore,
                                    detail = ProfileStrengthAdvisor.summary(myProfile),
                                    onClick = onOpenProfile
                                )
                            }
                        }
                        if (shouldPromptPartnerPreference && partnerPromptSkipped) {
                            item {
                                PartnerPreferenceReminderBanner(onOpen = onOpenPartnerPreferences)
                            }
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
                            item {
                                BestMatchesCarousel(
                                    profiles = bestMatches,
                                    content = content,
                                    adCards = adCards,
                                    showUpgrade = canShowUpgradeInsert,
                                    showAds = content.showBestMatchInsertCards && content.showBestMatchAdCards,
                                    onViewProfile = onViewProfile,
                                    onInterest = { vm.sendInterest(it) },
                                    onShortlist = { vm.toggleShortlist(it) },
                                    onOpenSubscription = onOpenSubscription,
                                    onOpenSearch = onOpenSearch,
                                    onOpenAstrology = { onProfileMenuDestination("astrology_services") }
                                )
                            }
                        }
                        item {
                            HomeSectionHeader(
                                title = "New Profiles",
                                modifier = Modifier.padding(top = 24.dp),
                                actionText = "See all",
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
                                title = "Pending Invitations",
                                modifier = Modifier.padding(top = 24.dp),
                                actionText = if (pendingInvitations.isEmpty()) null else "View all (${pendingInvitations.size})",
                                onAction = if (pendingInvitations.isEmpty()) null else onOpenInterests
                            )
                        }
                        if (pendingInvitations.isEmpty()) {
                            item {
                                NoPendingInvitationsCard(onBrowse = onOpenSearch)
                            }
                        } else {
                            item {
                                PendingInvitationsCarousel(
                                    invitations = pendingInvitations,
                                    onViewProfile = onViewProfile,
                                    onAccept = { vm.respondToInvitation(it, "accepted") },
                                    onDecline = { vm.respondToInvitation(it, "declined") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (shouldPromptPartnerPreference && !partnerPromptSkipped) {
        PartnerPreferencePromptDialog(
            onSkip = { partnerPromptSkipped = true },
            onOpenPreferences = {
                partnerPromptSkipped = true
                onOpenPartnerPreferences()
            }
        )
    }
}

@Composable
private fun HomeTopBar(
    unreadCount: Int,
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    Surface(
        color = Color(0xFFFFFCFA),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, SoftBorder.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(onClick = onOpenProfile, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Menu, contentDescription = "Open menu", tint = Color(0xFF1E1B18))
                }
                Text(
                    "SoulMatch",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF9B0044)
                )
            }
            IconButton(onClick = onOpenNotifications) {
                Box(
                    modifier = Modifier.size(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = Color(0xFF1E1B18))
                    if (unreadCount > 0) {
                        val badgeText = if (unreadCount > 99) "99+" else unreadCount.toString()
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .defaultMinSize(minWidth = 14.dp)
                                .height(14.dp),
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFF9B0044),
                            border = BorderStroke(1.dp, HomeBackground)
                        ) {
                            Text(
                                badgeText,
                                modifier = Modifier.padding(horizontal = 3.dp),
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
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "Hello, $firstName",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp
            ),
            color = Color(0xFF1E1B18)
        )
        Text(
            "Your journey to a meaningful connection continues.",
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
            color = Color(0xFF594045)
        )
    }
}

@Composable
private fun ProfileStrengthCard(score: Int, detail: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF8)),
        border = BorderStroke(1.dp, SoftBorder.copy(alpha = 0.36f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$score% PROFILE STRENGTH",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF9B0044),
                    fontWeight = FontWeight.ExtraBold
                )
                Icon(Icons.Filled.Verified, contentDescription = null, tint = Color(0xFF9B0044), modifier = Modifier.size(21.dp))
            }
            LinearProgressIndicator(
                progress = score / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = Color(0xFFB0004B),
                trackColor = Color(0xFFE6E2DC)
            )
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text("Review partner preferences", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun BestMatchInsertCard(
    slot: Int,
    adCards: List<HomeBestMatchAdData>,
    showUpgrade: Boolean,
    showAds: Boolean,
    upgradeTitle: String,
    upgradeDetail: String,
    modifier: Modifier = Modifier,
    onOpenSubscription: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAstrology: () -> Unit
) {
    val ad = adCards.getOrNull(slot % adCards.size.coerceAtLeast(1))
    val preferUpgrade = showUpgrade && (slot % 2 == 0 || !showAds || ad == null)
    when {
        preferUpgrade -> HomeUpgradeInsertCard(
            title = upgradeTitle,
            detail = upgradeDetail,
            modifier = modifier,
            onOpenSubscription = onOpenSubscription
        )
        showAds && ad != null -> HomeAdInsertCard(
            ad = ad,
            modifier = modifier,
            onOpen = {
                when (ad.destination.lowercase(Locale.getDefault())) {
                    "membership", "subscription", "upgrade" -> onOpenSubscription()
                    "astrology", "astrology_services" -> onOpenAstrology()
                    else -> onOpenSearch()
                }
            }
        )
        showUpgrade -> HomeUpgradeInsertCard(
            title = upgradeTitle,
            detail = upgradeDetail,
            modifier = modifier,
            onOpenSubscription = onOpenSubscription
        )
    }
}

@Composable
private fun HomeUpgradeInsertCard(
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
    onOpenSubscription: () -> Unit
) {
    PremiumCard(
        modifier = modifier,
        containerColor = Color(0xFFFFF0F4),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(14.dp),
                color = HomePrimary.copy(alpha = 0.12f)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = HomePrimary, modifier = Modifier.size(22.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title.ifBlank { "Upgrade for stronger reach" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryDark
                )
                Text(
                    detail.ifBlank { "Unlock more contact access, visibility, and assisted discovery." },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(onClick = onOpenSubscription) {
                Text("Upgrade")
            }
        }
    }
}

@Composable
private fun HomeAdInsertCard(ad: HomeBestMatchAdData, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    PremiumCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HomeTag(ad.type.ifBlank { "Sponsored" })
                    Text(
                        ad.title.ifBlank { "Recommended service" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryDark,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(onClick = onOpen) {
                    Text(ad.cta.ifBlank { "Open" })
                }
            }
            Text(
                ad.body.ifBlank { "Explore useful services and profiles selected for matrimony journeys." },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun shouldInsertBestMatchCard(content: HomeContentData, index: Int, lastIndex: Int): Boolean {
    if (!content.showBestMatchInsertCards || index >= lastIndex) return false
    val frequency = content.bestMatchInsertFrequency.coerceIn(1, 5)
    return (index + 1) % frequency == 0
}

private fun buildBestMatchSlots(
    profiles: List<ProfileSummary>,
    content: HomeContentData,
    hasInsertContent: Boolean
): List<BestMatchCarouselSlot> {
    var insertSlot = 0
    return buildList {
        profiles.forEachIndexed { index, profile ->
            add(BestMatchCarouselSlot(key = "profile-${profile.profileId}", profile = profile))
            if (hasInsertContent && shouldInsertBestMatchCard(content, index, profiles.lastIndex)) {
                add(BestMatchCarouselSlot(key = "insert-$index", insertSlot = insertSlot))
                insertSlot += 1
            }
        }
    }
}

private fun homeBestMatchAdCards(content: HomeContentData): List<HomeBestMatchAdData> {
    return content.bestMatchAdCards
        .ifEmpty { defaultHomeBestMatchAds() }
        .filter { it.title.isNotBlank() || it.body.isNotBlank() }
}

private fun shouldShowHomeUpgrade(
    subscription: SubscriptionData,
    packageGroups: List<UpgradePackageGroup>
): Boolean {
    val currentPlanId = subscription.planId.ifBlank { "free" }
    if (!subscription.isActive || currentPlanId.equals("free", ignoreCase = true)) return true
    val currentCanonicalRank = canonicalPlanRank(currentPlanId)
    val highestCanonicalRank = packageGroups
        .flatMap { it.packages }
        .map { canonicalPlanRank(it.planId) }
        .filter { it > 0 }
        .maxOrNull()
        ?: CANONICAL_PLATINUM_RANK
    if (currentCanonicalRank > 0) return currentCanonicalRank < highestCanonicalRank

    val allPackages = packageGroups.flatMap { it.packages }
    if (allPackages.isEmpty()) return true
    val currentPackageRank = allPackages
        .firstOrNull { it.planId == currentPlanId || it.pkgId.toString() == currentPlanId }
        ?.let(::upgradePackageRank)
        ?: 0
    val highestPackageRank = allPackages.maxOfOrNull(::upgradePackageRank) ?: currentPackageRank
    return currentPackageRank < highestPackageRank
}

private fun drawerMembershipLabel(subscription: SubscriptionData): String {
    if (!subscription.isActive || subscription.planId.equals("free", ignoreCase = true)) {
        return "Standard member"
    }
    val rawPlan = subscription.planName
        .ifBlank { subscription.planId }
        .replace("_", " ")
        .replace("-", " ")
        .trim()
    val planLabel = rawPlan
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            word.lowercase(Locale.getDefault()).replaceFirstChar { first ->
                if (first.isLowerCase()) first.titlecase(Locale.getDefault()) else first.toString()
            }
        }
        .ifBlank { "Premium" }
    return if (planLabel.contains("member", ignoreCase = true)) planLabel else "$planLabel member"
}

private fun upgradePackageRank(packageInfo: UpgradePackage): Int {
    val canonical = canonicalPlanRank(packageInfo.planId)
    return if (canonical > 0) {
        (canonical * 1000) + packageInfo.pkgDurationDays.coerceAtLeast(0)
    } else {
        packageInfo.payableAmount + packageInfo.pkgDurationDays.coerceAtLeast(0)
    }
}

private fun canonicalPlanRank(planId: String): Int {
    return when (planId.lowercase(Locale.getDefault())) {
        "silver" -> 1_000
        "gold" -> 2_000
        "platinum" -> CANONICAL_PLATINUM_RANK
        else -> 0
    }
}

@Composable
private fun PartnerPreferenceReminderBanner(onOpen: () -> Unit) {
    PremiumCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        containerColor = Color(0xFFFFF0F3),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "Set Your Partner Preferences",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryDark
                )
                Text(
                    "Add age, religion, and manglik preference to improve match ranking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Button(onClick = onOpen) {
                Text("Update")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PartnerPreferencePromptDialog(
    onSkip: () -> Unit,
    onOpenPreferences: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text("Set Your Partner Preferences") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "SoulMatch can rank matches much better once your preferences are saved.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                listOf(
                    "Basics: preferred age range",
                    "Community: religion or flexible matching",
                    "Horoscope: manglik preference"
                ).forEach { label ->
                    Surface(shape = RoundedCornerShape(14.dp), color = SurfaceWarm, border = BorderStroke(1.dp, Divider)) {
                        Text(
                            label,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    "You can skip now, but SoulMatch will keep reminding you until this is set.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            Button(onClick = onOpenPreferences) {
                Text("Set preferences")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onSkip) {
                Text("Remind later")
            }
        }
    )
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
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            ),
            color = Color(0xFF1E1B18)
        )
        when {
            trailing != null -> trailing()
            actionText != null && onAction != null -> {
                Text(
                    actionText,
                    modifier = Modifier.clickable(onClick = onAction),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9B0044),
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun BestMatchesCarousel(
    profiles: List<ProfileSummary>,
    content: HomeContentData,
    adCards: List<HomeBestMatchAdData>,
    showUpgrade: Boolean,
    showAds: Boolean,
    onViewProfile: (String) -> Unit,
    onInterest: (String) -> Unit,
    onShortlist: (String) -> Unit,
    onOpenSubscription: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAstrology: () -> Unit
) {
    val slots = remember(profiles, content, showUpgrade, showAds, adCards) {
        buildBestMatchSlots(
            profiles = profiles,
            content = content,
            hasInsertContent = showUpgrade || (showAds && adCards.isNotEmpty())
        )
    }
    val state = rememberLazyListState()
    LazyRow(
        state = state,
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(slots, key = { it.key }) { slot ->
            if (slot.profile != null) {
                HomeMatchCard(
                    profile = slot.profile,
                    modifier = Modifier.fillParentMaxWidth(0.88f),
                    onOpen = { onViewProfile(slot.profile.profileId) },
                    onInterest = { onInterest(slot.profile.profileId) },
                    onShortlist = { onShortlist(slot.profile.profileId) }
                )
            } else {
                BestMatchInsertCard(
                    slot = slot.insertSlot,
                    adCards = adCards,
                    showUpgrade = showUpgrade,
                    showAds = showAds,
                    upgradeTitle = content.upgradeTitle,
                    upgradeDetail = content.upgradeDetail,
                    modifier = Modifier
                        .fillParentMaxWidth(0.88f)
                        .aspectRatio(4f / 5f),
                    onOpenSubscription = onOpenSubscription,
                    onOpenSearch = onOpenSearch,
                    onOpenAstrology = onOpenAstrology
                )
            }
        }
    }
    BestMatchDots(state = state, slots = slots, profileCount = profiles.size)
}

@Composable
private fun BestMatchDots(
    state: LazyListState,
    slots: List<BestMatchCarouselSlot>,
    profileCount: Int
) {
    val activeProfileIndex by remember(state, slots) {
        derivedStateOf {
            val visible = state.firstVisibleItemIndex.coerceIn(0, slots.lastIndex.coerceAtLeast(0))
            slots.take(visible + 1).count { it.profile != null }.minus(1).coerceAtLeast(0)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(profileCount.coerceAtMost(5)) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .width(if (index == activeProfileIndex) 14.dp else 5.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (index == activeProfileIndex) Color(0xFFB0004B) else Color(0xFFE6E2DC))
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeMatchCard(
    profile: ProfileSummary,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onInterest: () -> Unit,
    onShortlist: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(4f / 5f)
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MemberPhoto(
                photoUrl = profile.primaryPhoto,
                contentDescription = "Photo of ${profile.name}",
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(24.dp)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.18f),
                                Color.Black.copy(alpha = 0.84f)
                            )
                        )
                    )
            )
            if (profile.isPhotoPrivate && profile.primaryPhoto.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White.copy(alpha = 0.86f),
                    border = BorderStroke(1.dp, Divider.copy(alpha = 0.72f))
                ) {
                    Text(
                        "Request photo",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryDark,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
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
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    OverlayTag("Created by ${profileOwnerLabel(profile.profileCreatedBy)}")
                    OverlayTag(profile.matchReasons.firstOrNull { it.isNotBlank() } ?: "Fits age range")
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${profile.name.removeSuffix(".")}, ${profile.age}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (profile.isVerified) {
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Filled.Verified, contentDescription = "Verified", tint = Color.White, modifier = Modifier.size(17.dp))
                        }
                    }
                    Text(
                        "${profile.occupation.ifBlank { "Profession not added" }} | ${profile.location.ifBlank { "Location not added" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.86f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFFB0004B),
                        border = BorderStroke(2.dp, Color.White)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "${profile.compatibilityScore.coerceIn(0, 99)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Text(
                        "Match Compatibility",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.92f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun profileOwnerLabel(profileCreatedBy: String): String =
    if (profileCreatedBy.equals("mediator", ignoreCase = true)) "Mediator" else "Self"

@Composable
private fun OverlayTag(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.20f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
    ) {
        Text(
            label.uppercase(Locale.getDefault()),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
        color = Color.White.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))
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
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
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
            .width(146.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.Start
    ) {
        MemberPhoto(
            photoUrl = profile.primaryPhoto,
            contentDescription = profile.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "${profile.name.removeSuffix(".")}, ${profile.age}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF1E1B18),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start
        )
        Text(
            listOf(profile.occupation, profile.location)
                .filter { it.isNotBlank() }
                .joinToString(" | ")
                .ifBlank { "Details in progress" },
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF594045),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun PendingInvitationsCarousel(
    invitations: List<InterestListItem>,
    onViewProfile: (String) -> Unit,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(invitations, key = { it.interestId }) { invitation ->
            PendingInvitationCard(
                invitation = invitation,
                onOpen = { onViewProfile(invitation.profileId) },
                onAccept = { onAccept(invitation.interestId) },
                onDecline = { onDecline(invitation.interestId) }
            )
        }
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
            .width(272.dp)
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF8)),
        border = BorderStroke(1.dp, SoftBorder.copy(alpha = 0.34f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                MemberPhoto(
                    photoUrl = invitation.primaryPhotoUrl,
                    contentDescription = invitation.fullName(),
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(0.dp)
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White.copy(alpha = 0.92f)
                ) {
                    Text(
                        invitationAgeLabel(invitation.sentAt).uppercase(Locale.getDefault()),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9B0044),
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            }
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        invitation.fullName().ifBlank { "Pending invitation" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF1E1B18),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color(0xFF594045), modifier = Modifier.size(14.dp))
                        Text(
                            listOf(
                                invitation.occupation,
                                invitation.workingCity.ifBlank { invitation.familyCity }
                            ).filter { it.isNotBlank() }
                                .joinToString(" | ")
                                .ifBlank { "Open profile to review details" },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF594045),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text("Accept", fontWeight = FontWeight.ExtraBold)
                    }
                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text("Decline")
                    }
                }
            }
        }
    }
}

@Composable
private fun NoPendingInvitationsCard(onBrowse: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF5ECE7).copy(alpha = 0.46f),
        border = BorderStroke(1.dp, SoftBorder.copy(alpha = 0.42f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(999.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.MailOutline, contentDescription = null, tint = TextSecondary)
                }
            }
            Text(
                "No pending invitations",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
                color = Color(0xFF1E1B18)
            )
            Text(
                "Keep exploring to find your perfect match.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF594045),
                textAlign = TextAlign.Center
            )
            OutlinedButton(onClick = onBrowse, shape = RoundedCornerShape(999.dp)) {
                Text("Browse matches", color = Color(0xFF9B0044), fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

private fun invitationAgeLabel(sentAt: String): String {
    return if (sentAt.isBlank()) "Recently" else "Recently"
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

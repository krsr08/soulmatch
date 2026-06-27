package com.soulmatch.app.ui.screens.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.data.local.ProfileInteractionStore
import com.soulmatch.app.data.models.HomeContentData
import com.soulmatch.app.data.models.HomeBestMatchAdData
import com.soulmatch.app.data.models.HomeScamAwarenessCardData
import com.soulmatch.app.data.models.InterestListItem
import com.soulmatch.app.data.models.ProfileSummary
import com.soulmatch.app.data.models.SubscriptionData
import com.soulmatch.app.data.models.defaultHomeBestMatchAds
import com.soulmatch.app.data.models.fullName
import com.soulmatch.app.data.upgrade.UpgradePackage
import com.soulmatch.app.data.upgrade.UpgradePackageGroup
import com.soulmatch.app.ui.components.media.MemberPhoto
import com.soulmatch.app.ui.components.premium.PremiumCard
import com.soulmatch.app.ui.components.premium.PremiumScreen
import com.soulmatch.app.ui.components.navigation.ProfileSideDrawer
import com.soulmatch.app.ui.components.status.ProfileStrengthAdvisor
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

private val HomeBackground = Color(0xFFFFFFFF)
private val HomePrimary = Color(0xFFFF5C00)
private val SoftBorder = Color(0xFFF3F4F6)
private const val CANONICAL_PLATINUM_RANK = 3_000

private data class BestMatchCarouselSlot(
    val key: String,
    val profile: ProfileSummary? = null,
    val insertSlot: Int = 0
)

private data class HomeAdPalette(
    val container: Color,
    val border: Color,
    val chip: Color,
    val accent: Color,
    val title: Color,
    val body: Color
)

private enum class HomeMatchFeedFilter(val label: String) {
    Viewed("Viewed"),
    JustJoined("Just Joined"),
    Nearby("Nearby")
}

private data class HomeAdvancedFilters(
    val typeOfMatches: String = "All",
    val religion: String = "All",
    val onlineStatus: String = "All",
    val familyBasedOutOf: String = "All",
    val profilePostedBy: String = "All",
    val activityOnSite: String = "All",
    val country: String = "All",
    val city: String = "All",
    val income: String = "All",
    val educationLevel: String = "All",
    val employedIn: String = "All",
    val occupation: String = "All",
    val photo: String = "All",
    val heightBand: String = "All",
    val ageBand: String = "All",
    val maritalStatus: String = "All",
    val horoscope: String = "All",
    val manglik: String = "All",
    val diet: String = "All",
    val minimumMatch: Int = 0,
    val minimumTrust: Int = 0
)

private data class HomeFilterOptions(
    val religions: List<String> = emptyList(),
    val familyBases: List<String> = emptyList(),
    val cities: List<String> = emptyList(),
    val incomes: List<String> = emptyList(),
    val educationLevels: List<String> = emptyList(),
    val occupations: List<String> = emptyList(),
    val diets: List<String> = emptyList()
)

private data class HomeFilterSectionSpec(
    val title: String,
    val options: List<String>,
    val value: (HomeAdvancedFilters) -> String,
    val update: (HomeAdvancedFilters, String) -> HomeAdvancedFilters
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
    val interactionState by ProfileInteractionStore.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var partnerPromptSkipped by rememberSaveable(myProfile.profileId) { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        subscriptionVm.load()
    }
    DisposableEffect(lifecycleOwner, vm) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.loadProfile()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val unreadNotificationCount = notifications.count {
        it.readAt.isNullOrBlank() && !it.status.equals("read", ignoreCase = true)
    }
    val notificationBadgeCount = (unreadNotificationCount + pendingInvitations.size).takeIf { it > 0 }
        ?: notifications.size
    val profileStrengthScore = ProfileStrengthAdvisor.score(myProfile)
    val shouldPromptPartnerPreference = myProfile.profileId.isNotBlank() &&
        profileStrengthScore >= 60 &&
        !myProfile.isPartnerPrefSet
    val rankedMatches = matches.sortedWith(
        compareByDescending<ProfileSummary> { it.compatibilityScore }
            .thenBy { it.name }
    )
    val filterOptions = remember(rankedMatches) { buildHomeFilterOptions(rankedMatches) }
    var selectedFeedFilter by rememberSaveable { mutableStateOf<HomeMatchFeedFilter?>(null) }
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    var advancedFilters by remember { mutableStateOf(HomeAdvancedFilters()) }
    val filteredRankedMatches = rankedMatches.filterForHomeFeed(
        filter = selectedFeedFilter,
        viewerProfile = myProfile,
        viewedProfileIds = interactionState.viewedProfileIds,
        advancedFilters = advancedFilters
    )
    val bestMatches = filteredRankedMatches.take(MAX_HOME_MATCHES)
    val canShowUpgradeInsert = content.showBestMatchInsertCards &&
        content.showBestMatchUpgradeCards &&
        shouldShowHomeUpgrade(subscription, packageGroups)
    val adCards = homeBestMatchAdCards(content, subscription, assistEnabled)
        .filter { canShowUpgradeInsert || !it.isUpgradeType() }

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
                    profile = myProfile,
                    unreadCount = notificationBadgeCount,
                    onOpenProfile = { scope.launch { drawerState.open() } },
                    onOpenNotifications = onOpenNotifications,
                    onOpenSearch = onOpenSearch,
                    onOpenPartnerPreferences = onOpenPartnerPreferences
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
                            HomeMatchFilterStrip(
                                selected = selectedFeedFilter,
                                onOpenAdvancedFilters = { showFilterDialog = true },
                                onSelected = { filter ->
                                    val nextFilter = if (selectedFeedFilter == filter) null else filter
                                    selectedFeedFilter = nextFilter
                                    advancedFilters = advancedFilters.copy(typeOfMatches = nextFilter?.label ?: "All")
                                }
                            )
                        }
                        if (profileStrengthScore < 100) {
                            item {
                                ProfileStrengthCard(
                                    score = profileStrengthScore,
                                    detail = ProfileStrengthAdvisor.summary(myProfile),
                                    onClick = onOpenPartnerPreferences
                                )
                            }
                        }
                        if (bestMatches.isEmpty()) {
                            item {
                                EmptyHomeCard(
                                    title = if (selectedFeedFilter == null) {
                                        content.emptyTitle.ifBlank { "Your profile needs a little more detail" }
                                    } else {
                                        "No ${selectedFeedFilter?.label.orEmpty().lowercase(Locale.getDefault())} matches yet"
                                    },
                                    body = if (selectedFeedFilter == null) {
                                        content.emptyBody.ifBlank { "Complete family, lifestyle, and preference details to unlock stronger recommendations." }
                                    } else {
                                        "Try another filter or refine your preferences to continue browsing compatible profiles."
                                    },
                                    action = if (selectedFeedFilter == null) content.emptyCta.ifBlank { "Improve my profile" } else "Open filters",
                                    onAction = {
                                        if (selectedFeedFilter == null) onOpenProfile() else showFilterDialog = true
                                    }
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
                                    onMarkViewed = { vm.markProfileViewed(it) },
                                    onInterest = { vm.sendInterest(it) },
                                    onShortlist = { vm.toggleShortlist(it) },
                                    onIgnore = { vm.hideProfile(it) },
                                    onRequestPhoto = { vm.requestPhotoAccess(it) },
                                    onChat = { profileId, name -> onOpenChat(profileId, name) },
                                    onOpenSubscription = onOpenSubscription,
                                    onOpenSearch = onOpenSearch,
                                    onOpenAstrology = { onProfileMenuDestination("astrology_services") },
                                    onOpenDestination = onProfileMenuDestination
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
    if (showFilterDialog) {
        HomeAdvancedFilterDialog(
            filters = advancedFilters,
            options = filterOptions,
            onDismiss = { showFilterDialog = false },
            onApply = {
                advancedFilters = it
                selectedFeedFilter = quickFilterForType(it.typeOfMatches)
                showFilterDialog = false
            },
            onReset = {
                advancedFilters = HomeAdvancedFilters()
                selectedFeedFilter = null
            }
        )
    }
}

@Composable
private fun HomeTopBar(
    profile: com.soulmatch.app.data.models.ProfileData,
    unreadCount: Int,
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenPartnerPreferences: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, SoftBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clickable(onClick = onOpenProfile)
                ) {
                    MemberPhoto(
                        photoUrl = profile.primaryPhotoUrl,
                        contentDescription = "Open profile menu",
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(999.dp)
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(21.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White,
                        shadowElevation = 1.dp,
                        border = BorderStroke(1.dp, Divider.copy(alpha = 0.65f))
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Menu, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "My matches",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 29.sp
                        ),
                        color = Color(0xFF1A1A1A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.clickable(onClick = onOpenPartnerPreferences),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            "as per ",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF888888)
                        )
                        Text(
                            "partner preferences",
                            style = MaterialTheme.typography.labelLarge,
                            color = HomePrimary,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(Icons.Filled.Edit, contentDescription = "Edit partner preferences", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenNotifications) {
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = HomePrimary)
                        if (unreadCount > 0) {
                            val badgeText = if (unreadCount > 99) "99+" else unreadCount.toString()
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .defaultMinSize(minWidth = 14.dp)
                                    .height(14.dp),
                                shape = RoundedCornerShape(999.dp),
                                color = HomePrimary,
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
                IconButton(onClick = onOpenSearch) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Search matches",
                        tint = HomePrimary,
                        modifier = Modifier.size(29.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeMatchFilterStrip(
    selected: HomeMatchFeedFilter?,
    onOpenAdvancedFilters: () -> Unit,
    onSelected: (HomeMatchFeedFilter) -> Unit
) {
    Surface(
        color = Color.White,
        border = BorderStroke(1.dp, Divider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeFilterPill(
                label = "Filters",
                selected = false,
                leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(15.dp)) },
                modifier = Modifier.weight(0.92f),
                onClick = onOpenAdvancedFilters
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(Color(0xFFD7DDE5))
            )
            HomeMatchFeedFilter.entries.forEach { filter ->
                HomeFilterPill(
                    label = filter.label,
                    selected = selected == filter,
                    modifier = Modifier.weight(if (filter == HomeMatchFeedFilter.JustJoined) 1.22f else 1f),
                    onClick = { onSelected(filter) }
                )
            }
        }
    }
}

@Composable
private fun HomeFilterPill(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val borderColor = if (selected) HomePrimary else Color(0xFFA9B0BA)
    val contentColor = if (selected) HomePrimary else Color(0xFF1A1A1A)
    Surface(
        modifier = modifier
            .height(38.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) Color(0xFFFFF1E8) else Color.White,
        border = BorderStroke(1.4.dp, borderColor.copy(alpha = if (selected) 0.88f else 0.72f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides contentColor) {
                    leadingIcon()
                }
                Spacer(Modifier.width(4.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeAdvancedFilterDialog(
    filters: HomeAdvancedFilters,
    options: HomeFilterOptions,
    onDismiss: () -> Unit,
    onApply: (HomeAdvancedFilters) -> Unit,
    onReset: () -> Unit
) {
    var draft by remember(filters) { mutableStateOf(filters) }
    val sections = remember(options) { homeFilterSections(options) }
    var selectedTab by rememberSaveable { mutableStateOf(sections.first().title) }
    val activeSection = sections.firstOrNull { it.title == selectedTab } ?: sections.first()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                        .background(Color.White)
                        .padding(start = 22.dp, end = 18.dp, top = 22.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Refine matches",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 28.sp
                            ),
                            color = Color(0xFF202A36)
                        )
                        Text(
                            "Choose filters that matter to your family.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF6E7785),
                            maxLines = 2
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                draft = HomeAdvancedFilters()
                                onReset()
                            },
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, Color(0xFFD0D6DE)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = HomePrimary),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text("Reset", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Close filters",
                                tint = Color(0xFF9AA1AB),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                Row(Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier
                            .width(146.dp)
                            .fillMaxSize()
                            .background(Color(0xFFF4F2EF)),
                        contentPadding = PaddingValues(top = 40.dp, bottom = 24.dp)
                    ) {
                        items(sections, key = { it.title }) { section ->
                            val selected = selectedTab == section.title
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = if (selected) 0.dp else 10.dp, top = 6.dp, bottom = 6.dp)
                                    .defaultMinSize(minHeight = 52.dp)
                                    .clickable { selectedTab = section.title },
                                shape = if (selected) {
                                    RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
                                } else {
                                    RoundedCornerShape(0.dp)
                                },
                                color = if (selected) Color.White else Color.Transparent,
                                shadowElevation = if (selected) 3.dp else 0.dp
                            ) {
                                Text(
                                    section.title,
                                    modifier = Modifier.padding(start = 28.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                                    style = MaterialTheme.typography.titleMedium.copy(lineHeight = 22.sp),
                                    color = if (selected) HomePrimary else Color(0xFF3B4655),
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 42.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        item {
                            FilterChoiceSection(
                                title = activeSection.title,
                                options = activeSection.options,
                                selected = activeSection.value(draft),
                                onSelect = { value -> draft = activeSection.update(draft, value) }
                            )
                        }
                        if (activeSection.title == "Type of Matches") {
                            item {
                                FilterChoiceSection(
                                    title = "Match compatibility",
                                    options = listOf("All", "70%+", "80%+", "90%+"),
                                    selected = when (draft.minimumMatch) {
                                        90 -> "90%+"
                                        80 -> "80%+"
                                        70 -> "70%+"
                                        else -> "All"
                                    },
                                    onSelect = { draft = draft.copy(minimumMatch = it.filter(Char::isDigit).toIntOrNull() ?: 0) }
                                )
                            }
                            item {
                                FilterChoiceSection(
                                    title = "Trust score",
                                    options = listOf("All", "50%+", "75%+", "90%+"),
                                    selected = when (draft.minimumTrust) {
                                        90 -> "90%+"
                                        75 -> "75%+"
                                        50 -> "50%+"
                                        else -> "All"
                                    },
                                    onSelect = { draft = draft.copy(minimumTrust = it.filter(Char::isDigit).toIntOrNull() ?: 0) }
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .background(Color.White)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            draft = HomeAdvancedFilters()
                            onReset()
                        },
                        modifier = Modifier
                            .weight(0.92f)
                            .height(56.dp),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.2.dp, Color(0xFFD0D6DE)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = HomePrimary,
                            containerColor = Color.White
                        )
                    ) {
                        Text("Reset", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    }
                    Button(
                        onClick = { onApply(draft) },
                        modifier = Modifier
                            .weight(1.22f)
                            .height(56.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HomePrimary,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp)
                    ) {
                        Text("Apply filters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

private fun homeFilterSections(options: HomeFilterOptions): List<HomeFilterSectionSpec> {
    fun withAll(items: List<String>, fallbacks: List<String> = emptyList()): List<String> =
        listOf("All") + (items + fallbacks)
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.equals("All", ignoreCase = true) }
            .distinctBy { it.lowercase(Locale.getDefault()) }

    return listOf(
        HomeFilterSectionSpec(
            title = "Type of Matches",
            options = listOf("All", "Viewed", "Verified", "Just Joined", "Nearby"),
            value = { it.typeOfMatches },
            update = { filters, value -> filters.copy(typeOfMatches = value) }
        ),
        HomeFilterSectionSpec(
            title = "Religion",
            options = withAll(options.religions),
            value = { it.religion },
            update = { filters, value -> filters.copy(religion = value) }
        ),
        HomeFilterSectionSpec(
            title = "Online Status",
            options = listOf("All", "Online", "Recently Active", "Inactive"),
            value = { it.onlineStatus },
            update = { filters, value -> filters.copy(onlineStatus = value) }
        ),
        HomeFilterSectionSpec(
            title = "Family Based Out Of",
            options = withAll(options.familyBases),
            value = { it.familyBasedOutOf },
            update = { filters, value -> filters.copy(familyBasedOutOf = value) }
        ),
        HomeFilterSectionSpec(
            title = "Profile Posted By",
            options = listOf("All", "Self", "Agent", "Admin"),
            value = { it.profilePostedBy },
            update = { filters, value -> filters.copy(profilePostedBy = value) }
        ),
        HomeFilterSectionSpec(
            title = "Activity on Site",
            options = listOf("All", "Today", "This Week", "This Month"),
            value = { it.activityOnSite },
            update = { filters, value -> filters.copy(activityOnSite = value) }
        ),
        HomeFilterSectionSpec(
            title = "Country",
            options = listOf("All", "India", "Abroad"),
            value = { it.country },
            update = { filters, value -> filters.copy(country = value) }
        ),
        HomeFilterSectionSpec(
            title = "City",
            options = withAll(options.cities),
            value = { it.city },
            update = { filters, value -> filters.copy(city = value) }
        ),
        HomeFilterSectionSpec(
            title = "Income",
            options = withAll(options.incomes, listOf("< 3 Lpa", "3-5 Lpa", "5-10 Lpa", "10-20 Lpa", "20+ Lpa")),
            value = { it.income },
            update = { filters, value -> filters.copy(income = value) }
        ),
        HomeFilterSectionSpec(
            title = "Education",
            options = withAll(options.educationLevels, listOf("Graduate", "Post Graduate", "Doctorate", "Professional")),
            value = { it.educationLevel },
            update = { filters, value -> filters.copy(educationLevel = value) }
        ),
        HomeFilterSectionSpec(
            title = "Employed In",
            options = listOf("All", "Government", "Private", "Business", "Self-employed", "Not working"),
            value = { it.employedIn },
            update = { filters, value -> filters.copy(employedIn = value) }
        ),
        HomeFilterSectionSpec(
            title = "Occupation",
            options = withAll(options.occupations),
            value = { it.occupation },
            update = { filters, value -> filters.copy(occupation = value) }
        ),
        HomeFilterSectionSpec(
            title = "Photo",
            options = listOf("All", "Visible Photos", "Request Photo"),
            value = { it.photo },
            update = { filters, value -> filters.copy(photo = value) }
        ),
        HomeFilterSectionSpec(
            title = "Height",
            options = listOf("All", "Below 5 ft", "5 ft - 5 ft 5 in", "5 ft 6 in - 6 ft", "Above 6 ft"),
            value = { it.heightBand },
            update = { filters, value -> filters.copy(heightBand = value) }
        ),
        HomeFilterSectionSpec(
            title = "Age",
            options = listOf("All", "21-25", "26-30", "31-35", "36-40", "41+"),
            value = { it.ageBand },
            update = { filters, value -> filters.copy(ageBand = value) }
        ),
        HomeFilterSectionSpec(
            title = "Marital Status",
            options = listOf("All", "Never Married", "Divorced", "Widowed"),
            value = { it.maritalStatus },
            update = { filters, value -> filters.copy(maritalStatus = value) }
        ),
        HomeFilterSectionSpec(
            title = "Horoscope",
            options = listOf("All", "Available", "Not Available"),
            value = { it.horoscope },
            update = { filters, value -> filters.copy(horoscope = value) }
        ),
        HomeFilterSectionSpec(
            title = "Manglik",
            options = listOf("All", "Manglik", "Non-Manglik"),
            value = { it.manglik },
            update = { filters, value -> filters.copy(manglik = value) }
        ),
        HomeFilterSectionSpec(
            title = "Diet",
            options = withAll(options.diets, listOf("Vegetarian", "Non vegetarian", "Eggetarian", "Vegan")),
            value = { it.diet },
            update = { filters, value -> filters.copy(diet = value) }
        )
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FilterChoiceSection(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 30.sp
            ),
            color = HomePrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            options.forEach { option ->
                RefineFilterOptionButton(
                    label = option,
                    selected = selected == option,
                    onClick = { onSelect(option) }
                )
            }
        }
    }
}

@Composable
private fun RefineFilterOptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = Color.White,
        border = BorderStroke(
            width = if (selected) 1.6.dp else 1.dp,
            color = if (selected) HomePrimary else Color(0xFFD0D6DE)
        )
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                label,
                modifier = Modifier.padding(horizontal = 14.dp),
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) HomePrimary else Color(0xFF202A36),
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
            .padding(horizontal = 20.dp, vertical = 12.dp)
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
                    color = HomePrimary,
                    fontWeight = FontWeight.ExtraBold
                )
                Icon(Icons.Filled.Verified, contentDescription = null, tint = HomePrimary, modifier = Modifier.size(21.dp))
            }
            LinearProgressIndicator(
                progress = score.coerceIn(0, 100) / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = PrimaryDark,
                trackColor = Color(0xFFE6E2DC)
            )
            Text(
                detail.ifBlank { "Complete profile and preference details to improve match quality." },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
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
    scamCards: List<HomeScamAwarenessCardData>,
    showUpgrade: Boolean,
    showAds: Boolean,
    upgradeTitle: String,
    upgradeDetail: String,
    modifier: Modifier = Modifier,
    onOpenSubscription: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAstrology: () -> Unit,
    onOpenDestination: (String) -> Unit
) {
    val ad = adCards.getOrNull(slot % adCards.size.coerceAtLeast(1))
    val preferUpgrade = showUpgrade && (!showAds || ad == null)
    when {
        preferUpgrade -> HomeUpgradeInsertCard(
            title = upgradeTitle,
            detail = upgradeDetail,
            modifier = modifier,
            onOpenSubscription = onOpenSubscription
        )
        showAds && ad != null -> {
            when (ad.type.lowercase(Locale.getDefault())) {
                "spotlight" -> HomeSpotlightInsertCard(ad = ad, modifier = modifier, onOpen = onOpenSubscription)
                "safety", "scam", "fraud" -> HomeScamAwarenessInsertCard(
                    ad = ad,
                    cards = scamCards,
                    modifier = modifier,
                    onOpen = { openHomeAdDestination(ad.destination, onOpenSubscription, onOpenSearch, onOpenAstrology, onOpenDestination) }
                )
                "upgrade", "membership", "subscription" -> HomeUpgradeBenefitsCard(
                    ad = ad,
                    modifier = modifier,
                    onOpen = onOpenSubscription
                )
                "trust" -> HomeTrustProgressInsertCard(
                    ad = ad,
                    modifier = modifier,
                    onOpen = { openHomeAdDestination(ad.destination, onOpenSubscription, onOpenSearch, onOpenAstrology, onOpenDestination) }
                )
                "astrology", "horoscope" -> HomeHoroscopeInsertCard(
                    ad = ad,
                    modifier = modifier,
                    onOpen = { openHomeAdDestination(ad.destination, onOpenSubscription, onOpenSearch, onOpenAstrology, onOpenDestination) }
                )
                "notification", "alerts" -> HomeNotificationInsertCard(
                    ad = ad,
                    modifier = modifier,
                    onOpen = { openHomeAdDestination(ad.destination, onOpenSubscription, onOpenSearch, onOpenAstrology, onOpenDestination) }
                )
                else -> HomeAdInsertCard(
                    ad = ad,
                    modifier = modifier,
                    onOpen = {
                        openHomeAdDestination(ad.destination, onOpenSubscription, onOpenSearch, onOpenAstrology, onOpenDestination)
                    }
                )
            }
        }
        showUpgrade -> HomeUpgradeInsertCard(
            title = upgradeTitle,
            detail = upgradeDetail,
            modifier = modifier,
            onOpenSubscription = onOpenSubscription
        )
    }
}

private fun openHomeAdDestination(
    destination: String,
    onOpenSubscription: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAstrology: () -> Unit,
    onOpenDestination: (String) -> Unit
) {
    when (destination.lowercase(Locale.getDefault())) {
        "membership", "subscription", "upgrade" -> onOpenSubscription()
        "astrology", "astrology_services" -> onOpenAstrology()
        "search", "discover" -> onOpenSearch()
        "safety" -> onOpenDestination("safety_center")
        "success_stories" -> onOpenDestination("success_stories/overview")
        else -> onOpenDestination(destination)
    }
}

@Composable
private fun HomeUpgradeBenefitsCard(
    ad: HomeBestMatchAdData,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1430)),
        border = BorderStroke(1.dp, Color(0xFF332148)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF3A233E), Color(0xFF0D0820))))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(
                        ad.discountLabel.ifBlank { "Now 73% OFF on all memberships!" },
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 36.sp
                        ),
                        color = Color(0xFFFFB5A6),
                        textAlign = TextAlign.Center
                    )
                    val bullets = ad.bullets.ifEmpty {
                        listOf(
                            "Get upto 3X more matches daily",
                            "Get access to contact details",
                            "Perform unlimited searches",
                            "Get 3 spotlights for free"
                        )
                    }
                    bullets.take(4).forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (index) {
                                    1 -> Icons.Outlined.MailOutline
                                    2 -> Icons.Filled.Search
                                    3 -> Icons.Filled.Star
                                    else -> Icons.Outlined.FavoriteBorder
                                },
                                contentDescription = null,
                                tint = Color(0xFFFF73B7),
                                modifier = Modifier.size(27.dp)
                            )
                            Text(
                                item,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Button(onClick = onOpen, shape = RoundedCornerShape(14.dp)) {
                    Text(ad.cta.ifBlank { "Upgrade now" }, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun HomeTrustProgressInsertCard(
    ad: HomeBestMatchAdData,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "trust-card")
    val fill by transition.animateFloat(
        initialValue = 0.56f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "trust-fill"
    )
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFFAF5)),
        border = BorderStroke(1.dp, Color(0xFFBDE5CD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(999.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFBDE5CD))) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = Color(0xFF0F7A4F), modifier = Modifier.size(18.dp))
                        Text(ad.badge.ifBlank { "Trust profile" }, style = MaterialTheme.typography.labelMedium, color = Color(0xFF0F7A4F), fontWeight = FontWeight.ExtraBold)
                    }
                }
                Text(ad.title.ifBlank { "Build trust before families connect" }, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, lineHeight = 31.sp), color = Color(0xFF143B2A))
                Text(ad.body.ifBlank { "Complete verification details to improve response confidence." }, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF49695A), maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LinearProgressIndicator(
                    progress = fill,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = Color(0xFF0F7A4F),
                    trackColor = Color.White
                )
                ad.bullets.ifEmpty { listOf("Higher trust score", "More confident family responses", "Verification status stays visible") }
                    .take(3)
                    .forEach { bullet ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF0F7A4F), modifier = Modifier.size(17.dp))
                            Text(bullet, style = MaterialTheme.typography.bodySmall, color = Color(0xFF143B2A), fontWeight = FontWeight.SemiBold)
                        }
                    }
                Button(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text(ad.cta.ifBlank { "Improve trust" }, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun HomeHoroscopeInsertCard(
    ad: HomeBestMatchAdData,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "horoscope-card")
    val glow by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "horoscope-glow"
    )
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F0FF)),
        border = BorderStroke(1.dp, Color(0xFFD8B2FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFF8EDFF), Color(0xFFFFFBF4))))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Surface(modifier = Modifier.size(108.dp), shape = RoundedCornerShape(999.dp), color = Color(0xFF6D28D9).copy(alpha = glow)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFF6D28D9), modifier = Modifier.size(48.dp))
                    }
                }
                Text(ad.badge.ifBlank { "Horoscope" }, style = MaterialTheme.typography.labelLarge, color = Color(0xFF6D28D9), fontWeight = FontWeight.ExtraBold)
                Text(ad.title.ifBlank { "Add horoscope details for family compatibility" }, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.ExtraBold, lineHeight = 31.sp), color = Color(0xFF3B136F), textAlign = TextAlign.Center)
                Text(ad.body.ifBlank { "Rashi, nakshatra, and manglik details help families compare expectations early." }, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF5E3E85), textAlign = TextAlign.Center)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ad.bullets.ifEmpty { listOf("Kundli details improve family fit", "Manglik and rashi checks stay clear", "Useful before a family call") }
                    .take(3)
                    .forEach { bullet ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF6D28D9), modifier = Modifier.size(17.dp))
                            Text(bullet, style = MaterialTheme.typography.bodySmall, color = Color(0xFF3B136F), fontWeight = FontWeight.SemiBold)
                        }
                    }
                Button(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text(ad.cta.ifBlank { "Open astrology" }, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun HomeNotificationInsertCard(
    ad: HomeBestMatchAdData,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF4FF)),
        border = BorderStroke(1.dp, Color(0xFFB9D6F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(modifier = Modifier.size(104.dp), shape = RoundedCornerShape(999.dp), color = Color.White) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Notifications, contentDescription = null, tint = Color(0xFF2563A8), modifier = Modifier.size(48.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(ad.title.ifBlank { "Turn on match alerts" }, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, lineHeight = 31.sp), color = Color(0xFF183B66), textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(ad.body.ifBlank { "Get notified when serious matches send interest, accept, or message you." }, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF3E5873), textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp))
            Button(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Text(ad.cta.ifBlank { "Manage alerts" }, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun HomeSpotlightInsertCard(
    ad: HomeBestMatchAdData,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFAB48)),
        border = BorderStroke(1.dp, Color(0xFFFF8B18)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Color(0xFFFF8B1A), Color(0xFFFFF8EF)))
                )
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(modifier = Modifier.size(118.dp), shape = RoundedCornerShape(999.dp), color = Color(0xFF172534)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(54.dp))
                }
            }
            Spacer(Modifier.height(38.dp))
            Text(
                ad.title.ifBlank { "Be the first profile others see for an entire day" },
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 31.sp
                ),
                color = Color(0xFF192637),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Text(
                ad.body.ifBlank { "Appear on top of recommendations and increase your chances of getting more interests." },
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF273849),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(30.dp))
            Button(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(ad.cta.ifBlank { "Get Spotlight" }, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun HomeScamAwarenessInsertCard(
    ad: HomeBestMatchAdData,
    cards: List<HomeScamAwarenessCardData>,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit
) {
    val awarenessCards = cards
        .ifEmpty { com.soulmatch.app.data.models.defaultScamAwarenessCards() }
        .filter { it.enabled }
        .ifEmpty { com.soulmatch.app.data.models.defaultScamAwarenessCards() }
    var selectedIndex by rememberSaveable(awarenessCards.size) { mutableStateOf(0) }
    val active = awarenessCards[selectedIndex.coerceIn(0, awarenessCards.lastIndex)]
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4D9)),
        border = BorderStroke(1.dp, Color(0xFFF7E0AD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    ad.badge.ifBlank { "Scam awareness" },
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF728091),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    ad.title.ifBlank { "Protect yourself from online frauds" },
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 31.sp
                    ),
                    color = Color(0xFF304457)
                )
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Divider.copy(alpha = 0.5f)),
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFFFF1EF)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Verified, contentDescription = null, tint = HomePrimary, modifier = Modifier.size(54.dp))
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        active.title.ifBlank { "Stay alert before sending money" },
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF304457),
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        active.body.ifBlank { "Verify every request through family and report suspicious activity." },
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF42566B)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                awarenessCards.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(9.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (index == selectedIndex) Color(0xFF9AA6B2) else Color(0xFFE3E8ED))
                            .clickable { selectedIndex = index }
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpen),
                color = Color.White.copy(alpha = 0.64f)
            ) {
                Text(
                    "Visit our safety centre to learn how to stay safe",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5D6B7B),
                    textAlign = TextAlign.Center
                )
            }
        }
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
        containerColor = Color(0xFFFFF1E8),
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
    val palette = homeAdPalette(ad.type, ad.theme)
    PremiumCard(
        modifier = modifier,
        containerColor = palette.container,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = palette.chip,
                        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.75f))
                    ) {
                        Text(
                            (ad.type.ifBlank { "Sponsored" }).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accent,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                    Text(
                        ad.title.ifBlank { "Recommended service" },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = onOpen,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(ad.cta.ifBlank { "Open" })
                }
            }
            Text(
                ad.body.ifBlank { "Explore useful services and profiles selected for matrimony journeys." },
                style = MaterialTheme.typography.bodyMedium,
                color = palette.body,
                maxLines = 3,
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

private fun homeAdPalette(type: String, theme: String = ""): HomeAdPalette {
    val key = theme.ifBlank { type }.lowercase(Locale.getDefault())
    return when (key) {
        "astrology" -> HomeAdPalette(
            container = Color(0xFFF9F0FF),
            border = Color(0xFFD8B2FF),
            chip = Color(0xFFEEDCFF),
            accent = Color(0xFF6D28D9),
            title = Color(0xFF3B136F),
            body = Color(0xFF5E3E85)
        )
        "profiles" -> HomeAdPalette(
            container = Color(0xFFEFFAF5),
            border = Color(0xFFB7E6CB),
            chip = Color(0xFFD9F4E6),
            accent = Color(0xFF047857),
            title = Color(0xFF114B3A),
            body = Color(0xFF45695D)
        )
        "gold", "sunrise" -> HomeAdPalette(
            container = Color(0xFFFFF3D6),
            border = Color(0xFFE7C45A),
            chip = Color(0xFFFFE69A),
            accent = Color(0xFF8B6500),
            title = Color(0xFF6A3600),
            body = Color(0xFF725A24)
        )
        "dark", "maroon" -> HomeAdPalette(
            container = Color(0xFF7A0026),
            border = Color(0xFFFFD77A),
            chip = Color(0x33FFFFFF),
            accent = Color(0xFFFFE39A),
            title = Color.White,
            body = Color(0xFFFFE4EA)
        )
        "blue" -> HomeAdPalette(
            container = Color(0xFFEAF4FF),
            border = Color(0xFFB9D6F5),
            chip = Color(0xFFDCEEFF),
            accent = Color(0xFF2563A8),
            title = Color(0xFF183B66),
            body = Color(0xFF3E5873)
        )
        "peach", "cream", "ivory" -> HomeAdPalette(
            container = Color(0xFFFFF6EC),
            border = Color(0xFFEAC9B7),
            chip = Color(0xFFFFE8D9),
            accent = Color(0xFF9B4A34),
            title = Color(0xFF6D2418),
            body = Color(0xFF704C43)
        )
        else -> HomeAdPalette(
            container = Color(0xFFFFF1F4),
            border = Color(0xFFF2C1D1),
            chip = Color(0xFFFFE1EA),
            accent = Color(0xFFB4235A),
            title = Color(0xFF6D1031),
            body = Color(0xFF70414E)
        )
    }
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

private fun homeBestMatchAdCards(
    content: HomeContentData,
    subscription: SubscriptionData,
    assistEnabled: Boolean
): List<HomeBestMatchAdData> {
    return content.bestMatchAdCards
        .ifEmpty { defaultHomeBestMatchAds() }
        .filter { it.enabled && (it.title.isNotBlank() || it.body.isNotBlank()) }
        .filterNot { assistEnabled && it.type.equals("assist", ignoreCase = true) }
        .filter { it.isEligibleForPlan(subscription) }
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
        "free", "bronze" -> 0
        "silver" -> 1_000
        "gold" -> 2_000
        "platinum" -> CANONICAL_PLATINUM_RANK
        else -> 0
    }
}

private fun HomeBestMatchAdData.isUpgradeType(): Boolean {
    val normalizedType = type.lowercase(Locale.getDefault())
    return normalizedType in setOf("upgrade", "membership", "subscription")
}

private fun HomeBestMatchAdData.isEligibleForPlan(subscription: SubscriptionData): Boolean {
    val currentPlan = normalizedMemberPlan(subscription.planId)
    val currentRank = canonicalPlanRank(currentPlan)
    val explicitTargets = targetPlans.map { normalizedMemberPlan(it) }.filter { it.isNotBlank() }
    if (explicitTargets.isNotEmpty() && currentPlan !in explicitTargets) return false

    val minRank = minPlan.takeIf { it.isNotBlank() }?.let { canonicalPlanRank(it) }
    val maxRank = maxPlan.takeIf { it.isNotBlank() }?.let { canonicalPlanRank(it) }
    if (minRank != null && currentRank < minRank) return false
    if (maxRank != null && currentRank > maxRank) return false
    return true
}

private fun normalizedMemberPlan(planId: String): String {
    val normalized = planId.ifBlank { "free" }
        .lowercase(Locale.getDefault())
        .replace("bronze", "free")
    return normalized.ifBlank { "free" }
}

@Composable
private fun PartnerPreferenceReminderBanner(
    isConfigured: Boolean,
    onOpen: () -> Unit
) {
    PremiumCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        containerColor = if (isConfigured) Color(0xFFF8F5EC) else Color(0xFFFFF0F3),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    if (isConfigured) "Partner Preferences Selected" else "Set Your Partner Preferences",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryDark
                )
                Text(
                    if (isConfigured) {
                        "Review or fine-tune your age, community, and horoscope preferences before exploring more matches."
                    } else {
                        "Add age, religion, and manglik preference to improve match ranking."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (isConfigured) {
                    Text(
                        "Selected and active for your current match feed.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8D6B20),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Button(onClick = onOpen) {
                Text(if (isConfigured) "Review" else "Update")
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
                    color = HomePrimary,
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
    onMarkViewed: (String) -> Unit,
    onInterest: (String) -> Unit,
    onShortlist: (String) -> Unit,
    onIgnore: (String) -> Unit,
    onRequestPhoto: (String) -> Unit,
    onChat: (String, String) -> Unit,
    onOpenSubscription: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAstrology: () -> Unit,
    onOpenDestination: (String) -> Unit
) {
    val slots = remember(profiles, content, showUpgrade, showAds, adCards) {
        buildBestMatchSlots(
            profiles = profiles,
            content = content,
            hasInsertContent = showUpgrade || (showAds && adCards.isNotEmpty())
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        slots.forEach { slot ->
            if (slot.profile != null) {
                HomeMatchCard(
                    profile = slot.profile,
                    modifier = Modifier.fillMaxWidth(),
                    onOpen = {
                        if (slot.profile.profileId.isNotBlank()) {
                            onMarkViewed(slot.profile.profileId)
                            onViewProfile(slot.profile.profileId)
                        }
                    },
                    onInterest = { if (slot.profile.profileId.isNotBlank()) onInterest(slot.profile.profileId) },
                    onShortlist = { if (slot.profile.profileId.isNotBlank()) onShortlist(slot.profile.profileId) },
                    onIgnore = { if (slot.profile.profileId.isNotBlank()) onIgnore(slot.profile.profileId) },
                    onRequestPhoto = { if (slot.profile.profileId.isNotBlank()) onRequestPhoto(slot.profile.profileId) },
                    onChat = {
                        if (slot.profile.userId.isNotBlank()) {
                            onChat(slot.profile.userId, slot.profile.name)
                        }
                    }
                )
            } else {
                BestMatchInsertCard(
                    slot = slot.insertSlot,
                    adCards = adCards,
                    scamCards = content.scamAwarenessCards,
                    showUpgrade = showUpgrade,
                    showAds = showAds,
                    upgradeTitle = content.upgradeTitle,
                    upgradeDetail = content.upgradeDetail,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 5f),
                    onOpenSubscription = onOpenSubscription,
                    onOpenSearch = onOpenSearch,
                    onOpenAstrology = onOpenAstrology,
                    onOpenDestination = onOpenDestination
                )
            }
        }
    }
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
    onShortlist: () -> Unit,
    onIgnore: () -> Unit,
    onRequestPhoto: () -> Unit,
    onChat: () -> Unit
) {
    val photoIsPrivate = profile.isPhotoPrivate
    val hasPhoto = !profile.primaryPhoto.isNullOrBlank()
    val shouldBlurPhoto = photoIsPrivate && hasPhoto
    val shouldRequestPhoto = photoIsPrivate && !hasPhoto
    val showActivity = !profile.hideLastSeen && profile.lastActiveLabel.isNotBlank()
    val premiumBorder = profile.trustScore >= 75 || profile.compatibilityScore >= 90
    val heroBadge = profileHeroBadge(profile)
    val membershipBadge = profileMembershipBadge(profile)
    Card(
        modifier = modifier
            .aspectRatio(0.60f)
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (premiumBorder) BorderStroke(2.5.dp, Color(0xFFE64545)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (shouldRequestPhoto) {
                PrivatePhotoPlaceholder(
                    name = profile.name,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                MemberPhoto(
                    photoUrl = profile.primaryPhoto,
                    contentDescription = "Photo of ${profile.name}",
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (shouldBlurPhoto) Modifier.blur(14.dp) else Modifier),
                    shape = RoundedCornerShape(28.dp)
                )
            }
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
            if (shouldRequestPhoto) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clickable(onClick = onRequestPhoto),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.3.dp, Color.White.copy(alpha = 0.88f))
                ) {
                    Text(
                        "Request photo",
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            if (shouldBlurPhoto) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    Text(
                        "Photo visible on acceptance of interest",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
            if (!photoIsPrivate) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = Color.Black.copy(alpha = 0.32f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text(profilePhotoCount(profile).toString(), style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            if (heroBadge.isNotBlank()) {
                MatchHeroBadge(
                    label = heroBadge,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 18.dp, end = 18.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (showActivity) {
                        Text(
                            normalizeActivityLabel(profile.lastActiveLabel),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (membershipBadge.isNotBlank()) {
                        PremiumProfileBadge(label = membershipBadge)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            listOf(profile.name.removeSuffix("."), profile.age.takeIf { it > 0 }?.toString())
                                .filterNotNull()
                                .joinToString(", "),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 30.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (profile.isVerified) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Filled.Verified, contentDescription = "Verified", tint = Color(0xFF3CA3FF), modifier = Modifier.size(27.dp))
                        }
                    }
                    Text(
                        listOf(profile.heightCm?.let(::formatHeightLabel), profile.location, profile.community)
                            .filterNotNull()
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
                            .ifBlank { "Height, location, and community in progress" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.94f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        listOf(profile.occupation, profileIncomeLabel(profile.annualIncome))
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
                            .ifBlank { "Profession and income in progress" },
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        profile.education.ifBlank { "Education in progress" },
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    "Profile managed by ${profileOwnerLabel(profile.profileCreatedBy)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontStyle = FontStyle.Italic),
                    color = Color.White.copy(alpha = 0.92f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MatchCardAction(
                        label = if (profile.interestSent) "Sent" else "Interest",
                        selected = profile.interestSent,
                        selectedContentDescription = "Interest sent",
                        unselectedContentDescription = "Send interest",
                        onClick = onInterest
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                    MatchCardAction(
                        label = "Shortlist",
                        selected = profile.shortlisted,
                        selectedContentDescription = "Shortlisted",
                        unselectedContentDescription = "Shortlist profile",
                        onClick = onShortlist
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    MatchCardAction(
                        label = "Ignore",
                        selected = false,
                        selectedContentDescription = "Profile hidden",
                        unselectedContentDescription = "Ignore profile",
                        onClick = onIgnore
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                    MatchCardAction(
                        label = "Chat",
                        selected = false,
                        selectedContentDescription = "Open chat",
                        unselectedContentDescription = "Open chat",
                        onClick = onChat
                    ) {
                        Icon(Icons.Filled.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(27.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivatePhotoPlaceholder(
    name: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF15202B), Color(0xFF05080C))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(210.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.08f))
        )
        Text(
            name.take(1).ifBlank { "S" },
            style = MaterialTheme.typography.displayLarge.copy(fontFamily = FontFamily.Serif),
            color = Color.White.copy(alpha = 0.11f),
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun MatchHeroBadge(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.94f),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (label.contains("compatible", ignoreCase = true)) Icons.Filled.Favorite else Icons.Filled.Send,
                contentDescription = null,
                tint = Color(0xFF1F2937),
                modifier = Modifier.size(16.dp)
            )
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(fontStyle = FontStyle.Italic),
                color = Color(0xFF1F2937),
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PremiumProfileBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))
    ) {
        Box(
            modifier = Modifier
                .background(Brush.horizontalGradient(listOf(Color(0xFFD94352), Color(0xFFB0448E))))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ShortlistedRibbonBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 14.dp, bottomEnd = 14.dp),
        color = HomePrimary.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
    ) {
        Text(
            "SHORTLISTED",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun profileHeroBadge(profile: ProfileSummary): String {
    return when {
        profile.compatibilityScore >= 94 -> "Most Compatible"
        profile.joinedToday() -> "Just joined"
        else -> ""
    }
}

private fun profileMembershipBadge(profile: ProfileSummary): String {
    return when {
        profile.trustScore >= 88 -> "Pro Max"
        profile.trustScore >= 72 || profile.compatibilityScore >= 88 -> "Pro"
        else -> ""
    }
}

private fun profilePhotoCount(profile: ProfileSummary): Int =
    when {
        profile.primaryPhoto.isNullOrBlank() -> 0
        profile.compatibilityScore >= 94 -> 4
        else -> 1
    }

private fun normalizeActivityLabel(value: String): String =
    when {
        value.contains("yesterday", ignoreCase = true) -> "Active Yesterday"
        value.contains("today", ignoreCase = true) || value.equals("active", ignoreCase = true) -> "Active Today"
        value.isBlank() -> ""
        else -> value.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

private fun profileIncomeLabel(income: String): String {
    val clean = income.trim()
    if (clean.isBlank()) return ""
    if (clean.contains("earn", ignoreCase = true) || clean.contains("income", ignoreCase = true)) return clean
    return "Earns $clean"
}

@Composable
private fun MatchInfoTags(matchScore: Int, trustScore: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MatchMetricTag("$matchScore% Match")
        MatchMetricTag("$trustScore% Trust")
    }
}

@Composable
private fun MatchMetricTag(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.Black.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge.copy(fontStyle = FontStyle.Italic),
            color = Color.White.copy(alpha = 0.94f),
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}

@Composable
private fun MatchCardAction(
    label: String,
    selected: Boolean,
    selectedContentDescription: String,
    unselectedContentDescription: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(58.dp)
                .semantics {
                    contentDescription = if (selected) selectedContentDescription else unselectedContentDescription
                }
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(999.dp),
            color = if (selected) HomePrimary else Color(0xFF4A4042).copy(alpha = 0.78f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                icon()
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

private fun profileOwnerLabel(profileCreatedBy: String): String =
    when (profileCreatedBy.lowercase(Locale.getDefault()).replace("_", " ").trim()) {
        "", "self" -> "Self"
        "parent" -> "Parent"
        "sibling" -> "Sibling"
        "relative", "relative friend", "relative/friend", "friend" -> "Relative/Friend"
        "agent", "advisor" -> "Agent"
        else -> profileCreatedBy.replace("_", " ").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }

private const val MAX_HOME_MATCHES = 80

private fun formatHeightLabel(heightCm: Int): String {
    val totalInches = (heightCm / 2.54).toInt()
    val feet = totalInches / 12
    val inches = totalInches % 12
    return if (feet > 0) "$feet ft $inches in" else "$heightCm cm"
}

private fun educationOccupationLocation(profile: ProfileSummary): String {
    return listOf(profile.education, profile.occupation, profile.location)
        .filter { it.isNotBlank() }
        .joinToString(" | ")
        .ifBlank { "Education, occupation, and location in progress" }
}

private fun List<ProfileSummary>.filterForHomeFeed(
    filter: HomeMatchFeedFilter?,
    viewerProfile: com.soulmatch.app.data.models.ProfileData,
    viewedProfileIds: Set<String>,
    advancedFilters: HomeAdvancedFilters
): List<ProfileSummary> {
    val viewerCity = viewerProfile.workingCity
        .ifBlank { viewerProfile.familyCity }
        .trim()
    return filter { profile ->
        val quickFilterMatch = when (filter) {
            null -> true
            HomeMatchFeedFilter.Viewed -> profile.profileId in viewedProfileIds
            HomeMatchFeedFilter.JustJoined -> profile.joinedToday()
            HomeMatchFeedFilter.Nearby -> viewerCity.isNotBlank() &&
                profile.matchesAnyText(viewerCity, profile.location, profile.familyCity)
        }
        quickFilterMatch &&
            profile.matchesTypeFilter(advancedFilters.typeOfMatches, viewerCity, viewedProfileIds) &&
            profile.matchesTextFilter(advancedFilters.religion, profile.religion, profile.community) &&
            profile.matchesOnlineStatus(advancedFilters.onlineStatus) &&
            profile.matchesTextFilter(advancedFilters.familyBasedOutOf, profile.familyCity, profile.familyState) &&
            profile.matchesProfilePostedBy(advancedFilters.profilePostedBy) &&
            profile.matchesActivityFilter(advancedFilters.activityOnSite) &&
            profile.matchesCountryFilter(advancedFilters.country) &&
            profile.matchesTextFilter(advancedFilters.city, profile.location, profile.familyCity) &&
            profile.matchesTextFilter(advancedFilters.income, profile.annualIncome) &&
            profile.matchesTextFilter(advancedFilters.educationLevel, profile.education) &&
            profile.matchesTextFilter(advancedFilters.employedIn, profile.occupation) &&
            profile.matchesTextFilter(advancedFilters.occupation, profile.occupation) &&
            profile.matchesPhotoFilter(advancedFilters.photo) &&
            profile.matchesHeightBand(advancedFilters.heightBand) &&
            profile.matchesAgeBand(advancedFilters.ageBand) &&
            profile.matchesTextFilter(advancedFilters.maritalStatus, profile.maritalStatus) &&
            profile.matchesHoroscopeFilter(advancedFilters.horoscope) &&
            profile.matchesManglikFilter(advancedFilters.manglik) &&
            profile.matchesTextFilter(advancedFilters.diet, profile.diet) &&
            profile.compatibilityScore >= advancedFilters.minimumMatch &&
            profile.trustScore >= advancedFilters.minimumTrust
    }
}

private fun ProfileSummary.matchesAgeBand(ageBand: String): Boolean {
    if (ageBand == "All" || age <= 0) return true
    return when (ageBand) {
        "21-25" -> age in 21..25
        "26-30" -> age in 26..30
        "31-35" -> age in 31..35
        "36-40" -> age in 36..40
        "41+" -> age >= 41
        else -> true
    }
}

private fun buildHomeFilterOptions(profiles: List<ProfileSummary>): HomeFilterOptions {
    fun unique(values: List<String>): List<String> = values
        .flatMap { it.split(",", "|", "/", "·") }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase(Locale.getDefault()) }
        .sorted()
    return HomeFilterOptions(
        religions = unique(profiles.map { it.religion.ifBlank { it.community } }),
        familyBases = unique(profiles.flatMap { listOf(it.familyCity, it.familyState) }),
        cities = unique(profiles.flatMap { listOf(it.location, it.familyCity) }),
        incomes = unique(profiles.map { it.annualIncome }),
        educationLevels = unique(profiles.map { it.education }),
        occupations = unique(profiles.map { it.occupation }),
        diets = unique(profiles.map { it.diet })
    )
}

private fun ProfileSummary.matchesTypeFilter(type: String, viewerCity: String, viewedProfileIds: Set<String>): Boolean {
    return when (type) {
        "Viewed" -> profileId in viewedProfileIds
        "Verified" -> isVerified
        "Just Joined" -> joinedToday()
        "Nearby" -> viewerCity.isNotBlank() && matchesAnyText(viewerCity, location, familyCity)
        else -> true
    }
}

private fun quickFilterForType(type: String): HomeMatchFeedFilter? {
    return when (type) {
        HomeMatchFeedFilter.Viewed.label -> HomeMatchFeedFilter.Viewed
        HomeMatchFeedFilter.JustJoined.label -> HomeMatchFeedFilter.JustJoined
        HomeMatchFeedFilter.Nearby.label -> HomeMatchFeedFilter.Nearby
        else -> null
    }
}

private fun ProfileSummary.matchesOnlineStatus(status: String): Boolean {
    if (status == "All" || hideLastSeen) return true
    val label = lastActiveLabel.lowercase(Locale.getDefault())
    return when (status) {
        "Online" -> label.contains("online") || label == "active"
        "Recently Active" -> label.contains("recent") || label.contains("today") || label.contains("week") || label == "active"
        "Inactive" -> label.contains("inactive") || label.contains("month") || label.contains("ago")
        else -> true
    }
}

private fun ProfileSummary.matchesActivityFilter(activity: String): Boolean {
    if (activity == "All") return true
    val label = listOf(lastActiveLabel, createdAt).joinToString(" ").lowercase(Locale.getDefault())
    return when (activity) {
        "Today" -> joinedToday() || label.contains("today") || label.contains("active")
        "This Week" -> joinedToday() || label.contains("week") || label.contains("today") || label.contains("recent")
        "This Month" -> label.contains("month") || label.contains("week") || label.contains("today") || label.contains("recent")
        else -> true
    }
}

private fun ProfileSummary.matchesCountryFilter(country: String): Boolean {
    return when (country) {
        "All" -> true
        "India" -> true
        "Abroad" -> location.contains("abroad", ignoreCase = true) || familyState.contains("abroad", ignoreCase = true)
        else -> true
    }
}

private fun ProfileSummary.matchesProfilePostedBy(value: String): Boolean {
    if (value == "All") return true
    return profileOwnerLabel(profileCreatedBy).equals(value, ignoreCase = true) ||
        profileCreatedBy.equals(value, ignoreCase = true)
}

private fun ProfileSummary.matchesPhotoFilter(value: String): Boolean {
    return when (value) {
        "Visible Photos" -> !isPhotoPrivate && !primaryPhoto.isNullOrBlank()
        "Request Photo" -> isPhotoPrivate
        else -> true
    }
}

private fun ProfileSummary.matchesHeightBand(heightBand: String): Boolean {
    val cm = heightCm ?: return heightBand == "All"
    val inches = (cm / 2.54).toInt()
    return when (heightBand) {
        "Below 5 ft" -> inches < 60
        "5 ft - 5 ft 5 in" -> inches in 60..65
        "5 ft 6 in - 6 ft" -> inches in 66..72
        "Above 6 ft" -> inches > 72
        else -> true
    }
}

private fun ProfileSummary.matchesHoroscopeFilter(value: String): Boolean {
    return when (value) {
        "Available" -> isManglik || matchReasons.any { it.contains("horoscope", ignoreCase = true) }
        "Not Available" -> !isManglik && matchReasons.none { it.contains("horoscope", ignoreCase = true) }
        else -> true
    }
}

private fun ProfileSummary.matchesManglikFilter(value: String): Boolean {
    return when (value) {
        "Manglik" -> isManglik
        "Non-Manglik" -> !isManglik
        else -> true
    }
}

private fun ProfileSummary.matchesTextFilter(selected: String, vararg fields: String): Boolean {
    if (selected == "All") return true
    return matchesAnyText(selected, *fields)
}

private fun ProfileSummary.matchesAnyText(needle: String, vararg fields: String): Boolean {
    val query = needle.trim()
    if (query.isBlank()) return true
    return fields.any { it.contains(query, ignoreCase = true) }
}

private fun ProfileSummary.joinedToday(): Boolean {
    val value = createdAt.ifBlank { lastActiveLabel }.lowercase(Locale.getDefault())
    val todayPrefix = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
    return value.contains("today") ||
        value.startsWith(todayPrefix) ||
        lastActiveLabel.contains("just joined", ignoreCase = true) ||
        lastActiveLabel.contains("new today", ignoreCase = true)
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
            .width(168.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SoftBorder.copy(alpha = 0.32f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            MemberPhoto(
                photoUrl = profile.primaryPhoto,
                contentDescription = profile.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f),
                shape = RoundedCornerShape(18.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "${profile.name.removeSuffix(".")}, ${profile.age}",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            ),
            color = Color(0xFF8F5D69),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            listOf(profile.education, profile.occupation, profile.location)
                .filter { it.isNotBlank() }
                .joinToString(" • ")
                .ifBlank { "Details in progress" },
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF594045),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
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
                        color = HomePrimary,
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
                "No interests received yet",
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
                Text("Browse matches", color = HomePrimary, fontWeight = FontWeight.ExtraBold)
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

package com.soulmatch.app.ui.screens.profile

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.soulmatch.app.R
import com.soulmatch.app.data.models.PartnerPreferencesData
import com.soulmatch.app.data.models.AssistStatusData
import com.soulmatch.app.data.models.FamilyDecisionData
import com.soulmatch.app.data.models.PhotoAccessRequestData
import com.soulmatch.app.data.models.ProfileData
import com.soulmatch.app.data.models.ProfilePhoto
import com.soulmatch.app.data.models.SubscriptionData
import com.soulmatch.app.data.models.TrustFactorData
import com.soulmatch.app.data.models.VerificationRequestData
import com.soulmatch.app.data.models.ViewerData
import com.soulmatch.app.data.models.fullName
import com.soulmatch.app.ui.components.AvatarInitial
import com.soulmatch.app.ui.components.ChipTone
import com.soulmatch.app.ui.components.FilterChoiceChip
import com.soulmatch.app.ui.components.LabeledProgress
import com.soulmatch.app.ui.components.MemberPhoto
import com.soulmatch.app.ui.components.MetricPill
import com.soulmatch.app.ui.components.PremiumCard
import com.soulmatch.app.ui.components.PremiumHeader
import com.soulmatch.app.ui.components.PremiumScreen
import com.soulmatch.app.ui.components.ProfileStrengthAdvisor
import com.soulmatch.app.ui.components.SectionTitle
import com.soulmatch.app.ui.components.SignalChip
import com.soulmatch.app.ui.components.SignalChips
import com.soulmatch.app.ui.components.UpgradePlanGate
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
import com.soulmatch.app.ui.titleCase
import com.soulmatch.app.ui.viewmodels.MyProfileViewModel
import com.soulmatch.app.ui.viewmodels.ProfileChecklistItem
import java.util.Locale
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    onBack: () -> Unit = {},
    onViewProfile: ((String) -> Unit)? = null,
    onOpenChat: ((String, String) -> Unit)? = null,
    onSubscribe: (() -> Unit)? = null,
    onEditSection: ((Int) -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onOpenAssist: (() -> Unit)? = null,
    onOpenPartnerPreferences: (() -> Unit)? = null,
    onOpenTrustDetails: (() -> Unit)? = null,
    onOpenFamilyBoard: (() -> Unit)? = null,
    profileId: String = "",
    chatId: String = "",
    participantName: String = "",
    vm: MyProfileViewModel = hiltViewModel()
) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val subscription by vm.subscription.collectAsStateWithLifecycle()
    val checklist by vm.checklist.collectAsStateWithLifecycle()
    val preferences by vm.preferences.collectAsStateWithLifecycle()
    val assistStatus by vm.assistStatus.collectAsStateWithLifecycle()
    val viewers by vm.viewers.collectAsStateWithLifecycle()
    val photos by vm.photos.collectAsStateWithLifecycle()
    val verifications by vm.verifications.collectAsStateWithLifecycle()
    val photoAccessRequests by vm.photoAccessRequests.collectAsStateWithLifecycle()
    val familyDecisions by vm.familyDecisions.collectAsStateWithLifecycle()
    val loading by vm.isLoading.collectAsStateWithLifecycle()
    val uploadingPhotos by vm.isUploadingPhotos.collectAsStateWithLifecycle()
    val submittingVerification by vm.isSubmittingVerification.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val loadMessage by vm.loadMessage.collectAsStateWithLifecycle()
    val editSection: (Int) -> Unit = onEditSection ?: { _ -> }
    val openSettings: () -> Unit = onOpenSettings ?: {}
    val openPartnerPreferences: () -> Unit = onOpenPartnerPreferences ?: {}
    val openTrustDetails: () -> Unit = onOpenTrustDetails ?: {}
    val openFamilyBoard: () -> Unit = onOpenFamilyBoard ?: {}
    val openSubscription: () -> Unit = onSubscribe ?: {}
    val toggleAssist: (Boolean) -> Unit = { enabled ->
        vm.updateAssistStatus(
            isOptedIn = enabled,
            supportLevel = if (enabled) "advisor_assisted" else "self_service",
            preferredContactWindow = assistStatus.preferredContactWindow,
            familyContactName = assistStatus.familyContactName,
            familyContactPhone = assistStatus.familyContactPhone,
            notes = assistStatus.notes
        )
    }
    val viewProfile: (String) -> Unit = onViewProfile ?: { _ -> }
    val context = LocalContext.current
    var localPhotoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showRecentViewers by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            localPhotoUris = (localPhotoUris + uris)
                .distinctBy { it.toString() }
                .take(8)
        }
        val parts = uris.mapIndexedNotNull { index, uri -> context.toPhotoPart(uri, index) }
        if (parts.isNotEmpty()) {
            vm.uploadPhotos(parts)
        }
    }

    LaunchedEffect(status) {
        if (status != null) {
            if (status?.contains("uploaded", ignoreCase = true) == true) {
                localPhotoUris = emptyList()
            }
            delay(3000)
            vm.clearStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = openSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
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
            if (loading && profile == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (profile == null) {
                EmptyProfileState(
                    message = loadMessage ?: "We couldn't load your saved details yet. Start with basic details or retry once the server is reachable.",
                    checklist = checklist,
                    onEditSection = editSection,
                    onRetry = { vm.load() }
                )
            } else {
                profile?.let { data ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            ProfileOwnerHeader(
                                profile = data,
                                subscription = subscription,
                                photos = photos,
                                localPhotoUris = localPhotoUris,
                                checklist = checklist,
                                onSubscribe = openSubscription,
                                onSettings = openSettings,
                                onUploadPhoto = { photoPicker.launch("image/*") }
                            )
                        }
                        if (!status.isNullOrBlank()) {
                            item {
                                StatusCard(status = status ?: "")
                            }
                        }
                        item {
                            ProfileStrengthOverviewCard(
                                profile = data,
                                checklist = checklist,
                                onComplete = { editSection(checklist.firstOrNull { !it.isComplete }?.editStep ?: 1) }
                            )
                        }
                        item {
                            ProfileQuickStatsRow(
                                checklist = checklist,
                                photos = photos,
                                subscription = subscription
                            )
                        }
                        item {
                            SectionTitle(
                                title = "Profile Details",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(checklist, key = { it.title }) { item ->
                            ChecklistRow(item = item, onEdit = { editSection(item.editStep) })
                        }
                        item {
                            TrustDetailsCard(
                                profile = data,
                                onOpenTrustDetails = openTrustDetails
                            )
                        }
                        item {
                            PartnerPreferencesSummaryCard(
                                preferences = preferences,
                                onEdit = openPartnerPreferences
                            )
                        }
                        item {
                            SoulMatchAssistProfileCard(
                                assistStatus = assistStatus,
                                onToggleAssist = toggleAssist
                            )
                        }
                        if (!data.verificationStatus.equals("verified", ignoreCase = true)) {
                            item {
                                VerificationStatusCard(
                                    profile = data,
                                    photos = photos,
                                    verifications = verifications,
                                    isSubmitting = submittingVerification,
                                    onSubmit = vm::submitProfileVerification,
                                    onCompleteProfile = { editSection(checklist.firstOrNull { !it.isComplete }?.editStep ?: 1) },
                                    onAddPhoto = { photoPicker.launch("image/*") }
                                )
                            }
                        }
                        item {
                            PhotoGalleryCard(
                                photos = photos,
                                localPhotoUris = localPhotoUris,
                                uploadingPhotos = uploadingPhotos,
                                onUpload = { photoPicker.launch("image/*") },
                                onDeleteLocal = { uri ->
                                    localPhotoUris = localPhotoUris.filterNot { it == uri }
                                },
                                onDelete = vm::deletePhoto,
                                onSetPrimary = vm::setPrimaryPhoto
                            )
                        }
                        item {
                            PhotoAccessRequestsCard(
                                requests = photoAccessRequests,
                                onApprove = { requestId -> vm.respondPhotoAccessRequest(requestId, approved = true) },
                                onDecline = { requestId -> vm.respondPhotoAccessRequest(requestId, approved = false) }
                            )
                        }
                        item {
                            RecentViewersToggleCard(
                                count = viewers.size,
                                enabled = showRecentViewers,
                                onToggle = { showRecentViewers = it }
                            )
                        }
                        if (showRecentViewers) {
                            if (viewers.isEmpty()) {
                                item {
                                    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), containerColor = SurfaceSoft) {
                                        Text("No recent viewers yet.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    }
                                }
                            }
                            items(viewers.take(5), key = { "${it.profileId}-${it.viewedAt}" }) { viewer ->
                                ViewerRow(viewer = viewer, onOpen = { viewProfile(viewer.profileId) })
                            }
                        }
                        item {
                            PrivacySettingsCard(
                                profile = data,
                                onUpdatePhotoPrivacy = vm::updatePrivacySettings
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerPreferencesScreen(
    onBack: () -> Unit,
    vm: MyProfileViewModel = hiltViewModel()
) {
    val preferences by vm.preferences.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Partner preferences", fontWeight = FontWeight.Bold) },
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    PremiumCard(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        containerColor = SurfaceWarm
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle(
                                title = "Set match filters",
                                subtitle = "Fill each tab carefully. These preferences are used by Best Matches, Search ranking, and future assisted matchmaking."
                            )
                            SignalChips(
                                labels = listOf("Basics", "Career", "Lifestyle", "Family", "Horoscope"),
                                tone = ChipTone.Info
                            )
                        }
                    }
                }
                if (!status.isNullOrBlank()) {
                    item { StatusCard(status = status ?: "") }
                }
                item {
                    PartnerPreferencesCard(
                        preferences = preferences,
                        onSave = vm::updatePartnerPreferences
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrustDetailsScreen(
    onBack: () -> Unit,
    vm: MyProfileViewModel = hiltViewModel()
) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val visibleFactors = profile?.trustFactors.orEmpty().filterNot { it.isFirebaseTrustFactor() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trust details", fontWeight = FontWeight.Bold) },
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    PremiumCard(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            TrustMetricTile(
                                label = "Trust Score",
                                value = "${profile?.trustScore?.coerceIn(0, 100) ?: 0}%",
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "Each item below shows the member-visible verification and profile quality signals used for this score.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
                if (visibleFactors.isEmpty()) {
                    item {
                        PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), containerColor = SurfaceSoft) {
                            Text("Trust details will appear after verification signals are available.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                } else {
                    items(visibleFactors, key = { "${it.key}-${it.label}" }) { factor ->
                        TrustFactorStatusRow(
                            label = factor.label,
                            detail = factor.detail,
                            status = factor.status
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyProfileState(
    message: String,
    checklist: List<ProfileChecklistItem>,
    onEditSection: (Int) -> Unit,
    onRetry: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PremiumCard(containerColor = SurfaceWarm) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Your profile needs attention", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { onEditSection(1) }, modifier = Modifier.weight(1f)) {
                            Text("Open step 1")
                        }
                        OutlinedButton(onClick = onRetry, modifier = Modifier.weight(1f)) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
        item {
            SectionTitle("Profile checklist", modifier = Modifier.padding(top = 8.dp))
        }
        items(checklist, key = { it.title }) { item ->
            ChecklistRow(item = item, onEdit = { onEditSection(item.editStep) })
        }
    }
}

@Composable
private fun ProfileOwnerHeader(
    profile: ProfileData,
    subscription: SubscriptionData,
    photos: List<ProfilePhoto>,
    localPhotoUris: List<Uri>,
    checklist: List<ProfileChecklistItem>,
    onSubscribe: () -> Unit,
    onSettings: () -> Unit,
    onUploadPhoto: () -> Unit
) {
    val headerPhoto = localPhotoUris.firstOrNull()?.toString()
        ?: photos.firstOrNull { it.isPrimary }?.photoUrl
        ?: photos.firstOrNull()?.photoUrl
        ?: profile.primaryPhotoUrl
    val planLabel = subscription.displayPlanName()
    val memberLabel = if (subscription.hasActivePaidPlan()) {
        "${planLabel.uppercase(Locale.getDefault())} MEMBER"
    } else {
        "STANDARD MEMBER"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box {
            Surface(
                modifier = Modifier
                    .size(132.dp)
                    .clickable(onClick = onUploadPhoto),
                shape = RoundedCornerShape(999.dp),
                color = SurfaceSoft,
                border = BorderStroke(4.dp, Color(0xFFE9E1DC)),
                shadowElevation = 8.dp
            ) {
                MemberPhoto(
                    photoUrl = headerPhoto,
                    contentDescription = "Profile photo. Tap to upload",
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxSize(),
                    shape = RoundedCornerShape(999.dp)
                )
            }
            if (profile.verificationStatus.equals("verified", ignoreCase = true)) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(34.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(2.dp, Color.White)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Verified, contentDescription = "Verified profile", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(
                profile.fullName(),
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
                color = Color(0xFF1E1B18),
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Text(
                        memberLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
                Text("|", color = TextSecondary)
                Text(
                    "ID: ${formatProfileId(profile.profileId)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ProfileStrengthOverviewCard(
    profile: ProfileData,
    checklist: List<ProfileChecklistItem>,
    onComplete: () -> Unit
) {
    val score = profile.completionScore.coerceIn(0, 100)
    val next = checklist.firstOrNull { !it.isComplete && !it.statusLabel.equals("Optional", ignoreCase = true) }
    PremiumCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(
                    "Profile Strength",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                    color = Color(0xFF1E1B18),
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "$score%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            LinearProgressIndicator(
                progress = score / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = SurfaceSoft
            )
            Text(
                next?.let { "Add your ${it.title.lowercase(Locale.getDefault())} to reach 100% and get better match recommendations." }
                    ?: "Your profile has the key details needed for strong recommendations.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            if (next != null) {
                TextButton(onClick = onComplete) {
                    Text("Complete Now", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileQuickStatsRow(
    checklist: List<ProfileChecklistItem>,
    photos: List<ProfilePhoto>,
    subscription: SubscriptionData
) {
    val completed = checklist.count { it.isComplete }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickStatCard(Icons.Filled.WorkspacePremium, "Sections", "$completed / ${checklist.size}", Modifier.weight(1f))
        QuickStatCard(Icons.Filled.PhotoLibrary, "Photos", if (photos.isEmpty()) "0 Active" else "${photos.size} Active", Modifier.weight(1f))
        QuickStatCard(Icons.Filled.Verified, "Plan", subscription.displayPlanName(), Modifier.weight(1f))
    }
}

@Composable
private fun QuickStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = SurfaceSoft,
        border = BorderStroke(1.dp, Divider.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
            Text(value, style = MaterialTheme.typography.labelMedium, color = Color(0xFF1E1B18), fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MembershipActiveCard(subscription: SubscriptionData) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SuccessSoft,
        border = BorderStroke(1.dp, Success.copy(alpha = 0.24f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Membership active",
                style = MaterialTheme.typography.titleSmall,
                color = Success,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                buildString {
                    append(subscription.displayPlanName())
                    subscription.endDate?.takeIf { it.isNotBlank() }?.let {
                        append(" is valid until ${formatDate(it)}.")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = PrimaryDark
            )
        }
    }
}

private fun SubscriptionData.hasActivePaidPlan(): Boolean =
    isActive && planId.isNotBlank() && !planId.equals("free", ignoreCase = true)

private fun SubscriptionData.displayPlanName(): String {
    val cleanName = planName.trim()
    if (cleanName.isNotBlank()) return cleanName
    return when (planId.lowercase(Locale.getDefault())) {
        "silver" -> "Verified Plus"
        "gold" -> "Family Assist"
        "platinum" -> "Platinum"
        "free", "" -> "Free"
        else -> titleCase(planId.replace('_', ' '))
    }
}

@Composable
private fun ProfilePhotoButton(photoUrl: String?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = Modifier
            .size(64.dp)
            .clickable(onClick = onClick),
        shape = shape,
        color = SurfaceSoft,
        border = BorderStroke(1.dp, Divider)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            MemberPhoto(
                photoUrl = photoUrl,
                contentDescription = "Profile photo. Tap to upload",
                modifier = Modifier
                    .fillMaxSize(),
                shape = shape
            )
            Surface(
                modifier = Modifier.padding(5.dp).size(22.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Filled.PhotoCamera,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusCard(status: String) {
    val isSuccess = status.contains("updated", ignoreCase = true) ||
        status.contains("uploaded", ignoreCase = true) ||
        status.contains("removed", ignoreCase = true) ||
        status.contains("saved", ignoreCase = true) ||
        status.contains("submitted", ignoreCase = true) ||
        status.contains("review", ignoreCase = true)
    PremiumCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        containerColor = if (isSuccess) SuccessSoft else ErrorSoft
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSuccess) Success else Error,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private enum class ProfileVerificationState {
    Verified,
    Pending,
    Rejected,
    NotRequested
}

@Composable
private fun VerificationStatusCard(
    profile: ProfileData,
    photos: List<ProfilePhoto>,
    verifications: List<VerificationRequestData>,
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
    onCompleteProfile: () -> Unit,
    onAddPhoto: () -> Unit
) {
    val latest = verifications.maxByOrNull { it.createdAt }
    val pending = verifications.firstOrNull { it.status.equals("pending", ignoreCase = true) }
    val state = when {
        profile.verificationStatus.equals("verified", ignoreCase = true) ||
            latest?.status.equals("verified", ignoreCase = true) -> ProfileVerificationState.Verified
        pending != null -> ProfileVerificationState.Pending
        latest?.status.equals("rejected", ignoreCase = true) ||
            profile.verificationStatus.equals("rejected", ignoreCase = true) -> ProfileVerificationState.Rejected
        else -> ProfileVerificationState.NotRequested
    }
    val hasPhoto = photos.isNotEmpty() || !profile.primaryPhotoUrl.isNullOrBlank()
    val isCompleteEnough = profile.completionScore >= 60
    val canSubmit = state != ProfileVerificationState.Verified &&
        state != ProfileVerificationState.Pending &&
        !isSubmitting
    val title = when (state) {
        ProfileVerificationState.Verified -> "Verified profile"
        ProfileVerificationState.Pending -> "Verification in review"
        ProfileVerificationState.Rejected -> "Verification needs update"
        ProfileVerificationState.NotRequested -> "Verification not requested"
    }
    val detail = when {
        state == ProfileVerificationState.Verified -> "Your verified badge is active across SoulMatch."
        state == ProfileVerificationState.Pending -> "Admin review is pending. You will get a notification once a decision is made."
        !hasPhoto -> "Add at least one clear profile photo before requesting verification."
        !isCompleteEnough -> "Complete your profile to at least 60% before requesting verification."
        state == ProfileVerificationState.Rejected -> "Update your profile or photos, then request verification again."
        else -> "Request admin review to activate the verified badge on your profile."
    }
    val statusColor = when (state) {
        ProfileVerificationState.Verified -> SuccessSoft
        ProfileVerificationState.Pending -> SurfaceWarm
        ProfileVerificationState.Rejected -> ErrorSoft
        ProfileVerificationState.NotRequested -> SurfaceSoft
    }
    val iconTint = when (state) {
        ProfileVerificationState.Verified -> Success
        ProfileVerificationState.Rejected -> Error
        else -> PrimaryDark
    }
    val buttonLabel = when {
        isSubmitting -> "Submitting"
        state == ProfileVerificationState.Verified -> "Verified"
        state == ProfileVerificationState.Pending -> "Review pending"
        state == ProfileVerificationState.Rejected -> "Request again"
        else -> "Request verification"
    }

    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Profile verification", "Admin-reviewed trust badge for member safety")
            Surface(shape = RoundedCornerShape(16.dp), color = statusColor, border = BorderStroke(1.dp, Divider)) {
                Row(
                    Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = iconTint)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(detail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        if (latest?.status.equals("rejected", ignoreCase = true) && !latest?.reviewNote.isNullOrBlank()) {
                            Text("Admin note: ${latest?.reviewNote}", style = MaterialTheme.typography.bodySmall, color = Error)
                        }
                        if (!latest?.createdAt.isNullOrBlank()) {
                            Text("Last request: ${formatDate(latest?.createdAt ?: "")}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
            }
            Button(
                onClick = onSubmit,
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Text(buttonLabel)
            }
            if (!hasPhoto || !isCompleteEnough) {
                OutlinedButton(
                    onClick = if (!hasPhoto) onAddPhoto else onCompleteProfile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (!hasPhoto) "Add photos first" else "Complete profile first")
                }
            }
        }
    }
}

@Composable
private fun PhotoGalleryCard(
    photos: List<ProfilePhoto>,
    localPhotoUris: List<Uri>,
    uploadingPhotos: Boolean,
    onUpload: () -> Unit,
    onDeleteLocal: (Uri) -> Unit,
    onDelete: (String) -> Unit,
    onSetPrimary: (String) -> Unit
) {
    val primaryLocal = localPhotoUris.firstOrNull()
    val primaryPhoto = photos.firstOrNull { it.isPrimary } ?: photos.firstOrNull()
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Photos", style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif), fontWeight = FontWeight.ExtraBold)
            TextButton(onClick = onUpload, enabled = !uploadingPhotos) {
                Text("Manage Photos", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ProfilePhotoSquare(
                localUri = primaryLocal,
                photoUrl = primaryPhoto?.photoUrl,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
                onClick = onUpload
            )
            AddMorePhotoSquare(
                uploadingPhotos = uploadingPhotos,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
                onUpload = onUpload
            )
        }
        if (photos.size + localPhotoUris.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(localPhotoUris.drop(1), key = { "local-${it}" }) { uri ->
                    LocalPhotoTile(uri = uri, onDelete = onDeleteLocal)
                }
                items(photos.filter { it.photoId != primaryPhoto?.photoId }, key = { it.photoId }) { photo ->
                    PhotoTile(photo = photo, onDelete = onDelete, onSetPrimary = onSetPrimary)
                }
            }
        }
    }
}

@Composable
private fun ProfilePhotoSquare(
    localUri: Uri?,
    photoUrl: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceSoft,
        border = BorderStroke(1.dp, Divider)
    ) {
        if (localUri != null) {
            AsyncImage(
                model = localUri,
                contentDescription = "Profile photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (!photoUrl.isNullOrBlank()) {
            MemberPhoto(
                photoUrl = photoUrl,
                contentDescription = "Profile photo",
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(18.dp)
            )
        } else {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(34.dp))
                Text("Add photo", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun AddMorePhotoSquare(
    uploadingPhotos: Boolean,
    modifier: Modifier = Modifier,
    onUpload: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(enabled = !uploadingPhotos, onClick = onUpload),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (uploadingPhotos) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                Text("Uploading", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            } else {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                Text("Add more", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun PhotoAccessRequestsCard(
    requests: List<PhotoAccessRequestData>,
    onApprove: (String) -> Unit,
    onDecline: (String) -> Unit
) {
    val pending = requests.filter { it.status.equals("pending", ignoreCase = true) }
    val recent = requests.filterNot { it.status.equals("pending", ignoreCase = true) }.take(3)
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = MaterialTheme.colorScheme.surface) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(
                title = "Photo access requests",
                subtitle = if (pending.isEmpty()) "Members who request your private photo will appear here." else "${pending.size} member(s) waiting for your approval"
            )
            if (pending.isEmpty() && recent.isEmpty()) {
                Surface(shape = RoundedCornerShape(16.dp), color = SurfaceSoft, border = BorderStroke(1.dp, Divider)) {
                    Text(
                        "No photo access requests yet.",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
            pending.forEach { request ->
                PhotoAccessRequestRow(
                    request = request,
                    showActions = true,
                    onApprove = { onApprove(request.requestId) },
                    onDecline = { onDecline(request.requestId) }
                )
            }
            recent.forEach { request ->
                PhotoAccessRequestRow(
                    request = request,
                    showActions = false,
                    onApprove = {},
                    onDecline = {}
                )
            }
        }
    }
}

@Composable
private fun TrustDetailsCard(
    profile: ProfileData,
    onOpenTrustDetails: () -> Unit
) {
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Trust Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Tap the score to review profile trust status.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Surface(
                modifier = Modifier.clickable(onClick = onOpenTrustDetails),
                shape = RoundedCornerShape(18.dp),
                color = SurfaceWarm,
                border = BorderStroke(1.dp, Divider)
            ) {
                Text(
                    text = if (profile.trustScore > 0) "${profile.trustScore.coerceIn(0, 100)}%" else "New",
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun VerifiedProfileCard(
    onOpenTrustDetails: () -> Unit
) {
    PremiumCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        containerColor = SurfaceWarm
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = Color.White)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Verified Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Government ID and mobile verified for security.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                )
            }
            IconButton(onClick = onOpenTrustDetails) {
                Icon(Icons.Filled.Verified, contentDescription = "Open verification details", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun PrivacySettingsCard(
    profile: ProfileData,
    onUpdatePhotoPrivacy: (String, String, Boolean) -> Unit
) {
    val showPhotoToEveryone = profile.photoPrivacy.equals("all", ignoreCase = true)
    var hideLastSeen by remember(profile.profileId, profile.hideLastSeen) { mutableStateOf(profile.hideLastSeen) }

    PremiumCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
                Text(
                    "Privacy Settings",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
                    color = Color(0xFF1E1B18),
                    fontWeight = FontWeight.ExtraBold
                )
            }
            PrivacySwitchRow(
                label = "Show photo to everyone",
                subtitle = "Turn this off to allow photo access only after member requests.",
                checked = showPhotoToEveryone,
                onCheckedChange = { enabled ->
                    onUpdatePhotoPrivacy(
                        if (enabled) "all" else "request_only",
                        profile.profileVisibility,
                        hideLastSeen
                    )
                }
            )
            PrivacySwitchRow(
                label = "Hide last seen",
                subtitle = "Use this when you want more privacy while browsing and reviewing matches.",
                checked = hideLastSeen,
                onCheckedChange = {
                    hideLastSeen = it
                    onUpdatePhotoPrivacy(profile.photoPrivacy, profile.profileVisibility, it)
                }
            )
        }
    }
}

@Composable
private fun PrivacySwitchRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = Color(0xFF1E1B18), fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PhotoAccessRequestRow(
    request: PhotoAccessRequestData,
    showActions: Boolean,
    onApprove: () -> Unit,
    onDecline: () -> Unit
) {
    Surface(shape = RoundedCornerShape(16.dp), color = if (showActions) SurfaceWarm else SurfaceSoft, border = BorderStroke(1.dp, Divider)) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                MemberPhoto(
                    photoUrl = request.primaryPhotoUrl,
                    contentDescription = request.fullName(),
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(14.dp)
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(request.fullName(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        listOf(request.occupation, request.workingCity).filter { it.isNotBlank() }.joinToString(" | ").ifBlank { "Requested your photo" },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        "Status: ${titleCase(request.status)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (request.status.equals("approved", ignoreCase = true)) Success else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (!request.message.isNullOrBlank()) {
                Text(request.message ?: "", style = MaterialTheme.typography.bodySmall, color = PrimaryDark)
            }
            if (showActions) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f)) {
                        Text("Decline")
                    }
                    Button(onClick = onApprove, modifier = Modifier.weight(1f)) {
                        Text("Allow photo")
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalPhotoTile(uri: Uri, onDelete: (Uri) -> Unit) {
    PremiumCard(
        modifier = Modifier.size(width = 160.dp, height = 292.dp),
        contentPadding = PaddingValues(0.dp),
        fillWidth = false
    ) {
        Column {
            AsyncImage(
                model = uri,
                contentDescription = "Selected profile photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("New photo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Visible here while upload completes.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                TextButton(onClick = { onDelete(uri) }, modifier = Modifier.fillMaxWidth().height(38.dp)) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun PhotoTile(
    photo: ProfilePhoto,
    onDelete: (String) -> Unit,
    onSetPrimary: (String) -> Unit
) {
    PremiumCard(
        modifier = Modifier.size(width = 160.dp, height = 292.dp),
        contentPadding = PaddingValues(0.dp),
        fillWidth = false
    ) {
        Column {
            Box {
                MemberPhoto(
                    photoUrl = photo.photoUrl,
                    contentDescription = if (photo.isPrimary) "Primary photo" else "Profile photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                )
                if (photo.isPrimary) {
                    SignalChip(
                        label = "Primary",
                        tone = ChipTone.Success,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (photo.isPrimary) "Primary photo" else "Gallery photo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (!photo.isPrimary) {
                    TextButton(onClick = { onSetPrimary(photo.photoId) }, modifier = Modifier.fillMaxWidth().height(34.dp)) {
                        Text("Make primary")
                    }
                }
                TextButton(onClick = { onDelete(photo.photoId) }, modifier = Modifier.fillMaxWidth().height(38.dp)) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun PartnerPreferencesCard(
    preferences: PartnerPreferencesData,
    onSave: (PartnerPreferencesData) -> Unit
) {
    var ageMin by remember(preferences.ageMin) { mutableIntStateOf(preferences.ageMin) }
    var ageMax by remember(preferences.ageMax) { mutableIntStateOf(preferences.ageMax) }
    var religion by remember(preferences.religion) { mutableStateOf(preferences.religion.orEmpty()) }
    var manglikPref by remember(preferences.manglikPref) { mutableStateOf(preferences.manglikPref) }
    var educationText by remember(preferences.educationLevels) { mutableStateOf(preferences.educationLevels.joinToString(", ")) }
    var occupationsText by remember(preferences.occupations) { mutableStateOf(preferences.occupations.joinToString(", ")) }
    var incomeMinText by remember(preferences.annualIncomeMin) { mutableStateOf(preferences.annualIncomeMin?.toString().orEmpty()) }
    var incomeMaxText by remember(preferences.annualIncomeMax) { mutableStateOf(preferences.annualIncomeMax?.toString().orEmpty()) }
    var heightMinText by remember(preferences.heightMinCm) { mutableStateOf(preferences.heightMinCm?.toString().orEmpty()) }
    var heightMaxText by remember(preferences.heightMaxCm) { mutableStateOf(preferences.heightMaxCm?.toString().orEmpty()) }
    var locationsText by remember(preferences.locations) { mutableStateOf(preferences.locations.joinToString(", ")) }
    var radiusText by remember(preferences.locationRadiusKm) { mutableStateOf(preferences.locationRadiusKm.toString()) }
    var dietText by remember(preferences.dietPrefs) { mutableStateOf(preferences.dietPrefs.joinToString(", ")) }
    var maritalText by remember(preferences.maritalStatuses) { mutableStateOf(preferences.maritalStatuses.joinToString(", ")) }
    var familyTypeText by remember(preferences.familyTypes) { mutableStateOf(preferences.familyTypes.joinToString(", ")) }
    var timeline by remember(preferences.timeline) { mutableStateOf(preferences.timeline.orEmpty()) }
    var dealBreakersText by remember(preferences.dealBreakers) { mutableStateOf(preferences.dealBreakers.joinToString(", ")) }
    var goodToHaveText by remember(preferences.goodToHave) { mutableStateOf(preferences.goodToHave.joinToString(", ")) }
    var relocationOpen by remember(preferences.relocationOpen) { mutableStateOf(preferences.relocationOpen) }
    var selectedTab by remember { mutableStateOf("Basics") }

    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = SurfaceWarm) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Partner preferences", "These inputs now power Smart Search, match ranking, and future AI recommendations")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(listOf("Basics", "Career", "Lifestyle", "Family", "Horoscope")) { tab ->
                    FilterChoiceChip(
                        label = tab,
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab }
                    )
                }
            }
            when (selectedTab) {
                "Basics" -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = ageMin.toString(),
                            onValueChange = { ageMin = it.toIntOrNull() ?: ageMin },
                            label = { Text("Age min") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ageMax.toString(),
                            onValueChange = { ageMax = it.toIntOrNull() ?: ageMax },
                            label = { Text("Age max") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = heightMinText,
                            onValueChange = { heightMinText = it.filter { char -> char.isDigit() }.take(3) },
                            label = { Text("Height min cm") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = heightMaxText,
                            onValueChange = { heightMaxText = it.filter { char -> char.isDigit() }.take(3) },
                            label = { Text("Height max cm") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = locationsText,
                        onValueChange = { locationsText = it },
                        label = { Text("Preferred cities/localities") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = radiusText,
                        onValueChange = { radiusText = it.filter { char -> char.isDigit() }.take(3) },
                        label = { Text("Location radius km") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text("SoulMatch ranks profiles inside these age, height, and location rules higher.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                "Career" -> {
                    OutlinedTextField(
                        value = educationText,
                        onValueChange = { educationText = it },
                        label = { Text("Education levels") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = occupationsText,
                        onValueChange = { occupationsText = it },
                        label = { Text("Occupations") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = incomeMinText,
                            onValueChange = { incomeMinText = it.filter { char -> char.isDigit() }.take(3) },
                            label = { Text("Income min LPA") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = incomeMaxText,
                            onValueChange = { incomeMaxText = it.filter { char -> char.isDigit() }.take(3) },
                            label = { Text("Income max LPA") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Text("Use comma separated values. Example: Graduate, Post Graduate or IT, Doctor.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                "Lifestyle" -> {
                    OutlinedTextField(
                        value = dietText,
                        onValueChange = { dietText = it },
                        label = { Text("Diet preferences") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = timeline,
                        onValueChange = { timeline = it.take(40) },
                        label = { Text("Marriage timeline") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilterChoiceChip("Relocation yes", relocationOpen == true, onClick = { relocationOpen = true })
                        FilterChoiceChip("Relocation no", relocationOpen == false, onClick = { relocationOpen = false })
                        FilterChoiceChip("Flexible", relocationOpen == null, onClick = { relocationOpen = null })
                    }
                    OutlinedTextField(
                        value = dealBreakersText,
                        onValueChange = { dealBreakersText = it },
                        label = { Text("Deal breakers") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = goodToHaveText,
                        onValueChange = { goodToHaveText = it },
                        label = { Text("Good to have") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                "Family" -> {
                    OutlinedTextField(
                        value = religion,
                        onValueChange = { religion = it },
                        label = { Text("Preferred religion") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maritalText,
                        onValueChange = { maritalText = it },
                        label = { Text("Marital statuses") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = familyTypeText,
                        onValueChange = { familyTypeText = it },
                        label = { Text("Family type") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text("Leave religion blank if your family is open to all communities.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                else -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("any", "yes", "no").forEach { option ->
                            FilterChoiceChip(
                                label = "Manglik ${titleCase(option)}",
                                selected = manglikPref.equals(option, ignoreCase = true),
                                onClick = { manglikPref = option }
                            )
                        }
                    }
                    Text("Use Any when horoscope is flexible for your family.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricPill("Age", "$ageMin-$ageMax", modifier = Modifier.weight(1f), background = SurfaceSoft)
                MetricPill("Cities", preferenceLabel(locationsText), modifier = Modifier.weight(1f), background = SurfaceSoft)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricPill("Education", preferenceLabel(educationText), modifier = Modifier.weight(1f), background = SurfaceSoft)
                MetricPill("Manglik", titleCase(manglikPref), modifier = Modifier.weight(1f), background = SurfaceSoft)
            }
            Button(
                onClick = {
                    onSave(
                        PartnerPreferencesData(
                            ageMin = ageMin.coerceAtLeast(18),
                            ageMax = ageMax.coerceAtLeast(ageMin),
                            religion = religion.ifBlank { null },
                            manglikPref = manglikPref,
                            educationLevels = csvToList(educationText),
                            occupations = csvToList(occupationsText),
                            annualIncomeMin = incomeMinText.toIntOrNull(),
                            annualIncomeMax = incomeMaxText.toIntOrNull(),
                            heightMinCm = heightMinText.toIntOrNull(),
                            heightMaxCm = heightMaxText.toIntOrNull(),
                            locations = csvToList(locationsText),
                            locationRadiusKm = radiusText.toIntOrNull()?.coerceIn(1, 500) ?: 50,
                            dietPrefs = csvToList(dietText),
                            maritalStatuses = csvToList(maritalText),
                            familyTypes = csvToList(familyTypeText),
                            relocationOpen = relocationOpen,
                            timeline = timeline.ifBlank { null },
                            dealBreakers = csvToList(dealBreakersText),
                            goodToHave = csvToList(goodToHaveText)
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save preferences")
            }
        }
    }
}

private fun csvToList(value: String): List<String> =
    value.split(",").map { it.trim() }.filter { it.isNotBlank() }.distinct()

private fun preferenceLabel(value: String): String {
    val items = csvToList(value)
    return when {
        items.isEmpty() -> "Any"
        items.size == 1 -> items.first()
        else -> "${items.first()} +${items.size - 1}"
    }
}

@Composable
private fun TrustMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = SurfaceWarm,
        border = BorderStroke(1.dp, Divider.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TrustFactorStatusRow(
    label: String,
    detail: String,
    status: String
) {
    val normalized = status.lowercase(Locale.getDefault())
    val statusLabel = when {
        normalized in listOf("positive", "verified", "complete", "passed", "approved") -> "Verified"
        normalized in listOf("pending", "review", "in_review") -> "Pending"
        normalized in listOf("negative", "missing", "failed", "rejected") -> "Needs update"
        else -> titleCase(status.ifBlank { "Pending" }.replace('_', ' '))
    }
    val color = when (statusLabel) {
        "Verified" -> Success
        "Needs update" -> Error
        else -> TextSecondary
    }
    PremiumCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = color.copy(alpha = 0.12f), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                if (detail.isNotBlank()) {
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            Text(statusLabel, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun PartnerPreferencesSummaryCard(
    preferences: PartnerPreferencesData,
    onEdit: () -> Unit
) {
    val education = preferences.educationLevels.joinToString(", ").ifBlank { "Any education" }
    val location = preferences.locations.joinToString(", ").ifBlank { "Any city" }
    val height = listOfNotNull(
        preferences.heightMinCm?.let { "${it}cm+" },
        preferences.heightMaxCm?.let { "up to ${it}cm" }
    ).joinToString(" ").ifBlank { "Open" }

    PremiumCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Partner Preferences",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                    color = Color(0xFF1E1B18),
                    fontWeight = FontWeight.ExtraBold
                )
                TextButton(onClick = onEdit) {
                    Text("Edit", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                }
            }
            ProfileInfoLine(Icons.Filled.Favorite, "Age", "${preferences.ageMin}-${preferences.ageMax}")
            ProfileInfoLine(Icons.Filled.School, "Education", education)
            ProfileInfoLine(Icons.Filled.Work, "Career", preferences.occupations.joinToString(", ").ifBlank { "Any profession" })
            ProfileInfoLine(Icons.Filled.Person, "Location", location)
            ProfileInfoLine(Icons.Filled.AutoAwesome, "Height", height)
            if (preferences.dealBreakers.isNotEmpty()) {
                ProfileInfoLine(Icons.Filled.Lock, "Deal breakers", preferences.dealBreakers.joinToString(", "))
            }
        }
    }
}

@Composable
private fun SoulMatchAssistProfileCard(
    assistStatus: AssistStatusData,
    onToggleAssist: (Boolean) -> Unit
) {
    val enabled = assistStatus.isOptedIn
    PremiumCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        containerColor = SurfaceWarm
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("SoulMatch Assistance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Turn this on to request guided SoulMatch assistance from the admin team.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggleAssist
            )
        }
    }
}

@Composable
private fun ProfileInfoLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF1E1B18),
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SoulMatchAssistSummaryCard(
    assistStatus: AssistStatusData,
    onOpenAssist: () -> Unit
) {
    val summary = when {
        assistStatus.advisor != null -> "Assigned to ${assistStatus.advisor.fullName} for guided matchmaking."
        assistStatus.isOptedIn && assistStatus.requestStatus == "waiting_assignment" -> "SoulMatch is finding the best-fit advisor for your area."
        assistStatus.isOptedIn -> "SoulMatch Assist is enabled for your profile."
        else -> "Opt in if you want family-aware advisor support, curated shortlists, and guided follow-up."
    }
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = SurfaceWarm) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("SoulMatch Assist", summary)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricPill(
                    "Mode",
                    titleCase((if (assistStatus.isOptedIn) assistStatus.supportLevel else "self_service").replace('_', ' ')),
                    modifier = Modifier.weight(1f),
                    background = SurfaceSoft
                )
                MetricPill(
                    "Status",
                    titleCase(assistStatus.requestStatus.replace('_', ' ')),
                    modifier = Modifier.weight(1f),
                    background = SurfaceSoft
                )
            }
            if (assistStatus.location.city.isNotBlank() || assistStatus.location.pincode.isNotBlank()) {
                SignalChips(
                    labels = listOfNotNull(
                        assistStatus.location.city.takeIf { it.isNotBlank() },
                        assistStatus.location.pincode.takeIf { it.isNotBlank() }
                    ),
                    tone = ChipTone.Info
                )
            }
            Button(onClick = onOpenAssist, modifier = Modifier.fillMaxWidth()) {
                Text("Open SoulMatch Assist")
            }
        }
    }
}

@Composable
private fun ViewerRow(viewer: ViewerData, onOpen: () -> Unit) {
    PremiumCard(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onOpen)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            MemberPhoto(
                photoUrl = viewer.primaryPhotoUrl,
                contentDescription = viewer.fullName(),
                modifier = Modifier.size(58.dp),
                shape = RoundedCornerShape(16.dp)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(viewer.fullName(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Viewed ${formatDate(viewer.viewedAt)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            OutlinedButton(onClick = onOpen) { Text("Open") }
        }
    }
}

@Composable
private fun RecentViewersToggleCard(
    count: Int,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    PremiumCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Recent viewers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (enabled) "$count profile(s) can be reviewed below" else "Turn on to see who viewed your profile",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun ChecklistRow(item: ProfileChecklistItem, onEdit: () -> Unit) {
    PremiumCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (item.isComplete) SuccessSoft else SurfaceSoft,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(profileChecklistIcon(item.title), contentDescription = null, tint = if (item.isComplete) Success else TextSecondary)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text(item.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(
                    item.statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        item.isComplete -> Success
                        item.statusLabel.equals("Optional", ignoreCase = true) -> TextSecondary
                        else -> Error
                    },
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedButton(onClick = onEdit) {
                Text(if (item.isComplete) "Edit" else "Add")
            }
        }
    }
}

private fun profileChecklistIcon(title: String): androidx.compose.ui.graphics.vector.ImageVector {
    val normalized = title.lowercase(Locale.getDefault())
    return when {
        "work" in normalized || "education" in normalized -> Icons.Filled.Work
        "family" in normalized -> Icons.Filled.FamilyRestroom
        "lifestyle" in normalized -> Icons.Filled.Favorite
        "horoscope" in normalized || "astro" in normalized -> Icons.Filled.AutoAwesome
        "physical" in normalized -> Icons.Filled.Person
        else -> Icons.Filled.Person
    }
}

private fun Context.toPhotoPart(uri: Uri, index: Int): MultipartBody.Part? {
    val contentType = contentResolver.getType(uri) ?: "image/jpeg"
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType)?.lowercase(Locale.US) ?: "jpg"
    val fileName = "profile-photo-${System.currentTimeMillis()}-$index.$extension"
    val body = bytes.toRequestBody(contentType.toMediaTypeOrNull())
    return MultipartBody.Part.createFormData("photos", fileName, body)
}

private fun photoPrivacyLabel(value: String): String {
    return when (value.lowercase(Locale.US)) {
        "all" -> "Public"
        "matches_only" -> "Accepted matches only"
        "request_only" -> "Approval required"
        "private" -> "Private"
        else -> titleCase(value.replace('_', ' '))
    }
}

private fun formatProfileId(profileId: String): String {
    val clean = profileId.trim()
    if (clean.isBlank()) return "SM-0000"
    if (clean.startsWith("SM-", ignoreCase = true)) return clean.uppercase(Locale.getDefault())
    val digits = clean.filter { it.isDigit() }
    if (digits.isNotBlank()) return "SM-${digits.takeLast(4).padStart(4, '0')}"
    val hash = kotlin.math.abs(clean.hashCode()).toString().takeLast(4).padStart(4, '0')
    return "SM-$hash"
}

private fun TrustFactorData.isFirebaseTrustFactor(): Boolean {
    val text = listOf(key, label, detail).joinToString(" ").lowercase(Locale.getDefault())
    return "firebase" in text || "fcm" in text
}

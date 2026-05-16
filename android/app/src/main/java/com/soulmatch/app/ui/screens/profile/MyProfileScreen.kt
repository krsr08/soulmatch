package com.soulmatch.app.ui.screens.profile

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import com.soulmatch.app.ui.components.premium.AvatarInitial
import com.soulmatch.app.ui.components.premium.ChipTone
import com.soulmatch.app.ui.components.premium.FilterChoiceChip
import com.soulmatch.app.ui.components.premium.LabeledProgress
import com.soulmatch.app.ui.components.media.MemberPhoto
import com.soulmatch.app.ui.components.premium.MetricPill
import com.soulmatch.app.ui.components.premium.PremiumCard
import com.soulmatch.app.ui.components.premium.PremiumHeader
import com.soulmatch.app.ui.components.premium.PremiumScreen
import com.soulmatch.app.ui.components.status.ProfileStrengthAdvisor
import com.soulmatch.app.ui.components.premium.SectionTitle
import com.soulmatch.app.ui.components.premium.SignalChip
import com.soulmatch.app.ui.components.premium.SignalChips
import com.soulmatch.app.ui.components.premium.UpgradePlanGate
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
    val savingAssist by vm.isSavingAssist.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val loadMessage by vm.loadMessage.collectAsStateWithLifecycle()
    val editSection: (Int) -> Unit = onEditSection ?: { _ -> }
    val openSettings: () -> Unit = onOpenSettings ?: {}
    val openPartnerPreferences: () -> Unit = onOpenPartnerPreferences ?: {}
    val openTrustDetails: () -> Unit = onOpenTrustDetails ?: {}
    val openFamilyBoard: () -> Unit = onOpenFamilyBoard ?: {}
    val openAssist: () -> Unit = onOpenAssist ?: {}
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
    val lifecycleOwner = LocalLifecycleOwner.current
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

    DisposableEffect(lifecycleOwner, vm) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.load()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
                                isSaving = savingAssist,
                                onToggleAssist = toggleAssist,
                                onOpenAssist = openAssist
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
    onEditProfileStep: (Int) -> Unit = {},
    vm: MyProfileViewModel = hiltViewModel()
) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val photos by vm.photos.collectAsStateWithLifecycle()
    val verifications by vm.verifications.collectAsStateWithLifecycle()
    val isSubmitting by vm.isSubmittingVerification.collectAsStateWithLifecycle()
    val uploadingPhotos by vm.isUploadingPhotos.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedEntry by remember { mutableStateOf<TrustActionType?>(null) }
    var pendingVerificationType by remember { mutableStateOf<String?>(null) }
    var contactDraft by remember(profile?.profileId, profile?.phone, profile?.email) {
        mutableStateOf(TrustContactDraft(phone = profile?.phone.orEmpty(), email = profile?.email.orEmpty()))
    }
    var familyDraft by remember(profile?.profileId) { mutableStateOf(TrustFamilyDraft()) }
    var isEducated by remember(profile?.profileId, profile?.educationLevel) {
        mutableStateOf(profile?.educationLevel.orEmpty().isNotBlank())
    }
    var incomeType by remember(profile?.profileId, profile?.isEmployed) {
        mutableStateOf(if (profile?.isEmployed == true) "Private" else "Not employed")
    }
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val type = pendingVerificationType
        pendingVerificationType = null
        if (uri != null && type != null) {
            context.toVerificationDocumentPart(uri)?.let { part ->
                vm.submitTrustVerification(type, part)
            }
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            context.toPhotoPart(uri, 0)?.let { part -> vm.uploadPhotos(listOf(part)) }
        }
    }
    val trustEntries = remember(profile, photos, verifications, contactDraft, familyDraft, isEducated, incomeType) {
        buildTrustChecklist(
            profile = profile,
            photos = photos,
            verifications = verifications,
            contactDraft = contactDraft,
            familyDraft = familyDraft,
            isEducated = isEducated,
            incomeType = incomeType
        )
    }
    val calculatedScore = remember(trustEntries) { calculateTrustScore(trustEntries) }

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
                if (!status.isNullOrBlank()) {
                    item {
                        StatusCard(status = status.orEmpty())
                    }
                }
                item {
                    PremiumCard(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            TrustMetricTile(
                                label = "Trust Score",
                                value = "$calculatedScore%",
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "Tap any trust item to add details, upload proof, or review the current verification status. The score updates as items are completed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            if ((profile?.trustScore ?: 0) > 0) {
                                Text(
                                    "Backend trust engine: ${profile?.trustScore?.coerceIn(0, 100)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                items(trustEntries, key = { it.type.name }) { entry ->
                    TrustChecklistStatusRow(
                        entry = entry,
                        onClick = { selectedEntry = entry.type }
                    )
                }
            }
        }
    }
    selectedEntry?.let { type ->
        val entry = trustEntries.firstOrNull { it.type == type }
        if (entry != null) {
            TrustActionDialog(
                entry = entry,
                profile = profile,
                contactDraft = contactDraft,
                onContactDraftChange = { contactDraft = it },
                familyDraft = familyDraft,
                onFamilyDraftChange = { familyDraft = it },
                isEducated = isEducated,
                onEducatedChange = { isEducated = it },
                incomeType = incomeType,
                onIncomeTypeChange = { incomeType = it },
                isSubmitting = isSubmitting,
                uploadingPhotos = uploadingPhotos,
                onDismiss = { selectedEntry = null },
                onEditStep = onEditProfileStep,
                onUploadDocument = { verificationType ->
                    pendingVerificationType = verificationType
                    documentPicker.launch("*/*")
                },
                onUploadPhoto = { photoPicker.launch("image/*") },
                onSubmitVerification = { verificationType -> vm.submitTrustVerification(verificationType) }
            )
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
    val score = ProfileStrengthAdvisor.score(profile)
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

private enum class TrustActionType {
    PhoneVerification,
    EmailVerification,
    ProfileCompletion,
    AdminVerification,
    DocumentVerification,
    EducationVerification,
    IncomeVerification,
    FamilyVerification,
    PhotoUploaded,
    ProfileActive,
    SafetyReports
}

private enum class TrustEntryState {
    Verified,
    Pending,
    Missing,
    Warning
}

private data class TrustContactDraft(
    val phone: String = "",
    val email: String = ""
)

private data class TrustFamilyDraft(
    val fatherName: String = "",
    val motherName: String = "",
    val hasSiblings: Boolean = false,
    val siblingCount: String = "",
    val alternatePhone: String = ""
)

private data class TrustChecklistEntry(
    val type: TrustActionType,
    val title: String,
    val detail: String,
    val statusLabel: String,
    val state: TrustEntryState,
    val icon: ImageVector
)

private fun buildTrustChecklist(
    profile: ProfileData?,
    photos: List<ProfilePhoto>,
    verifications: List<VerificationRequestData>,
    contactDraft: TrustContactDraft,
    familyDraft: TrustFamilyDraft,
    isEducated: Boolean,
    incomeType: String
): List<TrustChecklistEntry> {
    val completionScore = profile?.completionScore?.coerceIn(0, 100) ?: 0
    val hasPhoto = photos.isNotEmpty() || !profile?.primaryPhotoUrl.isNullOrBlank()
    val hasPhone = contactDraft.phone.isNotBlank() || profile?.phone.orEmpty().isNotBlank()
    val hasEmail = contactDraft.email.isNotBlank() || profile?.email.orEmpty().isNotBlank()
    val identity = latestVerification(verifications, "identity", "document", "aadhaar", "pan")
    val education = latestVerification(verifications, "education")
    val income = latestVerification(verifications, "income")
    val family = latestVerification(verifications, "family")
    val profileVerification = latestVerification(verifications, "profile", "identity")
    val adminStatus = profileVerification?.status ?: profile?.verificationStatus.orEmpty()
    val isEmployed = incomeType != "Not employed" || profile?.isEmployed == true
    val hasIncomeDetail = !profile?.occupation.orEmpty().isBlank() || !profile?.annualIncome.orEmpty().isBlank() || incomeType != "Not employed"
    val hasFamilyDetail = familyDraft.fatherName.isNotBlank() ||
        familyDraft.motherName.isNotBlank() ||
        familyDraft.alternatePhone.isNotBlank() ||
        profile?.fatherOccupation.orEmpty().isNotBlank() ||
        profile?.motherOccupation.orEmpty().isNotBlank() ||
        profile?.familyCity.orEmpty().isNotBlank() ||
        profile?.numBrothers != null ||
        profile?.numSisters != null

    return listOf(
        TrustChecklistEntry(
            type = TrustActionType.PhoneVerification,
            title = "Phone Verification",
            detail = when {
                profile?.isPhoneVerified == true -> "Mobile number is OTP verified."
                hasPhone -> "Phone added. OTP verification is pending."
                else -> "Add a mobile number for profile trust."
            },
            statusLabel = when {
                profile?.isPhoneVerified == true -> "Verified"
                hasPhone -> "Added"
                else -> "Missing"
            },
            state = when {
                profile?.isPhoneVerified == true -> TrustEntryState.Verified
                hasPhone -> TrustEntryState.Pending
                else -> TrustEntryState.Missing
            },
            icon = Icons.Filled.Call
        ),
        TrustChecklistEntry(
            type = TrustActionType.EmailVerification,
            title = "Email Verification",
            detail = if (hasEmail) "Email is linked to this profile." else "Add an email address for notifications and recovery.",
            statusLabel = if (hasEmail) "Linked" else "Missing",
            state = if (hasEmail) TrustEntryState.Verified else TrustEntryState.Missing,
            icon = Icons.Filled.Email
        ),
        TrustChecklistEntry(
            type = TrustActionType.ProfileCompletion,
            title = "Profile Completion",
            detail = "Current profile strength is $completionScore%.",
            statusLabel = if (completionScore >= 100) "Complete" else "$completionScore%",
            state = if (completionScore >= 100) TrustEntryState.Verified else TrustEntryState.Pending,
            icon = Icons.Filled.Badge
        ),
        TrustChecklistEntry(
            type = TrustActionType.AdminVerification,
            title = "Admin Verification",
            detail = adminVerificationDetail(adminStatus),
            statusLabel = statusLabelForVerification(adminStatus),
            state = stateForVerification(adminStatus, hasFallback = false),
            icon = Icons.Filled.Verified
        ),
        TrustChecklistEntry(
            type = TrustActionType.DocumentVerification,
            title = "Document Verification",
            detail = verificationDetail(identity, "Upload Aadhaar or PAN for document review."),
            statusLabel = statusLabelForVerification(identity?.status.orEmpty()),
            state = stateForVerification(identity?.status.orEmpty(), hasFallback = false),
            icon = Icons.Filled.Description
        ),
        TrustChecklistEntry(
            type = TrustActionType.EducationVerification,
            title = "Education Verification",
            detail = when {
                !isEducated -> "Marked as not applicable."
                education != null -> verificationDetail(education, "Education proof submitted.")
                profile?.educationLevel.orEmpty().isNotBlank() -> "Education details added. Upload proof for stronger trust."
                else -> "Add education details or mark not applicable."
            },
            statusLabel = when {
                !isEducated -> "Not applicable"
                education != null -> statusLabelForVerification(education.status)
                profile?.educationLevel.orEmpty().isNotBlank() -> "Details added"
                else -> "Missing"
            },
            state = when {
                !isEducated -> TrustEntryState.Verified
                education != null -> stateForVerification(education.status, hasFallback = false)
                profile?.educationLevel.orEmpty().isNotBlank() -> TrustEntryState.Pending
                else -> TrustEntryState.Missing
            },
            icon = Icons.Filled.School
        ),
        TrustChecklistEntry(
            type = TrustActionType.IncomeVerification,
            title = "Income Verification",
            detail = when {
                !isEmployed -> "Marked as not employed."
                income != null -> verificationDetail(income, "Income proof submitted.")
                hasIncomeDetail -> "Employment details added. Upload proof if you want income verification."
                else -> "Add employment type and proof when applicable."
            },
            statusLabel = when {
                !isEmployed -> "Not applicable"
                income != null -> statusLabelForVerification(income.status)
                hasIncomeDetail -> "Details added"
                else -> "Missing"
            },
            state = when {
                !isEmployed -> TrustEntryState.Verified
                income != null -> stateForVerification(income.status, hasFallback = false)
                hasIncomeDetail -> TrustEntryState.Pending
                else -> TrustEntryState.Missing
            },
            icon = Icons.Filled.Work
        ),
        TrustChecklistEntry(
            type = TrustActionType.FamilyVerification,
            title = "Family Verification",
            detail = when {
                family != null -> verificationDetail(family, "Family details submitted.")
                hasFamilyDetail -> "Family details are available for trust review."
                else -> "Add parent, sibling, and alternate family contact details."
            },
            statusLabel = when {
                family != null -> statusLabelForVerification(family.status)
                hasFamilyDetail -> "Details added"
                else -> "Missing"
            },
            state = when {
                family != null -> stateForVerification(family.status, hasFallback = false)
                hasFamilyDetail -> TrustEntryState.Pending
                else -> TrustEntryState.Missing
            },
            icon = Icons.Filled.FamilyRestroom
        ),
        TrustChecklistEntry(
            type = TrustActionType.PhotoUploaded,
            title = "Photo Uploaded",
            detail = if (hasPhoto) "At least one profile photo is available." else "Upload a clear profile photo.",
            statusLabel = if (hasPhoto) "Complete" else "Missing",
            state = if (hasPhoto) TrustEntryState.Verified else TrustEntryState.Missing,
            icon = Icons.Filled.PhotoLibrary
        ),
        TrustChecklistEntry(
            type = TrustActionType.ProfileActive,
            title = "Profile Active",
            detail = if (profile?.profileStatus.equals("active", ignoreCase = true)) {
                "Your profile is active and eligible for discovery."
            } else {
                "Profile is not active right now."
            },
            statusLabel = titleCase(profile?.profileStatus?.replace('_', ' ').orEmpty().ifBlank { "Pending" }),
            state = if (profile?.profileStatus.equals("active", ignoreCase = true)) TrustEntryState.Verified else TrustEntryState.Warning,
            icon = Icons.Filled.Shield
        ),
        TrustChecklistEntry(
            type = TrustActionType.SafetyReports,
            title = "Safety Reports",
            detail = if (profile?.trustWarnings.orEmpty().isEmpty()) {
                "No active safety reports are linked to this profile."
            } else {
                profile?.trustWarnings.orEmpty().joinToString(", ")
            },
            statusLabel = if (profile?.trustWarnings.orEmpty().isEmpty()) "Clear" else "Review",
            state = if (profile?.trustWarnings.orEmpty().isEmpty()) TrustEntryState.Verified else TrustEntryState.Warning,
            icon = Icons.Filled.ReportProblem
        )
    )
}

private fun latestVerification(verifications: List<VerificationRequestData>, vararg types: String): VerificationRequestData? {
    val normalized = types.map { it.lowercase(Locale.getDefault()) }.toSet()
    return verifications
        .filter { it.type.lowercase(Locale.getDefault()) in normalized }
        .maxByOrNull { it.createdAt }
}

private fun calculateTrustScore(entries: List<TrustChecklistEntry>): Int {
    if (entries.isEmpty()) return 0
    val score = entries.map { entry ->
        when (entry.state) {
            TrustEntryState.Verified -> 100
            TrustEntryState.Pending -> 70
            TrustEntryState.Warning -> 35
            TrustEntryState.Missing -> 0
        }
    }.sum() / entries.size
    return score.coerceIn(0, 100)
}

private fun stateForVerification(status: String, hasFallback: Boolean): TrustEntryState {
    val normalized = status.lowercase(Locale.getDefault())
    return when {
        normalized in listOf("approved", "verified", "complete", "passed") -> TrustEntryState.Verified
        normalized in listOf("pending", "review", "in_review", "under_review") -> TrustEntryState.Pending
        normalized in listOf("rejected", "declined", "failed") -> TrustEntryState.Warning
        hasFallback -> TrustEntryState.Pending
        else -> TrustEntryState.Missing
    }
}

private fun statusLabelForVerification(status: String): String {
    val normalized = status.lowercase(Locale.getDefault())
    return when {
        normalized in listOf("approved", "verified", "complete", "passed") -> "Approved"
        normalized in listOf("pending", "review", "in_review", "under_review") -> "In progress"
        normalized in listOf("rejected", "declined", "failed") -> "Rejected"
        else -> "Not submitted"
    }
}

private fun verificationDetail(request: VerificationRequestData?, fallback: String): String {
    if (request == null) return fallback
    val reviewed = request.reviewedAt?.takeIf { it.isNotBlank() }?.let { "Reviewed ${formatDate(it)}" }
    val created = request.createdAt.takeIf { it.isNotBlank() }?.let { "Submitted ${formatDate(it)}" }
    val note = request.reviewNote?.takeIf { it.isNotBlank() }?.let { "Admin note: $it" }
    return listOfNotNull(statusLabelForVerification(request.status), reviewed ?: created, note)
        .joinToString(" · ")
}

private fun adminVerificationDetail(status: String): String {
    return when (stateForVerification(status, hasFallback = false)) {
        TrustEntryState.Verified -> "Admin verification is approved."
        TrustEntryState.Pending -> "Admin review is in progress."
        TrustEntryState.Warning -> "Admin rejected the verification. Review comments and resubmit."
        TrustEntryState.Missing -> "Request admin review once profile details and photos are ready."
    }
}

@Composable
private fun TrustChecklistStatusRow(
    entry: TrustChecklistEntry,
    onClick: () -> Unit
) {
    val color = when (entry.state) {
        TrustEntryState.Verified -> Success
        TrustEntryState.Pending -> MaterialTheme.colorScheme.primary
        TrustEntryState.Warning -> Error
        TrustEntryState.Missing -> TextSecondary
    }
    val background = when (entry.state) {
        TrustEntryState.Verified -> SuccessSoft
        TrustEntryState.Pending -> SurfaceWarm
        TrustEntryState.Warning -> ErrorSoft
        TrustEntryState.Missing -> SurfaceSoft
    }
    PremiumCard(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(16.dp), color = background, modifier = Modifier.size(46.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(entry.icon, contentDescription = null, tint = color, modifier = Modifier.size(21.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                Text(entry.detail, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.statusLabel, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.ExtraBold)
                Icon(Icons.Filled.ChevronRight, contentDescription = "Open ${entry.title}", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun TrustActionDialog(
    entry: TrustChecklistEntry,
    profile: ProfileData?,
    contactDraft: TrustContactDraft,
    onContactDraftChange: (TrustContactDraft) -> Unit,
    familyDraft: TrustFamilyDraft,
    onFamilyDraftChange: (TrustFamilyDraft) -> Unit,
    isEducated: Boolean,
    onEducatedChange: (Boolean) -> Unit,
    incomeType: String,
    onIncomeTypeChange: (String) -> Unit,
    isSubmitting: Boolean,
    uploadingPhotos: Boolean,
    onDismiss: () -> Unit,
    onEditStep: (Int) -> Unit,
    onUploadDocument: (String) -> Unit,
    onUploadPhoto: () -> Unit,
    onSubmitVerification: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(entry.statusLabel, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(entry.detail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                when (entry.type) {
                    TrustActionType.PhoneVerification -> {
                        OutlinedTextField(
                            value = contactDraft.phone.ifBlank { profile?.phone.orEmpty() },
                            onValueChange = { value ->
                                onContactDraftChange(contactDraft.copy(phone = value.filter { it.isDigit() }.take(10)))
                            },
                            label = { Text("Phone number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Text(
                            "OTP login marks the phone as verified. New phone numbers stay added until OTP verification is completed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    TrustActionType.EmailVerification -> {
                        OutlinedTextField(
                            value = contactDraft.email.ifBlank { profile?.email.orEmpty() },
                            onValueChange = { value -> onContactDraftChange(contactDraft.copy(email = value.take(80))) },
                            label = { Text("Email address") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    TrustActionType.ProfileCompletion -> {
                        LabeledProgress(
                            label = "Profile completion",
                            value = (profile?.completionScore ?: 0).coerceIn(0, 100)
                        )
                        Button(
                            onClick = {
                                onDismiss()
                                onEditStep(1)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open profile steps")
                        }
                    }
                    TrustActionType.AdminVerification -> {
                        Button(
                            onClick = { onSubmitVerification("profile") },
                            enabled = !isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Text(if (isSubmitting) "Submitting" else "Request admin verification")
                        }
                    }
                    TrustActionType.DocumentVerification -> {
                        Text("Upload Aadhaar or PAN. The document is sent to admin review and is not shown publicly.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { onUploadDocument("identity") }, modifier = Modifier.weight(1f)) {
                                Text("Aadhaar")
                            }
                            Button(onClick = { onUploadDocument("identity") }, modifier = Modifier.weight(1f)) {
                                Text("PAN")
                            }
                        }
                    }
                    TrustActionType.EducationVerification -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            FilterChoiceChip("Educated", isEducated, onClick = { onEducatedChange(true) })
                            FilterChoiceChip("Not educated", !isEducated, onClick = { onEducatedChange(false) })
                        }
                        if (isEducated) {
                            Button(onClick = { onUploadDocument("education") }, modifier = Modifier.fillMaxWidth()) {
                                Text("Upload education proof")
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onEditStep(3)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Update education details")
                        }
                    }
                    TrustActionType.IncomeVerification -> {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("Not employed", "Self", "Gov", "Private")) { option ->
                                FilterChoiceChip(option, incomeType == option, onClick = { onIncomeTypeChange(option) })
                            }
                        }
                        if (incomeType != "Not employed") {
                            Button(onClick = { onUploadDocument("income") }, modifier = Modifier.fillMaxWidth()) {
                                Text("Upload income proof")
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onEditStep(3)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Update career details")
                        }
                    }
                    TrustActionType.FamilyVerification -> {
                        OutlinedTextField(
                            value = familyDraft.fatherName,
                            onValueChange = { onFamilyDraftChange(familyDraft.copy(fatherName = it.take(80))) },
                            label = { Text("Father name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = familyDraft.motherName,
                            onValueChange = { onFamilyDraftChange(familyDraft.copy(motherName = it.take(80))) },
                            label = { Text("Mother name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            FilterChoiceChip("Siblings: Yes", familyDraft.hasSiblings, onClick = { onFamilyDraftChange(familyDraft.copy(hasSiblings = true)) })
                            FilterChoiceChip("No", !familyDraft.hasSiblings, onClick = { onFamilyDraftChange(familyDraft.copy(hasSiblings = false, siblingCount = "")) })
                        }
                        if (familyDraft.hasSiblings) {
                            OutlinedTextField(
                                value = familyDraft.siblingCount,
                                onValueChange = { onFamilyDraftChange(familyDraft.copy(siblingCount = it.filter { char -> char.isDigit() }.take(2))) },
                                label = { Text("Sibling count") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        OutlinedTextField(
                            value = familyDraft.alternatePhone,
                            onValueChange = { onFamilyDraftChange(familyDraft.copy(alternatePhone = it.filter { char -> char.isDigit() }.take(10))) },
                            label = { Text("Alternate family contact") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onEditStep(4)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Edit family")
                            }
                            Button(onClick = { onSubmitVerification("family") }, enabled = !isSubmitting, modifier = Modifier.weight(1f)) {
                                Text("Submit")
                            }
                        }
                    }
                    TrustActionType.PhotoUploaded -> {
                        Button(onClick = onUploadPhoto, enabled = !uploadingPhotos, modifier = Modifier.fillMaxWidth()) {
                            if (uploadingPhotos) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Text(if (uploadingPhotos) "Uploading" else "Upload photo")
                        }
                    }
                    TrustActionType.ProfileActive -> {
                        Text(
                            "Active profiles are eligible for Best Matches and profile views. Use profile settings if you want to pause visibility.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    TrustActionType.SafetyReports -> {
                        val warnings = profile?.trustWarnings.orEmpty()
                        if (warnings.isEmpty()) {
                            SignalChips(labels = listOf("No active safety reports"), tone = ChipTone.Success)
                        } else {
                            warnings.forEach { warning ->
                                Text("• $warning", style = MaterialTheme.typography.bodySmall, color = Error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
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
    var previewPhoto by remember { mutableStateOf<Any?>(null) }
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
            IconButton(onClick = onUpload, enabled = !uploadingPhotos) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = "Add photos", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ProfilePhotoSquare(
                localUri = primaryLocal,
                photoUrl = primaryPhoto?.photoUrl,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
                onClick = {
                    val preview = primaryLocal ?: primaryPhoto?.photoUrl
                    if (preview?.toString().isNullOrBlank()) onUpload() else previewPhoto = preview
                }
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
                    LocalPhotoTile(uri = uri, onDelete = onDeleteLocal, onPreview = { previewPhoto = uri })
                }
                items(photos.filter { it.photoId != primaryPhoto?.photoId }, key = { it.photoId }) { photo ->
                    PhotoTile(photo = photo, onDelete = onDelete, onSetPrimary = onSetPrimary, onPreview = { previewPhoto = photo.photoUrl })
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SurfaceWarm,
            border = BorderStroke(1.dp, Divider.copy(alpha = 0.7f))
        ) {
            Text(
                "Tap a photo to view it. Use the badge icon to make a photo primary, or X to delete.",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
    previewPhoto?.let { model ->
        PhotoPreviewDialog(model = model, onDismiss = { previewPhoto = null })
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
    onUpdatePhotoPrivacy: (String, String, Boolean, String?) -> Unit
) {
    val showPhotoToEveryone = profile.photoPrivacy.equals("all", ignoreCase = true)
    val contactMasked = profile.contactPrivacy.equals("masked", ignoreCase = true)
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
                        hideLastSeen,
                        null
                    )
                }
            )
            PrivacySwitchRow(
                label = "Keep contact details private",
                subtitle = "When enabled, members see a message to connect through chat instead of unmasking your phone or email.",
                checked = contactMasked,
                onCheckedChange = { masked ->
                    onUpdatePhotoPrivacy(
                        profile.photoPrivacy,
                        profile.profileVisibility,
                        hideLastSeen,
                        if (masked) "masked" else "visible"
                    )
                }
            )
            PrivacySwitchRow(
                label = "Hide last seen",
                subtitle = "Use this when you want more privacy while browsing and reviewing matches.",
                checked = hideLastSeen,
                onCheckedChange = {
                    hideLastSeen = it
                    onUpdatePhotoPrivacy(profile.photoPrivacy, profile.profileVisibility, it, null)
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
private fun LocalPhotoTile(
    uri: Uri,
    onDelete: (Uri) -> Unit,
    onPreview: () -> Unit
) {
    PremiumCard(
        modifier = Modifier.size(width = 132.dp, height = 168.dp),
        contentPadding = PaddingValues(0.dp),
        fillWidth = false
    ) {
        Box {
            AsyncImage(
                model = uri,
                contentDescription = "Selected profile photo",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onPreview),
                contentScale = ContentScale.Crop
            )
            PhotoActionIcon(
                icon = Icons.Filled.Close,
                contentDescription = "Remove selected photo",
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                onClick = { onDelete(uri) }
            )
            SignalChip(
                label = "Uploading",
                tone = ChipTone.Neutral,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
            )
        }
    }
}

@Composable
private fun PhotoActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .size(34.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, Divider.copy(alpha = 0.7f)),
        shadowElevation = 2.dp
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PhotoTile(
    photo: ProfilePhoto,
    onDelete: (String) -> Unit,
    onSetPrimary: (String) -> Unit,
    onPreview: () -> Unit
) {
    PremiumCard(
        modifier = Modifier.size(width = 132.dp, height = 168.dp),
        contentPadding = PaddingValues(0.dp),
        fillWidth = false
    ) {
        Box {
            MemberPhoto(
                photoUrl = photo.photoUrl,
                contentDescription = if (photo.isPrimary) "Primary photo" else "Profile photo",
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onPreview),
                shape = RoundedCornerShape(8.dp)
            )
            if (photo.isPrimary) {
                SignalChip(
                    label = "Primary",
                    tone = ChipTone.Success,
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                )
            } else {
                PhotoActionIcon(
                    icon = Icons.Filled.WorkspacePremium,
                    contentDescription = "Make primary photo",
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                    onClick = { onSetPrimary(photo.photoId) }
                )
            }
            PhotoActionIcon(
                icon = Icons.Filled.Close,
                contentDescription = "Delete photo",
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                onClick = { onDelete(photo.photoId) }
            )
        }
    }
}

@Composable
private fun PhotoPreviewDialog(
    model: Any,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = {
            Text("Profile photo", fontWeight = FontWeight.ExtraBold)
        },
        text = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f),
                shape = RoundedCornerShape(18.dp),
                color = SurfaceSoft,
                border = BorderStroke(1.dp, Divider)
            ) {
                AsyncImage(
                    model = model,
                    contentDescription = "Profile photo preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
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
    var educationText by remember(preferences.educationLevels) { mutableStateOf(preferenceText(preferences.educationLevels)) }
    var occupationsText by remember(preferences.occupations) { mutableStateOf(preferenceText(preferences.occupations)) }
    var incomeMinText by remember(preferences.annualIncomeMin) { mutableStateOf(preferences.annualIncomeMin?.toString().orEmpty()) }
    var incomeMaxText by remember(preferences.annualIncomeMax) { mutableStateOf(preferences.annualIncomeMax?.toString().orEmpty()) }
    var heightMinText by remember(preferences.heightMinCm) { mutableStateOf(preferences.heightMinCm?.toString().orEmpty()) }
    var heightMaxText by remember(preferences.heightMaxCm) { mutableStateOf(preferences.heightMaxCm?.toString().orEmpty()) }
    var locationsText by remember(preferences.locations) { mutableStateOf(preferenceText(preferences.locations)) }
    var radiusText by remember(preferences.locationRadiusKm) { mutableStateOf(preferences.locationRadiusKm.toString()) }
    var dietText by remember(preferences.dietPrefs) { mutableStateOf(preferenceText(preferences.dietPrefs)) }
    var maritalText by remember(preferences.maritalStatuses) { mutableStateOf(preferenceText(preferences.maritalStatuses)) }
    var familyTypeText by remember(preferences.familyTypes) { mutableStateOf(preferenceText(preferences.familyTypes)) }
    var timeline by remember(preferences.timeline) { mutableStateOf(preferences.timeline.orEmpty()) }
    var dealBreakersText by remember(preferences.dealBreakers) { mutableStateOf(preferenceText(preferences.dealBreakers)) }
    var goodToHaveText by remember(preferences.goodToHave) { mutableStateOf(preferenceText(preferences.goodToHave)) }
    var relocationOpen by remember(preferences.relocationOpen) { mutableStateOf(preferences.relocationOpen) }
    var selectedTab by remember { mutableStateOf("Basics") }

    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = SurfaceWarm) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Partner preferences", "These inputs now power Discover, match ranking, and future AI recommendations")
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

private fun preferenceItems(values: List<String>?): List<String> =
    values.orEmpty()
        .mapNotNull { value -> (value as? String)?.trim()?.takeIf { it.isNotBlank() } }
        .distinct()

private fun preferenceText(values: List<String>?): String =
    preferenceItems(values).joinToString(", ")

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
    val education = preferenceText(preferences.educationLevels).ifBlank { "Any education" }
    val location = preferenceText(preferences.locations).ifBlank { "Any city" }
    val career = preferenceText(preferences.occupations).ifBlank {
        when {
            preferences.annualIncomeMin != null || preferences.annualIncomeMax != null -> "Income-led matching"
            else -> "Any profession"
        }
    }
    val height = listOfNotNull(
        preferences.heightMinCm?.let { "${it}cm+" },
        preferences.heightMaxCm?.let { "up to ${it}cm" }
    ).joinToString(" ").ifBlank { "Open" }
    val religion = preferences.religion?.takeIf { it.isNotBlank() } ?: "Any religion"
    val dealBreakers = preferenceItems(preferences.dealBreakers)

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
            ProfileInfoLine(Icons.Filled.Verified, "Religion", religion)
            ProfileInfoLine(Icons.Filled.School, "Education", education)
            ProfileInfoLine(Icons.Filled.Work, "Career", career)
            ProfileInfoLine(Icons.Filled.Person, "Location", location)
            ProfileInfoLine(Icons.Filled.AutoAwesome, "Height", height)
            if (dealBreakers.isNotEmpty()) {
                ProfileInfoLine(Icons.Filled.Lock, "Deal breakers", dealBreakers.joinToString(", "))
            }
        }
    }
}

@Composable
private fun SoulMatchAssistProfileCard(
    assistStatus: AssistStatusData,
    isSaving: Boolean,
    onToggleAssist: (Boolean) -> Unit,
    onOpenAssist: () -> Unit
) {
    val enabled = assistStatus.isOptedIn
    PremiumCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        containerColor = SurfaceWarm
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "SoulMatch Assistance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        if (enabled) {
                            "Enabled. Browse registered agents and connect with them directly offline when you want extra help."
                        } else {
                            "Turn this on to unlock the agent directory and explore offline support options."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSaving) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = onToggleAssist,
                        enabled = !isSaving
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, Divider)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (enabled) "Offline support directory enabled" else "Private self-service mode",
                        style = MaterialTheme.typography.labelLarge,
                        color = PrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (enabled) {
                            "SoulMatch only shows the directory. Any call, meeting, negotiation, or follow-up happens directly between you and the selected agent."
                        } else {
                            "You are managing your profile independently. Enable this anytime if you want to discover registered agents."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            if (enabled) {
                OutlinedButton(
                    onClick = onOpenAssist,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                ) {
                    Text("Open SoulMatch Assist", fontWeight = FontWeight.ExtraBold)
                }
            }
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

private fun Context.toVerificationDocumentPart(uri: Uri): MultipartBody.Part? {
    val contentType = contentResolver.getType(uri) ?: "application/octet-stream"
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType)?.lowercase(Locale.US)
        ?: if (contentType.contains("pdf", ignoreCase = true)) "pdf" else "jpg"
    val fileName = "trust-document-${System.currentTimeMillis()}.$extension"
    val body = bytes.toRequestBody(contentType.toMediaTypeOrNull())
    return MultipartBody.Part.createFormData("document", fileName, body)
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
    if (clean.isBlank()) return "SM-00000000"
    if (clean.startsWith("SM-", ignoreCase = true)) return clean.uppercase(Locale.getDefault())
    val compact = clean.replace("-", "").uppercase(Locale.getDefault())
    return "SM-${compact.take(8).padEnd(8, '0')}"
}

private fun TrustFactorData.isFirebaseTrustFactor(): Boolean {
    val text = listOf(key, label, detail).joinToString(" ").lowercase(Locale.getDefault())
    return "firebase" in text || "fcm" in text
}

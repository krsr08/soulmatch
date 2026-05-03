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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
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
    val openAssist: () -> Unit = onOpenAssist ?: {}
    val openFamilyBoard: () -> Unit = onOpenFamilyBoard ?: {}
    val openSubscription: () -> Unit = onSubscribe ?: {}
    val viewProfile: (String) -> Unit = onViewProfile ?: { _ -> }
    val context = LocalContext.current
    var localPhotoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

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
                        item {
                            PhotoGalleryCard(
                                photos = photos,
                                localPhotoUris = localPhotoUris,
                                photoPrivacyLabel = photoPrivacyLabel(data.photoPrivacy),
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
                            PartnerPreferencesCard(preferences = preferences, onSave = { ageMin, ageMax, religion, manglikPref ->
                                vm.updatePartnerPreferences(ageMin, ageMax, religion, manglikPref)
                            })
                        }
                        item {
                            SoulMatchAssistSummaryCard(
                                assistStatus = assistStatus,
                                onOpenAssist = openAssist
                            )
                        }
                        item {
                            TrustAndFamilyBoardCard(
                                profile = data,
                                familyDecisions = familyDecisions,
                                onOpenFamilyBoard = openFamilyBoard
                            )
                        }
                        if (viewers.isNotEmpty()) {
                            item {
                                SectionTitle(
                                    title = "Recent viewers",
                                    subtitle = "Warm signals from members already checking your profile",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                            items(viewers.take(5), key = { "${it.profileId}-${it.viewedAt}" }) { viewer ->
                                ViewerRow(viewer = viewer, onOpen = { viewProfile(viewer.profileId) })
                            }
                        }
                        item {
                            SectionTitle(
                                title = "Profile checklist",
                                subtitle = "Keep the profile complete and trustworthy",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                        items(checklist, key = { it.title }) { item ->
                            ChecklistRow(item = item, onEdit = { editSection(item.editStep) })
                        }
                        item {
                            PremiumCard(modifier = Modifier.padding(16.dp), containerColor = SurfaceWarm) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    SectionTitle("Privacy first", "Control photo access, visibility, contact filters, hidden profiles, and blocked profiles in one place")
                                    Button(onClick = openSettings, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Text("Open privacy settings")
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
    val completed = checklist.count { it.isComplete }
    val pendingRequired = checklist
        .filter { !it.isComplete && !it.statusLabel.equals("Optional", ignoreCase = true) }
        .map { it.title.lowercase(Locale.getDefault()) }
    val strengthDetail = if (pendingRequired.isEmpty()) {
        ProfileStrengthAdvisor.summary(profile)
    } else {
        "Next: ${pendingRequired.take(2).joinToString(", ")}."
    }
    val headerPhoto = localPhotoUris.firstOrNull()?.toString()
        ?: photos.firstOrNull { it.isPrimary }?.photoUrl
        ?: photos.firstOrNull()?.photoUrl
        ?: profile.primaryPhotoUrl
    PremiumHeader(
        eyebrow = "Account",
        title = profile.fullName(),
        subtitle = listOf(profile.occupation, profile.workingCity).filter { it.isNotBlank() }.joinToString(" | ").ifBlank { "Build a complete matrimonial profile" },
        trailing = {
            ProfilePhotoButton(
                photoUrl = headerPhoto,
                onClick = onUploadPhoto
            )
        }
    )
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp), containerColor = MaterialTheme.colorScheme.surface) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LabeledProgress(
                label = "Profile strength",
                value = profile.completionScore,
                detail = strengthDetail
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricPill("Sections", "$completed/${checklist.size}", modifier = Modifier.weight(1f), background = SurfaceWarm)
                MetricPill("Photos", photos.size.toString(), modifier = Modifier.weight(1f), background = SurfaceSoft)
                MetricPill("Plan", titleCase(subscription.planId), modifier = Modifier.weight(1f), accent = Success, background = SuccessSoft)
            }
            SignalChips(
                labels = listOf(
                    if (profile.profileStatus.equals("inactive", ignoreCase = true)) "Profile inactive" else "Profile active",
                    "Created by ${titleCase(profile.profileCreatedBy)}",
                    if (profile.isPartnerPrefSet) "Preferences synced" else "Preferences pending",
                    if (profile.trustScore > 0) "Trust ${profile.trustScore}%" else "Trust building"
                ),
                tone = ChipTone.Success
            )
            UpgradePlanGate(
                title = "Upgrade your member reach",
                detail = "Premium membership adds contact views, visitor insights, and stronger visibility in search.",
                onUpgrade = onSubscribe,
                compact = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onSubscribe, modifier = Modifier.weight(1f)) {
                    Text("Manage plan")
                }
                OutlinedButton(onClick = onSettings, modifier = Modifier.weight(1f)) {
                    Text("Privacy")
                }
            }
        }
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
    photoPrivacyLabel: String,
    uploadingPhotos: Boolean,
    onUpload: () -> Unit,
    onDeleteLocal: (Uri) -> Unit,
    onDelete: (String) -> Unit,
    onSetPrimary: (String) -> Unit
) {
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionTitle(
                title = "Photos and trust",
                subtitle = "Primary photo, gallery, verification, and photo privacy",
                actionLabel = if (uploadingPhotos) null else "Add",
                onAction = if (uploadingPhotos) null else onUpload
            )
            if (uploadingPhotos) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Uploading photos", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
            Surface(shape = RoundedCornerShape(16.dp), color = SurfaceWarm, border = BorderStroke(1.dp, Divider)) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = PrimaryDark)
                    Text("Photo visibility: $photoPrivacyLabel", style = MaterialTheme.typography.bodyMedium, color = PrimaryDark, fontWeight = FontWeight.SemiBold)
                }
            }
            if (photos.isEmpty() && localPhotoUris.isEmpty()) {
                Surface(shape = RoundedCornerShape(18.dp), color = SurfaceSoft, border = BorderStroke(1.dp, Divider)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(34.dp))
                        Text("No profile photos yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Add at least one clear photo to improve trust and ranking.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Button(onClick = onUpload, enabled = !uploadingPhotos) {
                            Text("Add photos")
                        }
                    }
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(localPhotoUris, key = { "local-${it}" }) { uri ->
                        LocalPhotoTile(uri = uri, onDelete = onDeleteLocal)
                    }
                    items(photos, key = { it.photoId }) { photo ->
                        PhotoTile(photo = photo, onDelete = onDelete, onSetPrimary = onSetPrimary)
                    }
                }
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
private fun TrustAndFamilyBoardCard(
    profile: ProfileData,
    familyDecisions: List<FamilyDecisionData>,
    onOpenFamilyBoard: () -> Unit
) {
    val activeDecisions = familyDecisions.filterNot { it.status.equals("archived", ignoreCase = true) }
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = SurfaceWarm) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Trust and family decisions", "Use proof signals and a shared shortlist before moving to calls")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricPill(
                    "Trust",
                    if (profile.trustScore > 0) "${profile.trustScore}%" else "New",
                    modifier = Modifier.weight(1f),
                    background = if (profile.trustScore >= 80) SuccessSoft else SurfaceSoft,
                    accent = if (profile.trustScore >= 80) Success else TextSecondary
                )
                MetricPill(
                    "Family board",
                    activeDecisions.size.toString(),
                    modifier = Modifier.weight(1f),
                    background = SurfaceSoft
                )
            }
            val trustLabels = profile.trustSignals.ifEmpty {
                listOf("Complete profile", "Add photos", "Request verification")
            }
            SignalChips(labels = trustLabels.take(3), tone = ChipTone.Info)
            Button(onClick = onOpenFamilyBoard, modifier = Modifier.fillMaxWidth()) {
                Text("Open family decision board")
            }
        }
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
    onSave: (Int, Int, String, String) -> Unit
) {
    var ageMin by remember(preferences.ageMin) { mutableIntStateOf(preferences.ageMin) }
    var ageMax by remember(preferences.ageMax) { mutableIntStateOf(preferences.ageMax) }
    var religion by remember(preferences.religion) { mutableStateOf(preferences.religion.orEmpty()) }
    var manglikPref by remember(preferences.manglikPref) { mutableStateOf(preferences.manglikPref) }
    var selectedTab by remember { mutableStateOf("Basics") }

    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = SurfaceWarm) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Partner preferences", "Clearly define the age, community, and horoscope signals used by Smart Search")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("Basics", "Community", "Horoscope").forEach { tab ->
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
                    Text("SoulMatch ranks profiles inside this age range higher.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                "Community" -> {
                    OutlinedTextField(
                        value = religion,
                        onValueChange = { religion = it },
                        label = { Text("Preferred religion") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text("Leave blank if you want matches from every religion or community.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
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
                MetricPill("Religion", religion.ifBlank { "Any" }, modifier = Modifier.weight(1f), background = SurfaceSoft)
            }
            MetricPill("Manglik", titleCase(manglikPref), modifier = Modifier.fillMaxWidth(), background = SurfaceSoft)
            Button(onClick = { onSave(ageMin, ageMax, religion, manglikPref) }, modifier = Modifier.fillMaxWidth()) {
                Text("Save preferences")
            }
        }
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
private fun ChecklistRow(item: ProfileChecklistItem, onEdit: () -> Unit) {
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = when {
                    item.isComplete -> SuccessSoft
                    item.statusLabel.equals("Optional", ignoreCase = true) -> SurfaceSoft
                    else -> ErrorSoft
                },
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = when {
                            item.isComplete -> Success
                            item.statusLabel.equals("Optional", ignoreCase = true) -> TextSecondary
                            else -> Error
                        }
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                Text("Edit")
            }
        }
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

package com.soulmatch.app.ui.screens.profile

import android.content.Context
import android.content.Intent

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.data.models.CompatibilityData
import com.soulmatch.app.data.models.ProfileData
import com.soulmatch.app.data.models.SubscriptionData
import com.soulmatch.app.data.models.fullName
import com.soulmatch.app.ui.formatDate
import com.soulmatch.app.ui.components.ChipTone
import com.soulmatch.app.ui.components.CompatibilityBar
import com.soulmatch.app.ui.components.DetailGrid
import com.soulmatch.app.ui.components.MemberPhoto
import com.soulmatch.app.ui.components.PremiumCard
import com.soulmatch.app.ui.components.PremiumScreen
import com.soulmatch.app.ui.components.ReportConcernDialog
import com.soulmatch.app.ui.components.SectionTitle
import com.soulmatch.app.ui.components.SignalChip
import com.soulmatch.app.ui.components.UpgradePlanGate
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.Error
import com.soulmatch.app.ui.theme.ErrorSoft
import com.soulmatch.app.ui.theme.Info
import com.soulmatch.app.ui.theme.InfoSoft
import com.soulmatch.app.ui.theme.PrimaryDark
import com.soulmatch.app.ui.theme.Success
import com.soulmatch.app.ui.theme.SuccessSoft
import com.soulmatch.app.ui.theme.SurfaceSoft
import com.soulmatch.app.ui.theme.SurfaceWarm
import com.soulmatch.app.ui.theme.TextSecondary
import com.soulmatch.app.ui.titleCase
import com.soulmatch.app.ui.viewmodels.ProfileDetailViewModel
import com.soulmatch.app.ui.viewmodels.SubscriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailScreen(
    onBack: () -> Unit = {},
    onViewProfile: ((String) -> Unit)? = null,
    onOpenChat: ((String, String) -> Unit)? = null,
    onSubscribe: (() -> Unit)? = null,
    onEditSection: ((Int) -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    profileId: String = "",
    chatId: String = "",
    participantName: String = "",
    vm: ProfileDetailViewModel = hiltViewModel(),
    subscriptionVm: SubscriptionViewModel = hiltViewModel()
) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val compatibility by vm.compatibility.collectAsStateWithLifecycle()
    val loading by vm.isLoading.collectAsStateWithLifecycle()
    val interestSent by vm.interestSent.collectAsStateWithLifecycle()
    val shortlisted by vm.shortlisted.collectAsStateWithLifecycle()
    val canChat by vm.canChat.collectAsStateWithLifecycle()
    val photoActionStatus by vm.status.collectAsStateWithLifecycle()
    val subscription by subscriptionVm.subscription.collectAsStateWithLifecycle()
    val openChat: (String, String) -> Unit = onOpenChat ?: { _, _ -> }
    val openSubscription: (() -> Unit)? = onSubscribe
    val hasActiveMembership = subscription.hasActivePaidMembership()
    val context = LocalContext.current
    var actionsExpanded by remember { mutableStateOf(false) }
    var actionNotice by remember { mutableStateOf<String?>(null) }
    var reportDialogOpen by remember { mutableStateOf(false) }

    LaunchedEffect(profileId) {
        vm.load(profileId)
    }
    LaunchedEffect(Unit) {
        subscriptionVm.load()
    }
    LaunchedEffect(photoActionStatus) {
        if (!photoActionStatus.isNullOrBlank()) {
            actionNotice = photoActionStatus
            vm.clearStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profile?.fullName() ?: "Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.toggleShortlist() }) {
                        Icon(
                            imageVector = if (shortlisted) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (shortlisted) "Saved" else "Save"
                        )
                    }
                    Box {
                        IconButton(onClick = { actionsExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Profile actions")
                        }
                        DropdownMenu(expanded = actionsExpanded, onDismissRequest = { actionsExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Hide profile") },
                                leadingIcon = { Icon(Icons.Filled.VisibilityOff, contentDescription = null) },
                                onClick = {
                                    actionsExpanded = false
                                    vm.hideProfile()
                                    actionNotice = "This profile has been hidden locally for this session."
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Block member") },
                                leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null) },
                                onClick = {
                                    actionsExpanded = false
                                    vm.blockProfile()
                                    actionNotice = "This member is blocked and removed from discovery."
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Report concern") },
                                leadingIcon = { Icon(Icons.Filled.Report, contentDescription = null) },
                                onClick = {
                                    actionsExpanded = false
                                    reportDialogOpen = true
                                }
                            )
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
            if (loading && profile == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                profile?.let { data ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 132.dp)
                    ) {
                        item {
                            ProfileHero(
                                profile = data,
                                compatibilityScore = compatibility.overallScore,
                                onRequestPhoto = { vm.requestPhotoAccess() }
                            )
                        }
                        item {
                            PrimaryActionPanel(
                                interestSent = interestSent,
                                canChat = canChat,
                                shortlisted = shortlisted,
                                onSendInterest = { vm.sendInterest() },
                                onOpenChat = { openChat(data.userId, data.fullName()) },
                                onSave = { vm.toggleShortlist() },
                                onAddFamilyReview = {
                                    actionNotice = "Adding this profile to your family board..."
                                    vm.addToFamilyBoard()
                                },
                                onShare = { shareProfileWithFamily(context, data, compatibility.overallScore) }
                            )
                        }
                        if (!actionNotice.isNullOrBlank()) {
                            item {
                                PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = SurfaceWarm) {
                                    Text(actionNotice ?: "", style = MaterialTheme.typography.bodyMedium, color = PrimaryDark, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        if (!canChat && !hasActiveMembership) {
                            item {
                                UpgradePlanGate(
                                    title = "Upgrade to contact this member",
                                    detail = "Premium plans unlock verified contact views, priority introductions, and richer profile access.",
                                    onUpgrade = openSubscription,
                                    compact = true,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        } else if (!canChat) {
                            item {
                                PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = SurfaceSoft) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Chat opens after mutual acceptance", style = MaterialTheme.typography.titleSmall, color = PrimaryDark, fontWeight = FontWeight.Bold)
                                        Text("Your membership is active. Send interest first; chat becomes available when both sides accept.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }
                            }
                        }
                        item {
                            ProfileOverview(profile = data, compatibility = compatibility)
                        }
                        item {
                            DetailSection(
                                title = "Personal snapshot",
                                subtitle = "The details families usually scan first",
                                rows = listOf(
                                    "Religion" to data.religion,
                                    "Community" to data.caste,
                                    "Mother tongue" to data.motherTongue,
                                    "Marital status" to titleCase(data.maritalStatus),
                                    "Height" to data.heightCm?.let { "$it cm" }.orEmpty(),
                                    "Weight" to data.weightKg?.let { "$it kg" }.orEmpty(),
                                    "Complexion" to data.complexion,
                                    "Body type" to data.bodyType,
                                    "Blood group" to data.bloodGroup
                                )
                            )
                        }
                        item {
                            DetailSection(
                                title = "Work and lifestyle",
                                subtitle = "Career, city, habits, and daily rhythm",
                                rows = buildList {
                                    add("Education" to data.educationLevel)
                                    add("Occupation" to data.occupation)
                                    add("Income" to data.annualIncome)
                                    add("Working city" to data.workingCity)
                                    add(
                                        (if (data.hideLastSeen) "Updated" else "Last seen") to
                                            formatDate((if (data.hideLastSeen) data.updatedAt else data.lastLogin ?: data.updatedAt))
                                    )
                                    add("Diet" to titleCase(data.diet))
                                    add("Smoking" to titleCase(data.smoking))
                                    add("Drinking" to titleCase(data.drinking))
                                }
                            )
                        }
                        item {
                            DetailSection(
                                title = "Family and traditions",
                                subtitle = "Family context and tradition preferences",
                                rows = listOf(
                                    "Family type" to titleCase(data.familyType),
                                    "Family city" to data.familyCity,
                                    "Father occupation" to data.fatherOccupation,
                                    "Mother occupation" to data.motherOccupation,
                                    "Brothers" to data.numBrothers?.toString().orEmpty(),
                                    "Sisters" to data.numSisters?.toString().orEmpty(),
                                    "Rashi" to data.rashi,
                                    "Nakshatra" to data.nakshatra,
                                    "Manglik" to if (data.isManglik) "Yes" else "No",
                                    "Birth city" to data.birthCity,
                                    "Gotra" to data.gotra
                                )
                            )
                        }
                        item {
                            TrustProfileSection(profile = data)
                        }
                    }
                }
            }
        }
    }
    if (reportDialogOpen) {
        ReportConcernDialog(
            profileName = profile?.fullName() ?: "this member",
            onDismiss = { reportDialogOpen = false },
            onSubmit = { concern ->
                vm.reportConcern(concern)
                actionNotice = "Concern saved. You can edit or delete it from Privacy."
                reportDialogOpen = false
            }
        )
    }
}

@Composable
private fun ProfileHero(profile: ProfileData, compatibilityScore: Int, onRequestPhoto: () -> Unit) {
    val isPhotoBlocked = profile.primaryPhotoUrl.isNullOrBlank() &&
        !profile.canViewPhoto &&
        !profile.photoPrivacy.equals("all", ignoreCase = true)
    val requestPending = profile.photoAccessStatus.equals("pending", ignoreCase = true)
    PremiumCard(
        modifier = Modifier.padding(16.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
            MemberPhoto(
                photoUrl = profile.primaryPhotoUrl,
                contentDescription = profile.fullName(),
                modifier = Modifier
                    .fillMaxSize(),
                shape = RoundedCornerShape(8.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xDD101828)),
                            startY = 140f
                        )
                    )
            )
            if (isPhotoBlocked) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, Divider.copy(alpha = 0.8f))
                    ) {
                        Text(
                            "Photo is private",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(onClick = onRequestPhoto, enabled = !requestPending) {
                        Icon(Icons.Filled.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(if (requestPending) "Request sent" else "Request photo")
                    }
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        profile.fullName(),
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (profile.verificationStatus.equals("verified", ignoreCase = true)) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = "Verified",
                            tint = Success,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Text(
                    listOfNotNull(
                        profile.heightCm?.let { "$it cm" },
                        profile.age.takeIf { it > 0 }?.let { "$it yrs" }
                    ).joinToString(" | "),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.92f)
                )
                Text(
                    listOfNotNull(
                        profile.educationLevel.takeIf { it.isNotBlank() },
                        profile.occupation.takeIf { it.isNotBlank() },
                        profile.workingCity.ifBlank { profile.familyCity }.takeIf { it.isNotBlank() }
                    ).joinToString(" | ").ifBlank { "Education, occupation, and location will appear here" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.86f)
                )
                SignalChip(
                    label = "$compatibilityScore% match compatibility",
                    tone = ChipTone.Success
                )
            }
        }
    }
}

@Composable
private fun PrimaryActionPanel(
    interestSent: Boolean,
    canChat: Boolean,
    shortlisted: Boolean,
    onSendInterest: () -> Unit,
    onOpenChat: () -> Unit,
    onSave: () -> Unit,
    onAddFamilyReview: () -> Unit,
    onShare: () -> Unit
) {
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp), containerColor = MaterialTheme.colorScheme.surface) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSendInterest,
                    enabled = !interestSent,
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(if (interestSent) "Interest sent" else "Send interest")
                }
                OutlinedButton(
                    onClick = onOpenChat,
                    enabled = canChat,
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(if (canChat) "Open chat" else "Chat locked")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onSave, modifier = Modifier.weight(1f)) {
                    Icon(if (shortlisted) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(if (shortlisted) "Shortlisted" else "Save")
                }
                OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Share family")
                }
            }
            OutlinedButton(onClick = onAddFamilyReview, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Icon(Icons.Filled.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Add to family board")
            }
        }
    }
}

private fun shareProfileWithFamily(context: Context, profile: ProfileData, compatibilityScore: Int) {
    val safeName = listOfNotNull(
        profile.firstName.ifBlank { null },
        profile.lastName.take(1).takeIf { it.isNotBlank() }?.let { "$it." }
    ).joinToString(" ").ifBlank { "A SoulMatch member" }
    val summary = buildString {
        appendLine("SoulMatch family review")
        appendLine("$safeName is ready for secure family review.")
        appendLine("Profile Match: $compatibilityScore%")
        profile.occupation.ifBlank { null }?.let { appendLine("Profession: $it") }
        profile.workingCity.ifBlank { profile.familyCity }.ifBlank { null }?.let { appendLine("City: $it") }
        appendLine()
        append("Open SoulMatch to review verified details. Please share only with trusted family members.")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "SoulMatch family review")
        putExtra(Intent.EXTRA_TEXT, summary)
    }
    context.startActivity(Intent.createChooser(intent, "Share safely with family"))
}

private fun SubscriptionData.hasActivePaidMembership(): Boolean {
    return isActive && planId.isNotBlank() && !planId.equals("free", ignoreCase = true)
}

@Composable
private fun ProfileOverview(profile: ProfileData, compatibility: CompatibilityData) {
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = SurfaceWarm) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionTitle("Overview", "Why this profile is worth a closer look")
            CompatibilityBar(score = compatibility.overallScore)
            DetailGrid(
                rows = listOf(
                    "Preferences" to "${compatibility.breakdown?.preferences ?: 0}%",
                    "Personality" to "${compatibility.breakdown?.personality ?: 0}%",
                    "Horoscope" to "${compatibility.breakdown?.horoscope ?: 0}%",
                    "Readiness" to "${profile.completionScore}%"
                )
            )
        }
    }
}

@Composable
private fun TrustProfileSection(profile: ProfileData) {
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = SurfaceWarm) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Trust profile", "A concise view of trust, seriousness, and profile privacy")
            CompatibilityBar(score = profile.trustScore.coerceIn(0, 100))
            DetailGrid(
                rows = listOf(
                    "Trust score" to if (profile.trustScore > 0) "${profile.trustScore}%" else "Building",
                    "Trust level" to titleCase(profile.trustLevel),
                    "Seriousness" to if (profile.seriousnessScore > 0) "${profile.seriousnessScore}% ${titleCase(profile.seriousnessLevel)}" else "Building",
                    "Verification" to titleCase(profile.verificationStatus),
                    "Photo privacy" to titleCase(profile.photoPrivacy.replace('_', ' '))
                )
            )
        }
    }
}

@Composable
private fun DetailSection(title: String, subtitle: String, rows: List<Pair<String, String>>) {
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionTitle(title, subtitle)
            DetailGrid(rows = rows)
        }
    }
}


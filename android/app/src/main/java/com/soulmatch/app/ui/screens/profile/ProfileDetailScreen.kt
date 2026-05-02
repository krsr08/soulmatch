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
import com.soulmatch.app.data.models.fullName
import com.soulmatch.app.ui.components.ChipTone
import com.soulmatch.app.ui.components.CompatibilityBar
import com.soulmatch.app.ui.components.DetailGrid
import com.soulmatch.app.ui.components.MemberPhoto
import com.soulmatch.app.ui.components.PremiumCard
import com.soulmatch.app.ui.components.PremiumScreen
import com.soulmatch.app.ui.components.ReportConcernDialog
import com.soulmatch.app.ui.components.SectionTitle
import com.soulmatch.app.ui.components.SignalChip
import com.soulmatch.app.ui.components.SignalChips
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
    vm: ProfileDetailViewModel = hiltViewModel()
) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val compatibility by vm.compatibility.collectAsStateWithLifecycle()
    val loading by vm.isLoading.collectAsStateWithLifecycle()
    val interestSent by vm.interestSent.collectAsStateWithLifecycle()
    val shortlisted by vm.shortlisted.collectAsStateWithLifecycle()
    val canChat by vm.canChat.collectAsStateWithLifecycle()
    val openChat: (String, String) -> Unit = onOpenChat ?: { _, _ -> }
    val openSubscription: (() -> Unit)? = onSubscribe
    val context = LocalContext.current
    var actionsExpanded by remember { mutableStateOf(false) }
    var actionNotice by remember { mutableStateOf<String?>(null) }
    var reportDialogOpen by remember { mutableStateOf(false) }

    LaunchedEffect(profileId) {
        vm.load(profileId)
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
                        contentPadding = PaddingValues(bottom = 28.dp)
                    ) {
                        item {
                            ProfileHero(
                                profile = data,
                                compatibilityScore = compatibility.overallScore
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
                                onShare = { shareProfileWithFamily(context, data, compatibility.overallScore) }
                            )
                        }
                        if (!canChat) {
                            item {
                                UpgradePlanGate(
                                    title = "Upgrade to contact this member",
                                    detail = "Premium plans unlock verified contact views, priority introductions, and richer profile access.",
                                    onUpgrade = openSubscription,
                                    compact = true,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                        if (!actionNotice.isNullOrBlank()) {
                            item {
                                PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = SurfaceWarm) {
                                    Text(actionNotice ?: "", style = MaterialTheme.typography.bodyMedium, color = PrimaryDark, fontWeight = FontWeight.SemiBold)
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
                                rows = listOf(
                                    "Education" to data.educationLevel,
                                    "Occupation" to data.occupation,
                                    "Income" to data.annualIncome,
                                    "Working city" to data.workingCity,
                                    "Diet" to titleCase(data.diet),
                                    "Smoking" to titleCase(data.smoking),
                                    "Drinking" to titleCase(data.drinking)
                                )
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
                            PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    SectionTitle("About me", "Specific introductions help families and members decide with care")
                                    Text(
                                        data.aboutMe.ifBlank { "This member has not added an introduction yet." },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                        item {
                            FutureActionsCard(
                                onHide = {
                                    vm.hideProfile()
                                    actionNotice = "This profile has been hidden locally for this session."
                                },
                                onBlock = {
                                    vm.blockProfile()
                                    actionNotice = "This member is blocked and removed from discovery."
                                },
                                onReport = { reportDialogOpen = true }
                            )
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
private fun ProfileHero(profile: ProfileData, compatibilityScore: Int) {
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
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SignalChips(
                    labels = listOf(
                        "$compatibilityScore% compatibility",
                        "Created by ${titleCase(profile.profileCreatedBy)}",
                        if (profile.profileStatus.equals("inactive", ignoreCase = true)) "Inactive" else "Active",
                        if (profile.completionScore >= 80) "Complete profile" else "Profile in progress",
                        if (profile.photoPrivacy == "matches_only") "Selective photo privacy" else "Photo visible"
                    ),
                    tone = ChipTone.Success
                )
                Text(profile.fullName(), style = MaterialTheme.typography.displayMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                Text(
                    listOfNotNull(
                        profile.age.takeIf { it > 0 }?.let { "$it yrs" },
                        profile.workingCity.ifBlank { profile.familyCity.ifBlank { null } },
                        profile.occupation.ifBlank { null },
                        profile.motherTongue.ifBlank { null }
                    ).joinToString(" | "),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.92f)
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
        }
    }
}

private fun shareProfileWithFamily(context: Context, profile: ProfileData, compatibilityScore: Int) {
    val summary = buildString {
        appendLine("SoulMatch profile: ${profile.fullName()}")
        appendLine("Profile Match: $compatibilityScore%")
        profile.age.takeIf { it > 0 }?.let { appendLine("Age: $it yrs") }
        profile.heightCm?.let { appendLine("Height: $it cm") }
        profile.occupation.ifBlank { null }?.let { appendLine("Profession: $it") }
        profile.workingCity.ifBlank { profile.familyCity }.ifBlank { null }?.let { appendLine("Location: $it") }
        profile.religion.ifBlank { null }?.let { appendLine("Religion: $it") }
        profile.caste.ifBlank { null }?.let { appendLine("Community: $it") }
        appendLine()
        append("Open SoulMatch to review this profile with the family.")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "SoulMatch profile: ${profile.fullName()}")
        putExtra(Intent.EXTRA_TEXT, summary)
    }
    context.startActivity(Intent.createChooser(intent, "Share profile"))
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
            SignalChips(
                labels = listOf("Verified path ready", "Family-share ready", "Privacy visible"),
                tone = ChipTone.Info
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

@Composable
private fun FutureActionsCard(
    onHide: () -> Unit,
    onBlock: () -> Unit,
    onReport: () -> Unit
) {
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = SurfaceSoft) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Safety and privacy actions", "Room for profile controls without crowding the main actions")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SafetyPill("Hide profile", InfoSoft, Info, Modifier.weight(1f), onClick = onHide)
                SafetyPill("Block", ErrorSoft, Error, Modifier.weight(1f), onClick = onBlock)
                SafetyPill("Report", SurfaceWarm, PrimaryDark, Modifier.weight(1f), onClick = onReport)
            }
            Text("Hide removes this member from your feed. Block prevents contact. Report sends a concern to SoulMatch support.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
private fun SafetyPill(label: String, color: Color, content: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = color,
        border = BorderStroke(1.dp, Divider)
    ) {
        Text(
            label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = content,
            fontWeight = FontWeight.Bold
        )
    }
}

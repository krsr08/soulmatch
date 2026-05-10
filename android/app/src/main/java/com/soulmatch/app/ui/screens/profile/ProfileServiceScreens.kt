package com.soulmatch.app.ui.screens.profile

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.data.models.AdvisorSummaryData
import com.soulmatch.app.data.models.AssistAgentStatsData
import com.soulmatch.app.ui.components.ChipTone
import com.soulmatch.app.ui.components.FilterChoiceChip
import com.soulmatch.app.ui.components.MemberPhoto
import com.soulmatch.app.ui.components.MetricPill
import com.soulmatch.app.ui.components.PremiumCard
import com.soulmatch.app.ui.components.PremiumHeader
import com.soulmatch.app.ui.components.PremiumScreen
import com.soulmatch.app.ui.components.ProfileStrengthAdvisor
import com.soulmatch.app.ui.components.SectionTitle
import com.soulmatch.app.ui.components.SignalChips
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.Error
import com.soulmatch.app.ui.theme.ErrorSoft
import com.soulmatch.app.ui.theme.Info
import com.soulmatch.app.ui.theme.InfoSoft
import com.soulmatch.app.ui.theme.Success
import com.soulmatch.app.ui.theme.SuccessSoft
import com.soulmatch.app.ui.theme.SurfaceSoft
import com.soulmatch.app.ui.theme.SurfaceWarm
import com.soulmatch.app.ui.theme.TextSecondary
import com.soulmatch.app.ui.titleCase
import com.soulmatch.app.ui.viewmodels.MyProfileViewModel
import com.soulmatch.app.ui.viewmodels.SettingsViewModel

private const val SupportEmail = "support@soulmatch.app"
private const val SupportPhone = "+91 90000 90000"

private data class FeaturePoint(
    val title: String,
    val detail: String
)

private data class FaqItem(
    val question: String,
    val answer: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoulMatchAssistScreen(
    onBack: () -> Unit,
    onOpenFamilyStep: () -> Unit,
    onOpenSubscription: () -> Unit,
    vm: MyProfileViewModel = hiltViewModel()
) {
    val assist by vm.assistStatus.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val isSaving by vm.isSavingAssist.collectAsStateWithLifecycle()
    var assistEnabled by remember(assist.profileId, assist.isOptedIn) { mutableStateOf(assist.isOptedIn) }
    var shareMode by remember(assist.profileId, assist.shareMode) { mutableStateOf(assist.shareMode) }
    var selectedAdvisorIds by remember(assist.profileId, assist.selectedAdvisorIds) { mutableStateOf(assist.selectedAdvisorIds) }
    var preferredWindow by remember(assist.profileId, assist.preferredContactWindow) { mutableStateOf(assist.preferredContactWindow) }
    var familyContactName by remember(assist.profileId, assist.familyContactName) { mutableStateOf(assist.familyContactName) }
    var familyContactPhone by remember(assist.profileId, assist.familyContactPhone) { mutableStateOf(assist.familyContactPhone) }
    var notes by remember(assist.profileId, assist.notes) { mutableStateOf(assist.notes) }
    var editFamilyDetails by remember(assist.profileId, assist.familyContactName, assist.familyContactPhone, assist.preferredContactWindow, assist.notes) {
        mutableStateOf(
            assist.familyContactName.isBlank() &&
                assist.familyContactPhone.isBlank() &&
                assist.preferredContactWindow.isBlank() &&
                assist.notes.isBlank()
        )
    }
    val locationSummary = listOf(
        assist.location.locality,
        assist.location.city,
        assist.location.state,
        assist.location.pincode
    ).filter { it.isNotBlank() }.joinToString(", ")
    val hasSavedFamilyDetails = familyContactName.isNotBlank() || familyContactPhone.isNotBlank() || preferredWindow.isNotBlank() || notes.isNotBlank()
    val desiredSupportLevel = if (assistEnabled) "advisor_assisted" else "self_service"
    val showAgentSelection = assistEnabled
    val showFamilyEditor = assistEnabled && (editFamilyDetails || !hasSavedFamilyDetails)
    val selectedAdvisorIdsForSave = if (showAgentSelection) selectedAdvisorIds else emptyList()
    val hasUnsavedAssistChanges =
        assistEnabled != assist.isOptedIn ||
            desiredSupportLevel != assist.supportLevel ||
            shareMode != assist.shareMode ||
            selectedAdvisorIdsForSave != assist.selectedAdvisorIds ||
            preferredWindow != assist.preferredContactWindow ||
            familyContactName != assist.familyContactName ||
            familyContactPhone != assist.familyContactPhone ||
            notes != assist.notes
    val showSaveCta = showFamilyEditor || hasUnsavedAssistChanges
    val canSaveSelection = !showAgentSelection || selectedAdvisorIds.isNotEmpty()
    val saveAssistChanges = {
        vm.updateAssistStatus(
            isOptedIn = assistEnabled,
            supportLevel = desiredSupportLevel,
            shareMode = shareMode,
            selectedAdvisorIds = selectedAdvisorIdsForSave,
            preferredContactWindow = preferredWindow,
            familyContactName = familyContactName,
            familyContactPhone = familyContactPhone,
            notes = notes
        )
    }

    DrawerDestinationScaffold(
        title = "SoulMatch Assist",
        onBack = onBack
    ) { padding ->
        PremiumScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    PremiumHeader(
                        eyebrow = "SoulMatch Assist",
                        title = "Connect with active local agents",
                        subtitle = "You can contact these registered agents directly for offline support. SoulMatch only lists the directory and does not participate in the discussion."
                    )
                }
                item {
                    AssistStatsRow(stats = assist.agentStats, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
                item {
                    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = SurfaceWarm) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SectionTitle("Control your assistance preference", "Enable SoulMatch Assistance if you want agent discovery and offline support options saved to your profile.")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        if (assistEnabled) "Assistance is enabled" else "Assistance is disabled",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        if (assistEnabled) {
                                            "You can save family contact details and share your profile with selected local agents."
                                        } else {
                                            "Turn this on only when you want offline agent discovery support."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Switch(
                                    checked = assistEnabled,
                                    onCheckedChange = { enabled ->
                                        assistEnabled = enabled
                                        if (!enabled) {
                                            shareMode = "single"
                                            selectedAdvisorIds = emptyList()
                                        }
                                    }
                                )
                            }
                            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, Divider)) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("How this works", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Agents may help with introductions and profile handling offline. SoulMatch will not join calls, negotiate, or manage any engagement between you and the agent.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            if (locationSummary.isNotBlank()) {
                                MetricPill("Family location", locationSummary, background = MaterialTheme.colorScheme.surface)
                            }
                        }
                    }
                }
                if (hasSavedFamilyDetails && !editFamilyDetails) {
                    item {
                        FamilyAssistSummaryCard(
                            locationSummary = locationSummary,
                            familyContactName = familyContactName,
                            familyContactPhone = familyContactPhone,
                            preferredWindow = preferredWindow,
                            notes = notes,
                            onEdit = { editFamilyDetails = true },
                            onReset = {
                                familyContactName = ""
                                familyContactPhone = ""
                                preferredWindow = ""
                                notes = ""
                                editFamilyDetails = true
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                } else if (showFamilyEditor) {
                    item {
                        PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                SectionTitle(
                                    if (hasSavedFamilyDetails) "Edit family details" else "Add family details",
                                    "These details are shared only with the agents you select."
                                )
                                OutlinedTextField(
                                    value = familyContactName,
                                    onValueChange = { familyContactName = it },
                                    label = { Text("Family contact name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = familyContactPhone,
                                    onValueChange = { familyContactPhone = it.filter(Char::isDigit).take(10) },
                                    label = { Text("Family contact number") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = preferredWindow,
                                    onValueChange = { preferredWindow = it },
                                    label = { Text("Preferred contact window") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    label = { Text("What should the advisor know?") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3
                                )
                            }
                        }
                    }
                }
                if (showAgentSelection) {
                    item {
                        PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = SurfaceWarm) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                SectionTitle(
                                    "Share your profile with agents",
                                    "Choose whether to share with one trusted agent or multiple agents, then select who should receive your profile."
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChoiceChip(
                                        label = "Single agent",
                                        selected = shareMode == "single",
                                        onClick = {
                                            shareMode = "single"
                                            selectedAdvisorIds = selectedAdvisorIds.take(1)
                                        }
                                    )
                                    FilterChoiceChip(
                                        label = "Multiple agents",
                                        selected = shareMode == "multiple",
                                        onClick = { shareMode = "multiple" }
                                    )
                                }
                                Text(
                                    if (selectedAdvisorIds.isEmpty()) "Select at least one agent to share your profile."
                                    else "${selectedAdvisorIds.size} agent${if (selectedAdvisorIds.size == 1) "" else "s"} selected for your profile.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
                if (showSaveCta) {
                    item {
                        Button(
                            onClick = {
                                saveAssistChanges()
                                if (hasSavedFamilyDetails || familyContactName.isNotBlank() || familyContactPhone.isNotBlank() || preferredWindow.isNotBlank() || notes.isNotBlank()) {
                                    editFamilyDetails = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            enabled = !isLoading && !isSaving && canSaveSelection
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Save SoulMatch Assistance")
                        }
                    }
                }
                if (showAgentSelection && !assist.readiness.canAutoAssign) {
                    item {
                        StatusInfoCard(
                            title = "Add stronger locality details for advisor routing",
                            detail = "Add family city and pincode in your family section so SoulMatch can assign the right nearby advisor instead of using a broad city guess.",
                            toneColor = InfoSoft,
                            contentColor = Info,
                            actionLabel = "Update family section",
                            onAction = onOpenFamilyStep
                        )
                    }
                }
                assist.advisor?.let { advisor ->
                    item {
                        PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                SectionTitle("Best-fit agent for your profile", "This recommendation is based on your current location, language, and member profile signals.")
                                AdvisorDirectoryCard(advisor = advisor, isHighlighted = true)
                            }
                        }
                    }
                }
                if (assist.recommendations.isNotEmpty()) {
                    item {
                        SectionTitle(
                            title = "Recommended agents",
                            subtitle = "These are the strongest matches for your current profile and family context.",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(assist.recommendations.take(3), key = { "rec-${it.advisorId}" }) { advisor ->
                        AdvisorDirectoryCard(
                            advisor = advisor,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            selected = selectedAdvisorIds.contains(advisor.advisorId),
                            selectionEnabled = showAgentSelection,
                            onToggleSelection = {
                                selectedAdvisorIds = if (shareMode == "single") {
                                    if (selectedAdvisorIds.contains(advisor.advisorId)) emptyList() else listOf(advisor.advisorId)
                                } else {
                                    if (selectedAdvisorIds.contains(advisor.advisorId)) selectedAdvisorIds - advisor.advisorId else selectedAdvisorIds + advisor.advisorId
                                }
                            }
                        )
                    }
                }
                item {
                    SectionTitle(
                        title = "All active agents",
                        subtitle = "Verified agents have completed platform review. Unverified agents are still pending review and should be handled with extra caution.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                if (assist.agents.isEmpty()) {
                    item {
                        PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = SurfaceSoft) {
                            Text("No active agents are available right now. Please check again shortly.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                } else {
                    items(assist.agents, key = { "agent-${it.advisorId}" }) { advisor ->
                        AdvisorDirectoryCard(
                            advisor = advisor,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            selected = selectedAdvisorIds.contains(advisor.advisorId),
                            selectionEnabled = showAgentSelection,
                            onToggleSelection = {
                                selectedAdvisorIds = if (shareMode == "single") {
                                    if (selectedAdvisorIds.contains(advisor.advisorId)) emptyList() else listOf(advisor.advisorId)
                                } else {
                                    if (selectedAdvisorIds.contains(advisor.advisorId)) selectedAdvisorIds - advisor.advisorId else selectedAdvisorIds + advisor.advisorId
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FamilyAssistSummaryCard(
    locationSummary: String,
    familyContactName: String,
    familyContactPhone: String,
    preferredWindow: String,
    notes: String,
    onEdit: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Divider)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Saved family details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (locationSummary.isNotBlank()) {
                        Text(locationSummary, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit saved family details", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onReset) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reset saved family details", tint = TextSecondary)
                    }
                }
            }
            if (familyContactName.isNotBlank()) {
                SavedFamilyDetailRow("Contact", familyContactName)
            }
            if (familyContactPhone.isNotBlank()) {
                SavedFamilyDetailRow("Phone", familyContactPhone)
            }
            if (preferredWindow.isNotBlank()) {
                SavedFamilyDetailRow("Preferred time", preferredWindow)
            }
            if (notes.isNotBlank()) {
                Text(notes, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun SavedFamilyDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(0.42f))
        Text(
            value,
            modifier = Modifier.weight(0.58f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            textAlign = TextAlign.End,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AssistStatsRow(
    stats: AssistAgentStatsData,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AssistStatCard("Active", stats.activeCount.toString(), Modifier.weight(1f), MaterialTheme.colorScheme.primary, SurfaceSoft)
        AssistStatCard("Verified", stats.verifiedCount.toString(), Modifier.weight(1f), Success, SuccessSoft)
        AssistStatCard("Unverified", stats.unverifiedCount.toString(), Modifier.weight(1f), TextSecondary, MaterialTheme.colorScheme.surface)
    }
}

@Composable
private fun AssistStatCard(
    label: String,
    value: String,
    modifier: Modifier,
    accent: androidx.compose.ui.graphics.Color,
    background: androidx.compose.ui.graphics.Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = background,
        border = BorderStroke(1.dp, Divider.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = accent)
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1)
        }
    }
}

@Composable
private fun AdvisorDirectoryCard(
    advisor: AdvisorSummaryData,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    selected: Boolean = advisor.isSelected,
    selectionEnabled: Boolean = false,
    onToggleSelection: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isVerified = advisor.score > 0 || advisor.reasons.any { it.contains("verified", ignoreCase = true) }
    val location = listOf(advisor.locality, advisor.city, advisor.state).filter { it.isNotBlank() }.joinToString(", ")
        .ifBlank { "Location shared after contact" }
    PremiumCard(
        modifier = modifier,
        containerColor = if (isHighlighted) SurfaceWarm else MaterialTheme.colorScheme.surface
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MemberPhoto(
                    photoUrl = null,
                    contentDescription = advisor.fullName,
                    modifier = Modifier.size(62.dp),
                    shape = RoundedCornerShape(18.dp)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            advisor.fullName.ifBlank { "SoulMatch Agent" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isVerified) {
                            Icon(Icons.Filled.Verified, contentDescription = "Verified agent", tint = Success, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(
                        advisor.serviceLabel.ifBlank { "Registered agent" },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        location,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            if (advisor.bio.isNotBlank()) {
                Text(advisor.bio, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AgentMetric("Success", "${advisor.successRate.toInt()}%", modifier = Modifier.weight(1f))
                AgentMetric("Rating", String.format("%.1f/5", advisor.averageRating), modifier = Modifier.weight(1f))
                AgentMetric("Cases", advisor.activeAssignments.toString(), modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectionEnabled && onToggleSelection != null) {
                    Button(
                        onClick = onToggleSelection,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (selected) "Selected" else "Select Agent")
                    }
                }
                OutlinedButton(
                    onClick = {
                        if (advisor.phone.isNotBlank()) {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${advisor.phone}")))
                        }
                    },
                    modifier = Modifier.size(44.dp),
                    contentPadding = PaddingValues(0.dp),
                    enabled = advisor.phone.isNotBlank()
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                OutlinedButton(
                    onClick = {
                        if (advisor.email.isNotBlank()) {
                            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${advisor.email}")))
                        }
                    },
                    modifier = Modifier.size(44.dp),
                    contentPadding = PaddingValues(0.dp),
                    enabled = advisor.email.isNotBlank()
                ) {
                    Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun AgentMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = SurfaceSoft,
        border = BorderStroke(1.dp, Divider.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotlightScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onUpgrade: () -> Unit,
    vm: MyProfileViewModel = hiltViewModel()
) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    var selectedDays by remember { mutableStateOf(7) }
    val isActive = !profile?.profileStatus.equals("inactive", ignoreCase = true)
    val projectedLift = when (selectedDays) {
        3 -> "2x more visibility during active browsing windows"
        15 -> "consistent top-of-stack placement for family-led browsing"
        else -> "stronger reach across search, best matches, and activity views"
    }
    val features = listOf(
        FeaturePoint("Top placement", "Push your profile higher in search and best-match feeds."),
        FeaturePoint("More attention", "Increase profile opens, shortlist actions, and interest chances."),
        FeaturePoint("Intent-led boost", "Works best when your profile is active, complete, and photo-ready.")
    )

    DrawerDestinationScaffold(
        title = "Spotlight",
        onBack = onBack
    ) { padding ->
        PremiumScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    PremiumHeader(
                        eyebrow = "Visibility boost",
                        title = "Get your profile seen first",
                        subtitle = "Spotlight is the fastest way to increase discovery when you want more high-intent families to notice your profile."
                    )
                }
                if (!isActive) {
                    item {
                        StatusInfoCard(
                            title = "Profile is currently inactive",
                            detail = "Turn your profile active first. Spotlight works only when your profile is visible in search and match feeds.",
                            toneColor = ErrorSoft,
                            contentColor = Error,
                            actionLabel = "Open visibility settings",
                            onAction = onOpenSettings
                        )
                    }
                }
                item {
                    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = SurfaceWarm) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SectionTitle("Choose spotlight duration", "Pick the push window that matches how aggressively you want to be discovered.")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(3, 7, 15).forEach { days ->
                                    FilterChoiceChip(
                                        label = "$days days",
                                        selected = selectedDays == days,
                                        onClick = { selectedDays = days }
                                    )
                                }
                            }
                            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, Divider)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Expected reach", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(projectedLift, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            }
                            Button(onClick = onUpgrade, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("View spotlight plans")
                            }
                        }
                    }
                }
                item {
                    TwoMetricRow(
                        leftLabel = "Profile status",
                        leftValue = if (isActive) "Active" else "Inactive",
                        rightLabel = "Readiness",
                        rightValue = "${profile?.let(ProfileStrengthAdvisor::score) ?: 0}%"
                    )
                }
                item {
                    FeatureListCard(
                        title = "How spotlight helps",
                        subtitle = "Use Spotlight when your profile photos, checklist, and partner preferences are already strong.",
                        features = features
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AstrologyServicesScreen(
    onBack: () -> Unit,
    onCompleteHoroscope: () -> Unit,
    onUpgrade: () -> Unit,
    vm: MyProfileViewModel = hiltViewModel()
) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val astroSignals = listOf(
        "Rashi" to profile?.rashi.orEmpty(),
        "Nakshatra" to profile?.nakshatra.orEmpty(),
        "Birth city" to profile?.birthCity.orEmpty(),
        "Gotra" to profile?.gotra.orEmpty()
    )
    val filledSignals = astroSignals.count { it.second.isNotBlank() } + if (profile?.isManglik == true) 1 else 0
    val readinessPercent = (filledSignals * 100 / 5).coerceIn(0, 100)
    val missing = astroSignals.filter { it.second.isBlank() }.map { it.first } +
        if (profile?.isManglik == false) listOf("Manglik preference") else emptyList()

    DrawerDestinationScaffold(
        title = "Astrology Services",
        onBack = onBack
    ) { padding ->
        PremiumScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    PremiumHeader(
                        eyebrow = "Compatibility layer",
                        title = "Astrology-ready matching",
                        subtitle = "Complete horoscope data unlocks better family confidence, clearer compatibility signals, and premium astrology support."
                    )
                }
                item {
                    TwoMetricRow(
                        leftLabel = "Astro readiness",
                        leftValue = "$readinessPercent%",
                        rightLabel = "Signals filled",
                        rightValue = "$filledSignals/5"
                    )
                }
                item {
                    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = SurfaceWarm) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SectionTitle("Current horoscope details", "Keep this complete before families ask for astro alignment.")
                            astroSignals.forEach { (label, value) ->
                                DetailRow(
                                    label = label,
                                    value = value.ifBlank { "Not added yet" },
                                    highlighted = value.isNotBlank()
                                )
                            }
                            DetailRow(
                                label = "Manglik",
                                value = if (profile?.isManglik == true) "Yes" else "Not confirmed",
                                highlighted = profile?.isManglik == true
                            )
                        }
                    }
                }
                if (missing.isNotEmpty()) {
                    item {
                        StatusInfoCard(
                            title = "A few astro details are still missing",
                            detail = "Next best update: ${missing.take(3).joinToString(", ")}.",
                            toneColor = InfoSoft,
                            contentColor = Info,
                            actionLabel = "Complete horoscope step",
                            onAction = onCompleteHoroscope
                        )
                    }
                }
                item {
                    FeatureListCard(
                        title = "What Astrology Services includes",
                        subtitle = "Designed for families who want additional reassurance before moving ahead.",
                        features = listOf(
                            FeaturePoint("Horoscope review", "Check if the profile has enough data for basic kundli-based review."),
                            FeaturePoint("Compatibility support", "Use premium astro-compatible plan flows when families ask for guidance."),
                            FeaturePoint("Better ranking context", "Astrology-ready profiles are easier to trust during shortlisting.")
                        )
                    )
                }
                item {
                    Button(
                        onClick = onUpgrade,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("View astrology plans")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyCenterScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenVerification: () -> Unit,
    onOpenHelp: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
    profileVm: MyProfileViewModel = hiltViewModel()
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val hiddenMembers by vm.hiddenMembers.collectAsStateWithLifecycle()
    val blockedMembers by vm.blockedMembers.collectAsStateWithLifecycle()
    val reportedConcerns by vm.reportedConcerns.collectAsStateWithLifecycle()
    val profile by profileVm.profile.collectAsStateWithLifecycle()
    val verificationLabel = when {
        profile?.verificationStatus.equals("verified", ignoreCase = true) -> "Verified"
        profile?.verificationStatus.equals("rejected", ignoreCase = true) -> "Needs update"
        else -> "Pending setup"
    }

    DrawerDestinationScaffold(
        title = "Safety Center",
        onBack = onBack
    ) { padding ->
        PremiumScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    PremiumHeader(
                        eyebrow = "Trust controls",
                        title = "Stay protected while matching",
                        subtitle = "Use verification, privacy controls, and reporting tools together so families can move with confidence."
                    )
                }
                item {
                    TwoMetricRow(
                        leftLabel = "Verification",
                        leftValue = verificationLabel,
                        rightLabel = "Profile visibility",
                        rightValue = if (settings.profileActive) "Active" else "Inactive"
                    )
                }
                item {
                    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionTitle("Quick actions", "Open the most important safety controls from one place.")
                            DrawerActionButton(
                                icon = Icons.Filled.Verified,
                                title = "Request or review verification",
                                subtitle = "Manage your verified profile badge and trust status.",
                                onClick = onOpenVerification
                            )
                            DrawerActionButton(
                                icon = Icons.Filled.Lock,
                                title = "Open privacy settings",
                                subtitle = "Control photo privacy, search visibility, and contact filters.",
                                onClick = onOpenSettings
                            )
                            DrawerActionButton(
                                icon = Icons.Filled.Help,
                                title = "Contact help & support",
                                subtitle = "Reach SoulMatch support for safety or account concerns.",
                                onClick = onOpenHelp
                            )
                        }
                    }
                }
                item {
                    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = SurfaceWarm) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SectionTitle("Safety overview", "A quick snapshot of how your account is protected today.")
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                MetricPill("Hidden", hiddenMembers.size.toString(), modifier = Modifier.weight(1f))
                                MetricPill("Blocked", blockedMembers.size.toString(), modifier = Modifier.weight(1f))
                                MetricPill("Reports", reportedConcerns.size.toString(), modifier = Modifier.weight(1f))
                            }
                            SignalChips(
                                labels = listOf(
                                    if (settings.pushEnabled) "Alerts enabled" else "Alerts paused",
                                    if (settings.photoPrivacyEnabled) "Photos protected" else "Photos public",
                                    if (settings.contactFilterEnabled) "Contact filter on" else "Open contact flow"
                                ),
                                tone = ChipTone.Info
                            )
                        }
                    }
                }
                if (reportedConcerns.isNotEmpty()) {
                    item {
                        PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = SurfaceSoft) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionTitle("Recent concerns", "These are the latest member safety notes you saved.")
                                reportedConcerns.take(3).forEach { concern ->
                                    DetailRow(
                                        label = concern.name,
                                        value = concern.concern,
                                        highlighted = true
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    FeatureListCard(
                        title = "Good safety habits",
                        subtitle = "These are the habits SoulMatch members should use before sharing personal information.",
                        features = listOf(
                            FeaturePoint("Verify before sharing", "Use the verified badge and complete photo gallery before giving personal contact details."),
                            FeaturePoint("Move gradually", "Keep early conversations in-app until both sides are comfortable."),
                            FeaturePoint("Report quickly", "If anything feels suspicious, report and block immediately.")
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(
    onBack: () -> Unit,
    onOpenSafetyCenter: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf<String?>(null) }
    val faqs = listOf(
        FaqItem("How do I request verification?", "Open My Profile and use the Profile verification card. Add at least one photo and complete the profile to improve approval chances."),
        FaqItem("Why is a member not replying?", "Some members wait for family review before responding. Better photos, a complete profile, and verified status usually improve response rate."),
        FaqItem("How do I report abuse or fraud?", "Use Safety Center for reporting guidance, then block or report the profile from the member screen.")
    )

    DrawerDestinationScaffold(
        title = "Help & Support",
        onBack = onBack
    ) { padding ->
        PremiumScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    PremiumHeader(
                        eyebrow = "Support desk",
                        title = "Get help quickly",
                        subtitle = "Reach SoulMatch for account, payment, verification, privacy, or safety help."
                    )
                }
                if (!status.isNullOrBlank()) {
                    item {
                        StatusInfoCard(
                            title = "Action update",
                            detail = status ?: "",
                            toneColor = SuccessSoft,
                            contentColor = Success
                        )
                    }
                }
                item {
                    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionTitle("Contact SoulMatch", "Use email for detailed help. Call is useful for urgent support follow-up.")
                            DrawerActionButton(
                                icon = Icons.Filled.Email,
                                title = SupportEmail,
                                subtitle = "Email support for account, payment, or verification help.",
                                onClick = {
                                    status = context.launchExternal(
                                        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SupportEmail?subject=${Uri.encode("SoulMatch support request")}"))
                                    )
                                }
                            )
                            DrawerActionButton(
                                icon = Icons.Filled.Call,
                                title = SupportPhone,
                                subtitle = "Call support if a live follow-up has already been arranged.",
                                onClick = {
                                    status = context.launchExternal(
                                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:$SupportPhone"))
                                    )
                                }
                            )
                        }
                    }
                }
                item {
                    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = SurfaceWarm) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionTitle("Useful actions", "Go straight to the area that solves the issue faster.")
                            DrawerActionButton(
                                icon = Icons.Filled.Lock,
                                title = "Safety Center",
                                subtitle = "Report suspicious behavior, review safety tips, and open privacy controls.",
                                onClick = onOpenSafetyCenter
                            )
                            DrawerActionButton(
                                icon = Icons.Filled.Verified,
                                title = "Privacy Policy",
                                subtitle = "Understand how SoulMatch stores and uses your account data.",
                                onClick = onOpenPrivacy
                            )
                            DrawerActionButton(
                                icon = Icons.Filled.CheckCircle,
                                title = "Terms of Service",
                                subtitle = "Review account, plan, payment, and platform responsibilities.",
                                onClick = onOpenTerms
                            )
                        }
                    }
                }
                item {
                    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = SurfaceSoft) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionTitle("Frequently asked questions")
                            faqs.forEach { faq ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Divider),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(faq.question, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(faq.answer, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawerDestinationScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = content
    )
}

@Composable
private fun FeatureListCard(
    title: String,
    subtitle: String,
    features: List<FeaturePoint>
) {
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = MaterialTheme.colorScheme.surface) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(title, subtitle)
            features.forEach { feature ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceSoft,
                    border = BorderStroke(1.dp, Divider)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(feature.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(feature.detail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusInfoCard(
    title: String,
    detail: String,
    toneColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = toneColor) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = contentColor)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = contentColor)
            if (!actionLabel.isNullOrBlank() && onAction != null) {
                OutlinedButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun TwoMetricRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricPill(label = leftLabel, value = leftValue, modifier = Modifier.weight(1f))
        MetricPill(label = rightLabel, value = rightValue, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    highlighted: Boolean
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (highlighted) SuccessSoft else SurfaceSoft,
        border = BorderStroke(1.dp, Divider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (highlighted) Success else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DrawerActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Divider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceSoft,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

private fun android.content.Context.launchExternal(intent: Intent): String {
    return runCatching {
        startActivity(intent)
        "Opening selected support action."
    }.getOrElse {
        "No supported app was found on this device for that action."
    }
}

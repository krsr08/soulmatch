package com.soulmatch.app.ui.screens.agent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Groups2
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.data.models.AgentManagedProfileSummaryData
import com.soulmatch.app.ui.viewmodels.AgentViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AgentDashboardScreen(
    onOpenOnboarding: () -> Unit,
    onOpenProfiles: (String) -> Unit,
    onOpenPlans: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenCreateProfile: () -> Unit,
    onOpenActivities: () -> Unit,
    onDrawerDestination: (AgentDrawerDestination) -> Unit,
    vm: AgentViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val pendingProfiles = state.managedProfiles.filter { !it.isVerifiedForAgent() }
    val managedProfiles = state.managedProfiles.filter { it.isVerifiedForAgent() }
    val recentActivities = state.managedProfiles
        .sortedByDescending { it.updatedAt ?: it.createdAt ?: "" }
        .take(5)

    val verificationProgress = when (state.agentProfile?.onboardingStatus) {
        "approved" -> 1f
        "under_review" -> 0.7f
        "rejected" -> 0.45f
        else -> 0.3f
    }

    val feePrefs = state.agentProfile?.feePreferences.orEmpty()
    val commissionEnabled = feePrefs["enabled"].equals("true", ignoreCase = true)
    val verifiedRate = feePrefs["verifiedProfileRateInr"]?.toIntOrNull() ?: 0
    val successfulMatchRate = feePrefs["successfulMatchRateInr"]?.toIntOrNull() ?: 0
    val monthlyTarget = feePrefs["monthlyTargetInr"]?.toIntOrNull() ?: 0
    val monthlyCommission = (managedProfiles.size * verifiedRate) + (managedProfiles.count { it.matchCount > 0 } * successfulMatchRate)
    val monthlyProgress = if (monthlyTarget > 0) (monthlyCommission.toFloat() / monthlyTarget.toFloat()).coerceIn(0f, 1f) else 0f

    AgentScaffold(
        title = "SoulMatch",
        selectedTab = AgentTab.Dashboard,
        onOpenDashboard = {},
        onOpenProfiles = { onOpenProfiles("all") },
        onOpenPlans = onOpenPlans,
        onOpenAccount = onOpenAccount,
        onDrawerDestination = onDrawerDestination
    ) { modifier ->
        Box(modifier = modifier.fillMaxSize()) {
            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AgentColorsAccent)
                return@AgentScaffold
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Agent Dashboard",
                            fontFamily = FontFamily.Serif,
                            fontSize = 34.sp,
                            lineHeight = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF24181B)
                        )
                        VerificationProgressCard(
                            status = state.agentProfile?.onboardingStatus ?: "pending",
                            progress = verificationProgress,
                            rejectionReason = state.agentProfile?.onboardingRejectionReason.orEmpty(),
                            onOpenOnboarding = onOpenOnboarding
                        )
                        Button(
                            onClick = if (state.agentProfile?.isOnboarded == true) onOpenCreateProfile else onOpenOnboarding,
                            modifier = Modifier.height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AgentColorsAccent, contentColor = Color.White),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            androidx.compose.material3.Icon(Icons.Outlined.Add, contentDescription = null)
                            Text("  Add New Member", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item {
                    DashboardSectionCard(
                        title = "Pending Verifications",
                        value = pendingProfiles.size.toString(),
                        detail = if (pendingProfiles.isEmpty()) "No profiles waiting right now" else "Drafts and submitted profiles waiting for review",
                        icon = Icons.Outlined.AccessTime,
                        accent = Color(0xFF9D5B00),
                        badge = if (pendingProfiles.isEmpty()) null else "${pendingProfiles.size} waiting",
                        onClick = { onOpenProfiles("pending") }
                    )
                }
                item {
                    DashboardSectionCard(
                        title = "Managed Profiles",
                        value = managedProfiles.size.toString(),
                        detail = if (managedProfiles.isEmpty()) "Verified profiles will appear here" else "Only verified profiles are counted as actively managed",
                        icon = Icons.Outlined.Groups2,
                        accent = AgentColorsAccent,
                        badge = if (managedProfiles.isEmpty()) null else "${managedProfiles.count { (it.viewCount + it.matchCount) > 0 }} active",
                        onClick = { onOpenProfiles("managed") }
                    )
                }
                item {
                    DashboardStatStrip(
                        verificationRate = if (state.managedProfiles.isEmpty()) 0 else ((managedProfiles.size.toFloat() / state.managedProfiles.size.toFloat()) * 100).roundToInt(),
                        matchMomentum = managedProfiles.sumOf { it.matchCount }
                    )
                }
                item {
                    RecentActivityCard(
                        profiles = recentActivities,
                        onViewAll = onOpenActivities
                    )
                }
                if (commissionEnabled) {
                    item {
                        MonthlyCommissionCard(
                            amountInr = monthlyCommission,
                            progress = monthlyProgress,
                            targetLabel = if (monthlyTarget > 0) "${(monthlyProgress * 100).roundToInt()}% reached monthly target" else "Monthly target not configured"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerificationProgressCard(
    status: String,
    progress: Float,
    rejectionReason: String,
    onOpenOnboarding: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = AgentShapesCard,
        border = BorderStroke(1.dp, Color(0xFFF0E2DE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Verification Progress", color = Color(0xFF2F2326), fontWeight = FontWeight.SemiBold)
                    Text(
                        when (status) {
                            "approved" -> "Your agent account is approved."
                            "under_review" -> "Your onboarding is under admin review."
                            "rejected" -> "Your details need an update before approval."
                            else -> "Complete your details and KYC to unlock the full dashboard."
                        },
                        color = AgentColorsMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFFFFF1F4)
                ) {
                    Text(
                        status.replaceFirstChar { it.uppercase() },
                        color = AgentColorsAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = AgentColorsAccent,
                trackColor = Color(0xFFF2E7E5)
            )
            if (status == "rejected" && rejectionReason.isNotBlank()) {
                Text(rejectionReason, color = Color(0xFF8F3C50), style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "Update details",
                color = AgentColorsAccent,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onOpenOnboarding)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DashboardSectionCard(
    title: String,
    value: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    badge: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = AgentShapesCard,
        border = BorderStroke(1.dp, Color(0xFFF0E2DE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(accent.copy(alpha = 0.10f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(icon, contentDescription = null, tint = accent)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    badge?.let {
                        Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFFFF3F5)) {
                            Text(
                                it,
                                color = AgentColorsAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    androidx.compose.material3.Icon(Icons.Outlined.ArrowOutward, contentDescription = null, tint = Color(0xFF6C5960))
                }
            }
            Text(title, color = Color(0xFF2A1D20), fontWeight = FontWeight.SemiBold)
            Text(value, fontFamily = FontFamily.Serif, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color(0xFF161214))
            Text(detail, color = AgentColorsMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DashboardStatStrip(verificationRate: Int, matchMomentum: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MiniStatCard(
            modifier = Modifier.weight(1f),
            title = "Verification Rate",
            value = "$verificationRate%",
            icon = Icons.Outlined.AssignmentTurnedIn
        )
        MiniStatCard(
            modifier = Modifier.weight(1f),
            title = "Match Momentum",
            value = matchMomentum.toString(),
            icon = Icons.Outlined.Insights
        )
    }
}

@Composable
private fun MiniStatCard(
    modifier: Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFA)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFF0E2DE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            androidx.compose.material3.Icon(icon, contentDescription = null, tint = AgentColorsAccent)
            Text(title, color = AgentColorsMuted, style = MaterialTheme.typography.bodySmall)
            Text(value, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }
    }
}

@Composable
private fun RecentActivityCard(
    profiles: List<AgentManagedProfileSummaryData>,
    onViewAll: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = AgentShapesCard,
        border = BorderStroke(1.dp, Color(0xFFF0E2DE))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Activity", fontFamily = FontFamily.Serif, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text("View all", color = AgentColorsAccent, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onViewAll))
            }
            if (profiles.isEmpty()) {
                Text(
                    "No profile activity yet. Once profiles are added, review updates will appear here.",
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                    color = AgentColorsMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                profiles.forEach { profile ->
                    ActivityRow(profile = profile)
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(profile: AgentManagedProfileSummaryData) {
    val label = profileActivityLabel(profile)
    val timeline = profileTimelineLabel(profile.updatedAt ?: profile.createdAt)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color(0xFFF8EFED), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(Icons.Outlined.PersonOutline, contentDescription = null, tint = AgentColorsAccent)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(profile.fullDisplayName(), fontWeight = FontWeight.SemiBold, color = Color(0xFF2B1D1F))
                Text(timeline, color = AgentColorsMuted, style = MaterialTheme.typography.bodySmall)
            }
            Text(label.first, color = Color(0xFF544446), style = MaterialTheme.typography.bodyMedium)
            Surface(shape = RoundedCornerShape(999.dp), color = label.second.copy(alpha = 0.14f)) {
                Text(
                    label.third,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = label.second,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun MonthlyCommissionCard(
    amountInr: Int,
    progress: Float,
    targetLabel: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = AgentShapesCard,
        border = BorderStroke(1.dp, Color(0xFFF0E2DE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Monthly Commission", fontFamily = FontFamily.Serif, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "INR ${"%,d".format(amountInr)}",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                color = AgentColorsAccent
            )
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Color(0xFFF5C03A),
                trackColor = Color(0xFFE7E1DE)
            )
            Text(targetLabel, color = AgentColorsMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

internal fun AgentManagedProfileSummaryData.isVerifiedForAgent(): Boolean {
    return reviewStatus == "verified" || verificationStatus == "verified"
}

internal fun AgentManagedProfileSummaryData.fullDisplayName(): String {
    return listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Client Profile" }
}

internal fun profileActivityLabel(profile: AgentManagedProfileSummaryData): Triple<String, Color, String> {
    return when {
        profile.isVerifiedForAgent() -> Triple("Profile verified and moved into active management.", Color(0xFF0F8D57), "Verified")
        profile.reviewStatus == "submitted" || profile.reviewStatus == "under_review" -> Triple("Profile is waiting for verification review.", Color(0xFF9D5B00), "Under review")
        profile.reviewStatus == "rejected" -> Triple("Profile needs edits before it can be verified.", Color(0xFFB03B57), "Needs update")
        else -> Triple("Draft profile saved by agent.", AgentColorsAccent, "Draft")
    }
}

internal fun profileTimelineLabel(rawIso: String?): String {
    val instant = runCatching { rawIso?.let(Instant::parse) }.getOrNull() ?: return "Recently"
    val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    return when (localDate) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> "${localDate.dayOfMonth}/${localDate.monthValue}/${localDate.year}"
    }
}

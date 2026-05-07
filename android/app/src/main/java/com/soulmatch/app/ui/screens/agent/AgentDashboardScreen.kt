package com.soulmatch.app.ui.screens.agent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.StarOutline
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
    val agentProfile = state.agentProfile
    val pendingProfiles = state.managedProfiles.filter { !it.isVerifiedForAgent() }
    val managedProfiles = state.managedProfiles.filter { it.isVerifiedForAgent() }
    val recentActivities = state.managedProfiles
        .sortedByDescending { it.updatedAt ?: it.createdAt ?: "" }
        .take(3)

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
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        AgentHeroCard(
                            fullName = agentProfile?.fullName.orEmpty(),
                            agentCode = agentProfile?.agentCode.orEmpty(),
                            businessName = agentProfile?.businessName.orEmpty(),
                            city = agentProfile?.city.orEmpty(),
                            stateName = agentProfile?.state.orEmpty(),
                            status = agentProfile?.onboardingStatus ?: "pending",
                            progress = verificationProgress,
                            onOpenOnboarding = onOpenOnboarding
                        )
                        Button(
                            onClick = if (agentProfile?.isOnboarded == true) onOpenCreateProfile else onOpenOnboarding,
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
                    DashboardMetricCard(
                        title = "Managed Profiles",
                        value = managedProfiles.size.toString(),
                        detail = if (managedProfiles.isEmpty()) "No verified profiles yet" else "${managedProfiles.size} verified",
                        icon = Icons.Outlined.Groups2,
                        accent = Color(0xFF24181B),
                        onClick = { onOpenProfiles("managed") }
                    )
                }
                item {
                    DashboardMetricCard(
                        title = "Pending Verifications",
                        value = pendingProfiles.size.toString(),
                        detail = if (pendingProfiles.isEmpty()) "0 pending" else "${pendingProfiles.size} pending",
                        icon = Icons.Outlined.AccessTime,
                        accent = Color(0xFF24181B),
                        onClick = { onOpenProfiles("pending") }
                    )
                }
                item {
                    EngagementCard(
                        verificationRate = if (state.managedProfiles.isEmpty()) 0 else ((managedProfiles.size.toFloat() / state.managedProfiles.size.toFloat()) * 100).roundToInt()
                    )
                }
                item {
                    DecorativeDashboardDivider()
                }
                item {
                    RecentActivityCard(
                        title = "Client Activity Overview",
                        profiles = recentActivities,
                        onViewAll = onOpenActivities
                    )
                }
                item {
                    if (commissionEnabled) {
                        MonthlyCommissionCard(
                            amountInr = monthlyCommission,
                            progress = monthlyProgress,
                            targetLabel = if (monthlyTarget > 0) "${(monthlyProgress * 100).roundToInt()}% reached monthly target" else "Monthly target not configured"
                        )
                    } else {
                        QuickLinksCard(
                            onReviewPending = { onOpenProfiles("pending") },
                            onUpdateProfile = onOpenOnboarding
                        )
                    }
                }
                if (commissionEnabled) {
                    item {
                        QuickLinksCard(
                            onReviewPending = { onOpenProfiles("pending") },
                            onUpdateProfile = onOpenOnboarding
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentHeroCard(
    fullName: String,
    agentCode: String,
    businessName: String,
    city: String,
    stateName: String,
    status: String,
    progress: Float,
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = Color(0xFFF8E4EA),
                    border = BorderStroke(2.dp, AgentColorsAccent)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            fullName.take(1).ifBlank { "A" },
                            style = MaterialTheme.typography.headlineSmall,
                            color = AgentColorsAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        fullName.ifBlank { "SoulMatch Agent" },
                        fontFamily = FontFamily.Serif,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2B1D1F)
                    )
                    Text(
                        "ID: ${agentCode.ifBlank { "AGT-0000" }}",
                        color = Color(0xFF3E2C31),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Business: ${businessName.ifBlank { "SoulMatch Partner" }}",
                        color = Color(0xFF3E2C31),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = AgentColorsMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            listOf(city, stateName).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "Location pending" },
                            color = AgentColorsMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFF8E7)
                ) {
                    Box(
                        modifier = Modifier.size(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Outlined.StarOutline,
                            contentDescription = null,
                            tint = Color(0xFFE0B628),
                            modifier = Modifier.size(16.dp)
                        )
                    }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    when (status) {
                        "approved" -> "Verification complete"
                        "under_review" -> "Verification under review"
                        "rejected" -> "Verification needs update"
                        else -> "Verification in progress"
                    },
                    color = AgentColorsMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Update Profile",
                    color = AgentColorsAccent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onOpenOnboarding)
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DashboardMetricCard(
    title: String,
    value: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color(0xFF2A1D20), fontWeight = FontWeight.Medium)
                androidx.compose.material3.Icon(icon, contentDescription = null, tint = accent)
            }
            Text(value, fontFamily = FontFamily.Serif, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color(0xFF161214))
            Text(detail, color = Color(0xFFD7A12F), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EngagementCard(verificationRate: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AgentColorsAccent),
        shape = AgentShapesCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Engagement Rate", color = Color.White, fontWeight = FontWeight.SemiBold)
                androidx.compose.material3.Icon(Icons.Outlined.Insights, contentDescription = null, tint = Color.White)
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$verificationRate%", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = Color.White)
                Text("Top 10% of agents", color = Color.White.copy(alpha = 0.84f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun RecentActivityCard(
    title: String,
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
                Text(title, fontFamily = FontFamily.Serif, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
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
                "₹${"%,d".format(amountInr)}",
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

@Composable
private fun DecorativeDashboardDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(70.dp).height(1.dp).background(Color(0xFFE2D198)))
        Text(
            "⚭",
            modifier = Modifier.padding(horizontal = 12.dp),
            color = Color(0xFFE0B628),
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.width(70.dp).height(1.dp).background(Color(0xFFE2D198)))
    }
}

@Composable
private fun QuickLinksCard(
    onReviewPending: () -> Unit,
    onUpdateProfile: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFBF5F2)),
        shape = AgentShapesCard,
        border = BorderStroke(1.dp, Color(0xFFF0E2DE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Quick Links", fontWeight = FontWeight.SemiBold, color = Color(0xFF2A1D20))
            QuickLinkRow("Review Pending", onClick = onReviewPending)
            QuickLinkRow("Update Profile", onClick = onUpdateProfile)
        }
    }
}

@Composable
private fun QuickLinkRow(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE7D9D4))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color(0xFF4F232C), fontWeight = FontWeight.Medium)
            androidx.compose.material3.Icon(Icons.Outlined.ArrowOutward, contentDescription = null, tint = Color(0xFF4F232C))
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

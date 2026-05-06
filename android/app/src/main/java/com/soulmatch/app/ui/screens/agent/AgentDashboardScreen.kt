package com.soulmatch.app.ui.screens.agent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Groups2
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.InsertChart
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Reviews
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import kotlin.math.roundToInt

@Composable
fun AgentDashboardScreen(
    onOpenOnboarding: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenCreateProfile: () -> Unit,
    vm: AgentViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val profiles = state.managedProfiles
    val managedCount = profiles.size
    val pendingCount = profiles.count { it.reviewStatus == "submitted" || it.reviewStatus == "under_review" || it.verificationStatus == "pending" }
    val verifiedCount = profiles.count { it.reviewStatus == "verified" || it.verificationStatus == "verified" }
    val engagementRate = if (managedCount == 0) 0 else ((verifiedCount.toFloat() / managedCount.toFloat()) * 100f).roundToInt()
    val estimatedCommission = (verifiedCount * 100) + (pendingCount * 50)
    val commissionProgress = if (estimatedCommission <= 0) 0f else (estimatedCommission / 6000f).coerceIn(0f, 1f)

    AgentScaffold(
        title = "SoulMatch",
        selectedTab = AgentTab.Dashboard,
        onOpenDashboard = {},
        onOpenProfiles = onOpenProfiles,
        onOpenPlans = onOpenPlans,
        onOpenAccount = onOpenAccount
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Agent Dashboard", fontFamily = FontFamily.Serif, fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("✧", color = Color(0xFFF0B72C))
                            Text(
                                if (state.agentProfile?.membershipPlan?.isNotBlank() == true) {
                                    "Verified ${state.agentProfile?.membershipPlan?.replaceFirstChar { it.uppercase() }} Agent"
                                } else {
                                    "Verified Premium Agent"
                                },
                                color = AgentColorsAccent,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                item {
                    Button(
                        onClick = if (state.agentProfile?.isOnboarded == true) onOpenCreateProfile else onOpenOnboarding,
                        modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AgentColorsAccent, contentColor = Color.White),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Text("  Add New Profile", fontWeight = FontWeight.Bold)
                    }
                }
                item {
                    DashboardMetricCard(
                        title = "Managed Profiles",
                        value = managedCount.toString(),
                        detail = if (managedCount == 0) "Start by adding your first client" else "+${profiles.count { it.viewCount > 0 }} active this week",
                        icon = Icons.Outlined.Groups2,
                        accent = Color(0xFF7A142A)
                    )
                }
                item {
                    DashboardMetricCard(
                        title = "Pending Verifications",
                        value = pendingCount.toString(),
                        detail = if (pendingCount == 0) "All caught up" else "Action required",
                        icon = Icons.Outlined.HourglassBottom,
                        accent = Color(0xFF8A6B00)
                    )
                }
                item {
                    DashboardMetricCard(
                        title = "Engagement Rate",
                        value = "$engagementRate%",
                        detail = if (engagementRate >= 75) "Top 10% of agents" else "Keep client profiles moving",
                        icon = Icons.Outlined.Insights,
                        accent = AgentColorsAccent,
                        filled = true
                    )
                }
                item {
                    DividerSection()
                }
                item {
                    ClientActivityOverview(profiles = profiles)
                }
                item {
                    MonthlyCommissionCard(
                        amountUsd = estimatedCommission,
                        progress = commissionProgress
                    )
                }
                item {
                    QuickLinksCard(
                        onReviewPending = if (state.agentProfile?.isOnboarded == true) onOpenProfiles else onOpenOnboarding,
                        onBroadcast = onOpenAccount
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardMetricCard(
    title: String,
    value: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    filled: Boolean = false
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (filled) AgentColorsAccent else Color.White),
        shape = AgentShapesCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = if (filled) Color.White else Color(0xFF352B2C))
                Icon(icon, contentDescription = null, tint = if (filled) Color.White else accent)
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(value, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = if (filled) Color.White else Color(0xFF161314))
                Text(detail, color = if (filled) Color.White.copy(alpha = 0.84f) else Color(0xFF7F6A69))
            }
        }
    }
}

@Composable
private fun DividerSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Divider(modifier = Modifier.weight(1f), color = Color(0xFFF0D9A9))
        Text("⚭", color = Color(0xFFE5B321))
        Divider(modifier = Modifier.weight(1f), color = Color(0xFFF0D9A9))
    }
}

@Composable
private fun ClientActivityOverview(profiles: List<AgentManagedProfileSummaryData>) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = AgentShapesCard) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text("Client Activity\nOverview", fontFamily = FontFamily.Serif, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium)
                Text("View\nAll", color = AgentColorsAccent, textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
            if (profiles.isEmpty()) {
                Text(
                    "No client activity yet. Add a profile to start tracking submissions and milestones.",
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    color = AgentColorsMuted
                )
            } else {
                profiles.take(3).forEachIndexed { index, profile ->
                    if (index > 0) Divider(color = Color(0xFFF2E6E2))
                    ActivityRow(profile = profile)
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(profile: AgentManagedProfileSummaryData) {
    val statusLabel = when {
        profile.reviewStatus == "draft" -> "Profile Update"
        profile.reviewStatus == "submitted" || profile.reviewStatus == "under_review" -> "Action Needed"
        profile.matchCount > 0 -> "Match Milestone"
        else -> "Profile Update"
    }
    val statusColor = when (statusLabel) {
        "Action Needed" -> Color(0xFFFFE3DE)
        "Match Milestone" -> Color(0xFFFFF2CE)
        else -> Color(0xFFF9E6EC)
    }
    val detail = when {
        profile.reviewStatus == "draft" -> "Updated preferences and saved as draft."
        profile.reviewStatus == "submitted" || profile.reviewStatus == "under_review" -> "New profile draft submitted for review."
        profile.matchCount > 0 -> "Mutual interest confirmed. Matching activity has started."
        else -> "Profile progress moved forward this week."
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(Color(0xFFF7F1EE), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (profile.matchCount > 0) Icons.Outlined.InsertChart else Icons.Outlined.PersonOutline,
                contentDescription = null,
                tint = AgentColorsAccent
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    listOf(profile.firstName, profile.lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Client" },
                    fontWeight = FontWeight.Bold
                )
                Text(activityAgeLabel(profile), color = AgentColorsMuted, style = MaterialTheme.typography.bodySmall)
            }
            Text(detail, color = Color(0xFF554748), style = MaterialTheme.typography.bodyMedium)
            Box(
                modifier = Modifier
                    .background(statusColor, RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(statusLabel, color = AgentColorsAccent, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun MonthlyCommissionCard(amountUsd: Int, progress: Float) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = AgentShapesCard) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Monthly Commission", fontFamily = FontFamily.Serif, fontSize = 18.sp)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$${"%,d".format(amountUsd)}", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = AgentColorsAccent)
                Text("USD", color = AgentColorsMuted)
            }
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = Color(0xFFFFCF41),
                trackColor = Color(0xFFE4E0DE)
            )
            Text("${(progress * 100).roundToInt()}% to next tier bonus", color = AgentColorsMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun QuickLinksCard(onReviewPending: () -> Unit, onBroadcast: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF9)), shape = AgentShapesCard) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Quick Links", fontWeight = FontWeight.SemiBold)
            QuickLinkButton("Review Pending", Icons.Outlined.Reviews, onReviewPending)
            QuickLinkButton("Broadcast Message", Icons.Outlined.Campaign, onBroadcast)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun QuickLinkButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE9D9D4)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = AgentColorsAccent)
                Text(title, color = Color(0xFF4A2C35), fontWeight = FontWeight.Medium)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Color(0xFF4A2C35))
        }
    }
}

private fun activityAgeLabel(profile: AgentManagedProfileSummaryData): String {
    return when {
        profile.reviewStatus == "submitted" || profile.reviewStatus == "under_review" -> "2h ago"
        profile.matchCount > 0 -> "5h ago"
        profile.viewCount > 0 -> "1d ago"
        else -> "Recently"
    }
}

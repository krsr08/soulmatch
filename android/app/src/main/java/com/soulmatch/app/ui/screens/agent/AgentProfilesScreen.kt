package com.soulmatch.app.ui.screens.agent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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

@Composable
fun AgentProfilesScreen(
    filter: String = "all",
    onOpenDashboard: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenCreateProfile: () -> Unit,
    vm: AgentViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val pendingProfiles = state.managedProfiles.filter { !it.isVerifiedForAgent() }
    val managedProfiles = state.managedProfiles.filter { it.isVerifiedForAgent() }
    val selectedFilter = when (filter.lowercase()) {
        "pending", "managed" -> filter.lowercase()
        else -> "all"
    }
    val visibleProfiles = when (selectedFilter) {
        "pending" -> pendingProfiles
        "managed" -> managedProfiles
        else -> state.managedProfiles
    }

    AgentScaffold(
        title = "Profiles",
        selectedTab = AgentTab.Profiles,
        onOpenDashboard = onOpenDashboard,
        onOpenProfiles = {},
        onOpenPlans = onOpenPlans,
        onOpenAccount = onOpenAccount
    ) { modifier ->
        Column(modifier = modifier.fillMaxSize()) {
            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp), color = AgentColorsAccent)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                if (selectedFilter == "pending") "Pending Verifications" else if (selectedFilter == "managed") "Managed Profiles" else "All Client Profiles",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 30.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilterPill("All", selectedFilter == "all")
                                FilterPill("Pending", selectedFilter == "pending")
                                FilterPill("Managed", selectedFilter == "managed")
                            }
                            Text(
                                if (selectedFilter == "pending") "Draft, submitted, and under-review profiles stay here until verification is complete."
                                else if (selectedFilter == "managed") "Only verified profiles appear in active management."
                                else "Review every client profile from one place.",
                                color = AgentColorsMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    if (visibleProfiles.isEmpty()) {
                        item {
                            EmptyProfilesCard(selectedFilter)
                        }
                    } else {
                        items(visibleProfiles) { profile ->
                            AgentProfileSummaryCard(profile = profile)
                        }
                    }
                }
                ExtendedFloatingActionButton(
                    onClick = onOpenCreateProfile,
                    containerColor = AgentColorsAccent,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text("Add New Profile") },
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (selected) AgentColorsAccent else Color.White,
        border = BorderStroke(1.dp, if (selected) AgentColorsAccent else Color(0xFFEBDCD8))
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = if (selected) Color.White else Color(0xFF604A50),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun EmptyProfilesCard(selectedFilter: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = AgentShapesCard,
        border = BorderStroke(1.dp, Color(0xFFF0E2DE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                when (selectedFilter) {
                    "pending" -> "No pending profiles"
                    "managed" -> "No managed profiles yet"
                    else -> "No profiles yet"
                },
                fontWeight = FontWeight.SemiBold
            )
            Text(
                when (selectedFilter) {
                    "pending" -> "Newly added member profiles will appear here while verification is pending."
                    "managed" -> "Once a pending profile is verified, it will move into managed profiles."
                    else -> "Add your first profile to start the agent workflow."
                },
                color = AgentColorsMuted
            )
        }
    }
}

@Composable
private fun AgentProfileSummaryCard(profile: AgentManagedProfileSummaryData) {
    val verified = profile.isVerifiedForAgent()
    val statusColor = when {
        verified -> Color(0xFF0F8D57)
        profile.reviewStatus == "rejected" -> Color(0xFFB03B57)
        else -> Color(0xFF9D5B00)
    }
    val statusLabel = when {
        verified -> "Managed"
        profile.reviewStatus == "submitted" -> "Submitted"
        profile.reviewStatus == "under_review" -> "Under review"
        profile.reviewStatus == "rejected" -> "Rejected"
        else -> "Draft"
    }
    Card(
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFFF8EFED), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (verified) Icons.Outlined.AssignmentTurnedIn else Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = statusColor
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(profile.fullDisplayName(), fontWeight = FontWeight.SemiBold)
                        Text(
                            listOf(profile.occupation.takeIf { it.isNotBlank() }, profile.city.takeIf { it.isNotBlank() })
                                .filterNotNull()
                                .joinToString(" • ")
                                .ifBlank { "Member details captured by agent" },
                            color = AgentColorsMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Surface(shape = RoundedCornerShape(999.dp), color = statusColor.copy(alpha = 0.12f)) {
                    Text(
                        statusLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniInfoChip(icon = Icons.Outlined.Visibility, label = "${profile.viewCount} views")
                MiniInfoChip(icon = Icons.Outlined.AssignmentTurnedIn, label = "${profile.documentChecklistPercent}% docs")
                MiniInfoChip(icon = Icons.Outlined.Schedule, label = profileTimelineLabel(profile.updatedAt ?: profile.createdAt))
            }
            if (!verified && profile.rejectionReason.isNotBlank()) {
                Text(profile.rejectionReason, color = Color(0xFF98495E), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun MiniInfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFFFF6F7)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = AgentColorsAccent, modifier = Modifier.size(14.dp))
            Text(label, color = Color(0xFF664E54), style = MaterialTheme.typography.labelSmall)
        }
    }
}

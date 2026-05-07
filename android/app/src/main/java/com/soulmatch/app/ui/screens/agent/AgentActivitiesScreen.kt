package com.soulmatch.app.ui.screens.agent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
fun AgentActivitiesScreen(
    onBack: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenAccount: () -> Unit,
    vm: AgentViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val profiles = state.managedProfiles.sortedByDescending { it.updatedAt ?: it.createdAt ?: "" }

    AgentScaffold(
        title = "Activity",
        selectedTab = AgentTab.Dashboard,
        onOpenDashboard = onOpenDashboard,
        onOpenProfiles = onOpenProfiles,
        onOpenPlans = onOpenPlans,
        onOpenAccount = onOpenAccount
    ) { modifier ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("All Activities", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp)
                    Text("Today, yesterday, and earlier profile actions are grouped here for a quick review.", color = AgentColorsMuted)
                }
            }
            if (profiles.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = AgentShapesCard,
                        border = BorderStroke(1.dp, Color(0xFFF0E2DE))
                    ) {
                        Text(
                            "No activities yet.",
                            modifier = Modifier.padding(18.dp),
                            color = AgentColorsMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                activitySections(profiles).forEach { (label, itemsForDay) ->
                    item {
                        Text(label, color = AgentColorsAccent, fontWeight = FontWeight.SemiBold)
                    }
                    items(itemsForDay) { profile ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = AgentShapesCard,
                            border = BorderStroke(1.dp, Color(0xFFF0E2DE))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(profile.fullDisplayNameForAgent(), fontWeight = FontWeight.SemiBold)
                                Text(profileActivityLabel(profile).first, color = Color(0xFF554648), style = MaterialTheme.typography.bodyMedium)
                                Text(profileActivityLabel(profile).third, color = profileActivityLabel(profile).second, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun activitySections(profiles: List<AgentManagedProfileSummaryData>): List<Pair<String, List<AgentManagedProfileSummaryData>>> {
    return profiles.groupBy { profileTimelineLabel(it.updatedAt ?: it.createdAt) }
        .entries
        .map { it.key to it.value }
}

private fun AgentManagedProfileSummaryData.fullDisplayNameForAgent(): String {
    return listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Client Profile" }
}

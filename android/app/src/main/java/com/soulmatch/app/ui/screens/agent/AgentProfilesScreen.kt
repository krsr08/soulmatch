package com.soulmatch.app.ui.screens.agent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.ui.viewmodels.AgentViewModel

@Composable
fun AgentProfilesScreen(
    onOpenDashboard: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenCreateProfile: () -> Unit,
    vm: AgentViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
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
                        Text("Client Profiles", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    }
                    items(state.managedProfiles) { profile ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = AgentShapesCard
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("${profile.firstName} ${profile.lastName}".trim().ifBlank { "Client Profile" }, fontWeight = FontWeight.Bold)
                                Text("Step flow: Agent 4-step client form", color = AgentColorsMuted)
                                Text("Status: ${profile.reviewStatus.replace('_', ' ')}", color = AgentColorsAccent, fontWeight = FontWeight.SemiBold)
                                Text("Views ${profile.viewCount} • Matches ${profile.matchCount} • Docs ${profile.documentChecklistPercent}%")
                            }
                        }
                    }
                }
                ExtendedFloatingActionButton(
                    onClick = onOpenCreateProfile,
                    containerColor = AgentColorsAccent,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text("Create Profile") },
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

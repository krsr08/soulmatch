package com.soulmatch.app.ui.screens.agent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
@OptIn(ExperimentalMaterial3Api::class)
fun AgentAccountScreen(
    onOpenDashboard: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenOnboarding: () -> Unit,
    vm: AgentViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val profile = state.agentProfile
    AgentScaffold(
        title = "Account",
        selectedTab = AgentTab.Account,
        onOpenDashboard = onOpenDashboard,
        onOpenProfiles = onOpenProfiles,
        onOpenPlans = onOpenPlans,
        onOpenAccount = {}
    ) { modifier ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Agent Account", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = AgentShapesCard, onClick = onOpenOnboarding) {
                    Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(profile?.fullName?.ifBlank { "Agent Registration" } ?: "Agent Registration", fontWeight = FontWeight.Bold)
                        Text(profile?.businessName?.ifBlank { "Business details pending" } ?: "Business details pending", color = AgentColorsMuted)
                        Text("Status: ${profile?.onboardingStatus ?: "pending"}", color = AgentColorsAccent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = AgentColorsCard), shape = AgentShapesCard) {
                    Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Partner Preferences", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Agent partner preference tools and document vault screens are routed through the agent profile workspace, not the member account flow.", color = AgentColorsMuted)
                    }
                }
            }
        }
    }
}

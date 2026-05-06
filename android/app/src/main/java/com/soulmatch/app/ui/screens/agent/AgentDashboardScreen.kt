package com.soulmatch.app.ui.screens.agent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.ui.viewmodels.AgentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentDashboardScreen(
    onBack: () -> Unit,
    onOpenOnboarding: () -> Unit,
    vm: AgentViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Agent Dashboard", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        if (state.loading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                val profile = state.agentProfile
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(profile?.fullName ?: "Agent profile pending", fontWeight = FontWeight.Bold)
                        Text("Status: ${profile?.onboardingStatus ?: "pending"}")
                        Text("Agent ID: ${profile?.agentCode?.ifBlank { "Pending approval" } ?: "Pending approval"}")
                        Text("Plan: ${state.membership?.planId ?: "free"}")
                        if (profile?.isOnboarded != true) {
                            Button(onClick = onOpenOnboarding, modifier = Modifier.fillMaxWidth()) {
                                Text("Finish onboarding")
                            }
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Managed profiles", fontWeight = FontWeight.Bold)
                        Text("Total: ${state.managedProfiles.size}")
                        Text("Contact views used: ${state.membership?.contactViewsUsed ?: 0}")
                    }
                }
            }
            items(state.managedProfiles) { profile ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${profile.firstName} ${profile.lastName}".trim(), fontWeight = FontWeight.Bold)
                        Text("Status: ${profile.reviewStatus}")
                        Text("Views: ${profile.viewCount}   Matches: ${profile.matchCount}")
                        Text("Documents: ${profile.documentChecklistPercent}% complete")
                    }
                }
            }
            item {
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Back")
                }
            }
        }
    }
}

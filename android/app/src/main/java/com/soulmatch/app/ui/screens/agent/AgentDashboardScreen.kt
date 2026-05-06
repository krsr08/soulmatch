package com.soulmatch.app.ui.screens.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.ui.viewmodels.AgentViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AgentDashboardScreen(
    onOpenOnboarding: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenCreateProfile: () -> Unit,
    vm: AgentViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    AgentScaffold(
        title = "Agent Dashboard",
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
                        Text("Agent Dashboard", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        Text(
                            state.agentProfile?.let { "Welcome back, ${it.fullName.ifBlank { "Agent" }}." } ?: "Welcome back.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = AgentColorsMuted
                        )
                        if (state.agentProfile?.isOnboarded != true) {
                            Text("Complete your onboarding to unlock managed profiles.", color = AgentColorsAccent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DashboardStatCard("Total Profiles", state.managedProfiles.size.toString(), Icons.Outlined.People, Modifier.weight(1f))
                        DashboardStatCard(
                            "Verified",
                            state.managedProfiles.count { it.reviewStatus == "verified" }.toString(),
                            Icons.Outlined.CheckCircle,
                            Modifier.weight(1f)
                        )
                    }
                }
                item {
                    DashboardStatCard(
                        "Pending",
                        state.managedProfiles.count { it.reviewStatus == "submitted" || it.reviewStatus == "under_review" }.toString(),
                        Icons.Outlined.HourglassBottom,
                        Modifier.fillMaxWidth()
                    )
                }
                item {
                    MembershipCard(
                        planName = state.membership?.planId ?: "free",
                        used = state.managedProfiles.size,
                        allowed = state.membership?.profilesAllowed ?: 5,
                        renewNote = state.agentProfile?.membershipExpiresAt ?: "Upgrade to unlock more capacity",
                        onUpgrade = onOpenPlans
                    )
                }
                if (state.agentProfile?.isOnboarded != true) {
                    item {
                        ActionCard(
                            title = "Finish Agent Registration",
                            body = "Add your business details and KYC to continue with the agent flow.",
                            cta = "Open Agent Flow",
                            onClick = onOpenOnboarding
                        )
                    }
                }
                item {
                    Text("Recent Clients", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                items(state.managedProfiles.take(6)) { profile ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = AgentShapesCard,
                        onClick = onOpenProfiles
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${profile.firstName} ${profile.lastName}".trim().ifBlank { "Client Profile" }, fontWeight = FontWeight.Bold)
                                Text(
                                    listOf(profile.occupation, profile.city).filter { it.isNotBlank() }.joinToString(" • "),
                                    color = AgentColorsMuted
                                )
                            }
                            StatusChip(profile.reviewStatus)
                        }
                    }
                }
            }
            ExtendedFloatingActionButton(
                onClick = onOpenCreateProfile,
                containerColor = AgentColorsAccent,
                contentColor = Color.White,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("New Client") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            )
        }
    }
}

@Composable
private fun DashboardStatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = AgentColorsCard),
        shape = AgentShapesCard
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(AgentColorsAccent.copy(alpha = 0.1f), CircleShape)
                    .padding(10.dp)
            ) {
                Icon(icon, contentDescription = null, tint = AgentColorsAccent)
            }
            Text(title, color = AgentColorsMuted)
            Text(value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MembershipCard(planName: String, used: Int, allowed: Int, renewNote: String, onUpgrade: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AgentColorsAccent),
        shape = AgentShapesCard,
        onClick = onUpgrade
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(planName.uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
            Text("$used / ${if (allowed < 0) "Unlimited" else allowed} Profiles", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = if (allowed <= 0) 1f else (used.toFloat() / allowed.toFloat()).coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.24f)
            )
            Text(renewNote, color = Color.White.copy(alpha = 0.86f))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ActionCard(title: String, body: String, cta: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = AgentShapesCard,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(body, color = AgentColorsMuted)
            Text(cta, color = AgentColorsAccent, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    Box(
        modifier = Modifier
            .background(AgentColorsAccent.copy(alpha = 0.1f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(status.replace('_', ' ').uppercase(), color = AgentColorsAccent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

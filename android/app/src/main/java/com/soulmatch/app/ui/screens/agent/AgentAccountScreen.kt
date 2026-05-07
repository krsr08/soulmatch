package com.soulmatch.app.ui.screens.agent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onDrawerDestination: (AgentDrawerDestination) -> Unit,
    vm: AgentViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val profile = state.agentProfile
    var enabled by remember(profile?.feePreferences) { mutableStateOf(profile?.feePreferences?.get("enabled").equals("true", ignoreCase = true)) }
    var verifiedRate by remember(profile?.feePreferences) { mutableStateOf(profile?.feePreferences?.get("verifiedProfileRateInr").orEmpty()) }
    var successfulMatchRate by remember(profile?.feePreferences) { mutableStateOf(profile?.feePreferences?.get("successfulMatchRateInr").orEmpty()) }
    var monthlyTarget by remember(profile?.feePreferences) { mutableStateOf(profile?.feePreferences?.get("monthlyTargetInr").orEmpty()) }

    AgentScaffold(
        title = "Account",
        selectedTab = AgentTab.Account,
        onOpenDashboard = onOpenDashboard,
        onOpenProfiles = onOpenProfiles,
        onOpenPlans = onOpenPlans,
        onOpenAccount = {},
        onDrawerDestination = onDrawerDestination
    ) { modifier ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Agent Account", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp)
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = AgentShapesCard,
                    border = BorderStroke(1.dp, Color(0xFFF0E2DE)),
                    onClick = onOpenOnboarding
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(profile?.fullName?.ifBlank { "Agent Registration" } ?: "Agent Registration", fontWeight = FontWeight.SemiBold)
                        Text(profile?.businessName?.ifBlank { "Business details pending" } ?: "Business details pending", color = AgentColorsMuted)
                        Text("Status: ${profile?.onboardingStatus ?: "pending"}", color = AgentColorsAccent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item {
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
                        Text("Commission Settings", fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
                        Text("Show the monthly commission card on the dashboard only when your rate card is configured.", color = AgentColorsMuted)
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Enable commission tracking", fontWeight = FontWeight.Medium)
                                Text("Currency is fixed to INR for now.", color = AgentColorsMuted, style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = enabled, onCheckedChange = { enabled = it })
                        }
                        OutlinedTextField(
                            value = verifiedRate,
                            onValueChange = { verifiedRate = it },
                            label = { Text("Verified profile rate (INR)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = successfulMatchRate,
                            onValueChange = { successfulMatchRate = it },
                            label = { Text("Successful match rate (INR)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = monthlyTarget,
                            onValueChange = { monthlyTarget = it },
                            label = { Text("Monthly target (INR)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            singleLine = true
                        )
                        state.saveMessage?.takeIf { it.isNotBlank() }?.let {
                            Text(it, color = AgentColorsAccent, style = MaterialTheme.typography.bodySmall)
                        }
                        state.error?.takeIf { it.isNotBlank() }?.let {
                            Text(it, color = Color(0xFF9B4156), style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = {
                                vm.saveCommissionPreferences(
                                    enabled = enabled,
                                    verifiedProfileRate = verifiedRate,
                                    successfulMatchRate = successfulMatchRate,
                                    monthlyTarget = monthlyTarget
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.saving,
                            colors = ButtonDefaults.buttonColors(containerColor = AgentColorsAccent, contentColor = Color.White)
                        ) {
                            Text("Save Rate Card")
                        }
                    }
                }
            }
        }
    }
}

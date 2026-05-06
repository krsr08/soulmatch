package com.soulmatch.app.ui.screens.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.data.models.AgentOnboardingRequest
import com.soulmatch.app.ui.viewmodels.AgentViewModel

@Composable
fun AgentOnboardingScreen(
    onBack: () -> Unit,
    onCompleted: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenAccount: () -> Unit,
    vm: AgentViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var fullName by remember { mutableStateOf(state.agentProfile?.fullName.orEmpty()) }
    var phone by remember { mutableStateOf(state.agentProfile?.phone.orEmpty()) }
    var email by remember { mutableStateOf(state.agentProfile?.email.orEmpty()) }
    var city by remember { mutableStateOf(state.agentProfile?.city.orEmpty()) }
    var stateName by remember { mutableStateOf(state.agentProfile?.state.orEmpty()) }
    var businessName by remember { mutableStateOf(state.agentProfile?.businessName.orEmpty()) }
    var referralCode by remember { mutableStateOf(state.agentProfile?.referralCode.orEmpty()) }
    AgentScaffold(
        title = "Agent Registration",
        selectedTab = AgentTab.Account,
        onOpenDashboard = onOpenDashboard,
        onOpenProfiles = onOpenProfiles,
        onOpenPlans = onOpenPlans,
        onOpenAccount = onOpenAccount
    ) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("STEP 1 OF 2", color = AgentColorsAccent, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color(0xFFE8DDD9), RoundedCornerShape(999.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(6.dp)
                        .background(AgentColorsAccent, RoundedCornerShape(999.dp))
                ) {}
            }
            Text("Business Details", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Register your agent workspace. Once approved, you will manage clients from the agent dashboard only.", color = AgentColorsMuted)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = AgentShapesCard
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(value = businessName, onValueChange = { businessName = it }, label = { Text("Business / Agency Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Professional Email") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Mobile Number") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(value = stateName, onValueChange = { stateName = it }, label = { Text("State") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(value = referralCode, onValueChange = { referralCode = it }, label = { Text("Referral Code (Optional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                    Text("KYC Verification", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Please upload your primary identification documents for platform security.", color = AgentColorsMuted)
                    DocumentBox("Aadhaar Card", "Recommended for faster approval", highlighted = true)
                    DocumentBox("PAN Card", "Alternative verification", highlighted = false)
                    state.error?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = AgentColorsAccent, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            vm.submitOnboarding(
                                AgentOnboardingRequest(
                                    fullName = fullName,
                                    phone = phone,
                                    email = email,
                                    city = city,
                                    state = stateName,
                                    businessName = businessName,
                                    referralCode = referralCode
                                ),
                                onCompleted = onCompleted
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AgentColorsAccent),
                        enabled = !state.saving
                    ) {
                        if (state.saving) {
                            CircularProgressIndicator(color = Color.White)
                        } else {
                            Text("Register as Agent")
                        }
                    }
                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = AgentColorsAccent)
                    ) {
                        Text("Back")
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentBox(title: String, subtitle: String, highlighted: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (highlighted) AgentColorsAccent else Color(0xFFEAD8D2),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold)
        Text(subtitle, color = AgentColorsMuted)
    }
}

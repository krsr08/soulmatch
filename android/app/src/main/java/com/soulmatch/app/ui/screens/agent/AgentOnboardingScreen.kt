package com.soulmatch.app.ui.screens.agent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.data.models.AgentOnboardingRequest
import com.soulmatch.app.ui.viewmodels.AgentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentOnboardingScreen(
    onBack: () -> Unit,
    onCompleted: () -> Unit,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agent Onboarding", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Complete your business profile to unlock managed profiles and review workflows.")
            OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Mobile number") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = stateName, onValueChange = { stateName = it }, label = { Text("State") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = businessName, onValueChange = { businessName = it }, label = { Text("Business / agency name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = referralCode, onValueChange = { referralCode = it }, label = { Text("Referral code (optional)") }, modifier = Modifier.fillMaxWidth())
            state.error?.takeIf { it.isNotBlank() }?.let { Text(it) }
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
                enabled = !state.saving
            ) {
                if (state.saving) {
                    CircularProgressIndicator()
                } else {
                    Text("Submit for approval")
                }
            }
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
    }
}

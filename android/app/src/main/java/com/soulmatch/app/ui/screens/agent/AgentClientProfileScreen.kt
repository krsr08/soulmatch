package com.soulmatch.app.ui.screens.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun AgentClientProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenAccount: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Select Gender") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var complexion by remember { mutableStateOf("Fair") }
    val complexions = listOf("Fair", "Wheatish", "Tan", "Dark", "V. Fair", "Other")
    AgentScaffold(
        title = "Client Profile",
        selectedTab = AgentTab.Profiles,
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
            Text("STEP 1 OF 4", color = AgentColorsAccent, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color(0xFFE8DDD9), RoundedCornerShape(999.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.25f)
                        .height(6.dp)
                        .background(AgentColorsAccent, RoundedCornerShape(999.dp))
                ) {}
            }
            Text("Tell us about your client", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = AgentColorsAccent)
            Text("Start the journey by providing the essential details.", color = AgentColorsMuted)
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = AgentShapesCard) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(value = dob, onValueChange = { dob = it }, label = { Text("Date of Birth") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF7EEE9), RoundedCornerShape(16.dp))
                            .clickable { gender = if (gender == "Select Gender") "Male" else "Female" }
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(gender)
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null)
                    }
                    OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Height (in cm)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight (in kg)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                    Text("Complexion Type", fontWeight = FontWeight.SemiBold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        complexions.forEach { item ->
                            Text(
                                item,
                                modifier = Modifier
                                    .background(
                                        if (complexion == item) AgentColorsAccent.copy(alpha = 0.12f) else Color.White,
                                        RoundedCornerShape(999.dp)
                                    )
                                    .clickable { complexion = item }
                                    .padding(horizontal = 18.dp, vertical = 12.dp),
                                color = if (complexion == item) AgentColorsAccent else Color.Black
                            )
                        }
                    }
                    Button(
                        onClick = onSaved,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AgentColorsAccent)
                    ) {
                        Text("Next Step")
                    }
                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = AgentColorsAccent)
                    ) {
                        Text("Save Draft")
                    }
                }
            }
        }
    }
}

package com.soulmatch.app.ui.screens.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.data.models.AgentManagedProfileCreateRequest
import com.soulmatch.app.ui.viewmodels.AgentViewModel

@Composable
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
fun AgentClientProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenAccount: () -> Unit,
    vm: AgentViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var religion by remember { mutableStateOf("") }
    var caste by remember { mutableStateOf("") }
    var motherTongue by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var complexion by remember { mutableStateOf("Fair") }
    var education by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }
    var income by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var stateName by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var fatherOccupation by remember { mutableStateOf("") }
    var motherOccupation by remember { mutableStateOf("") }
    var familyType by remember { mutableStateOf("Nuclear") }
    var diet by remember { mutableStateOf("Vegetarian") }
    var aboutMe by remember { mutableStateOf("") }
    val complexions = listOf("Fair", "Wheatish", "Tan", "Dark", "Very Fair", "Other")

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add Member Profile", fontFamily = FontFamily.Serif, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Capture the member details once. The profile will move into pending verification after save.",
                    color = AgentColorsMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = AgentShapesCard) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Basic Information", fontWeight = FontWeight.SemiBold, color = AgentColorsAccent)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AgentField("First Name", firstName, { firstName = it }, Modifier.weight(1f))
                        AgentField("Last Name", lastName, { lastName = it }, Modifier.weight(1f))
                    }
                    AgentField("Date of Birth (YYYY-MM-DD)", dob, { dob = it })
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AgentChoiceField("Gender", gender, Modifier.weight(1f))
                        AgentField("Religion", religion, { religion = it }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AgentField("Caste", caste, { caste = it }, Modifier.weight(1f))
                        AgentField("Mother Tongue", motherTongue, { motherTongue = it }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AgentField("Height (cm)", height, { height = it }, Modifier.weight(1f))
                        AgentField("Weight (kg)", weight, { weight = it }, Modifier.weight(1f))
                    }
                    Text("Complexion", fontWeight = FontWeight.SemiBold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        complexions.forEach { item ->
                            ChoiceChip(
                                label = item,
                                selected = complexion == item,
                                onClick = { complexion = item }
                            )
                        }
                    }
                    Text("Education & Work", fontWeight = FontWeight.SemiBold, color = AgentColorsAccent)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AgentField("Education", education, { education = it }, Modifier.weight(1f))
                        AgentField("Occupation", occupation, { occupation = it }, Modifier.weight(1f))
                    }
                    AgentField("Annual Income", income, { income = it })
                    Text("Contact & Family", fontWeight = FontWeight.SemiBold, color = AgentColorsAccent)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AgentField("Mobile", mobile, { mobile = it }, Modifier.weight(1f))
                        AgentField("Email", email, { email = it }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AgentField("City", city, { city = it }, Modifier.weight(1f))
                        AgentField("State", stateName, { stateName = it }, Modifier.weight(1f))
                    }
                    AgentField("Pincode", pincode, { pincode = it })
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AgentField("Father's Occupation", fatherOccupation, { fatherOccupation = it }, Modifier.weight(1f))
                        AgentField("Mother's Occupation", motherOccupation, { motherOccupation = it }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AgentChoiceField("Family Type", familyType, Modifier.weight(1f))
                        AgentChoiceField("Diet", diet, Modifier.weight(1f))
                    }
                    OutlinedTextField(
                        value = aboutMe,
                        onValueChange = { aboutMe = it },
                        label = { Text("About Member") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        minLines = 4
                    )
                    state.error?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = AgentColorsAccent, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = {
                            vm.createManagedProfile(
                                AgentManagedProfileCreateRequest(
                                    firstName = firstName,
                                    lastName = lastName,
                                    dob = dob,
                                    gender = gender.lowercase(),
                                    religion = religion,
                                    caste = caste,
                                    motherTongue = motherTongue,
                                    heightCm = height.toIntOrNull(),
                                    weightKg = weight.toIntOrNull(),
                                    complexion = complexion,
                                    educationLevel = education,
                                    occupation = occupation,
                                    annualIncome = income,
                                    city = city,
                                    state = stateName,
                                    pincode = pincode,
                                    mobile = mobile,
                                    email = email,
                                    fatherOccupation = fatherOccupation,
                                    motherOccupation = motherOccupation,
                                    familyType = familyType.lowercase(),
                                    diet = diet.lowercase(),
                                    aboutMe = aboutMe
                                ),
                                onCompleted = onSaved
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AgentColorsAccent),
                        enabled = !state.saving
                    ) {
                        if (state.saving) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Save And Send To Verification")
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
private fun AgentField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        singleLine = true
    )
}

@Composable
private fun AgentChoiceField(
    label: String,
    value: String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Row(
        modifier = modifier
            .background(Color(0xFFF8EFED), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = AgentColorsMuted, style = MaterialTheme.typography.labelSmall)
            Text(value, fontWeight = FontWeight.Medium)
        }
        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = AgentColorsMuted)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (selected) AgentColorsAccent.copy(alpha = 0.14f) else Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) AgentColorsAccent else Color(0xFFE7DAD7)),
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            color = if (selected) AgentColorsAccent else Color(0xFF4A3B40)
        )
    }
}

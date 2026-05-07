package com.soulmatch.app.ui.screens.agent

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.soulmatch.app.data.models.AgentManagedProfileCreateRequest
import com.soulmatch.app.ui.viewmodels.AgentViewModel

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun AgentClientProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenAccount: () -> Unit,
    onDrawerDestination: (AgentDrawerDestination) -> Unit,
    vm: AgentViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var step by remember { mutableStateOf(1) }

    var fullName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Female") }
    var dob by remember { mutableStateOf("") }
    var religion by remember { mutableStateOf("") }

    var email by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("India") }
    var stateName by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }

    var qualification by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var annualIncome by remember { mutableStateOf("") }

    var fatherOccupation by remember { mutableStateOf("") }
    var motherOccupation by remember { mutableStateOf("") }
    var siblingsCount by remember { mutableStateOf("") }
    var familyType by remember { mutableStateOf("Nuclear") }

    var aboutMember by remember { mutableStateOf("") }
    val selectedInterests = remember { mutableStateListOf<String>() }
    var primaryPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var secondaryPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var tertiaryPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var publishProfile by remember { mutableStateOf(true) }

    var activePhotoSlot by remember { mutableStateOf(0) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        when (activePhotoSlot) {
            0 -> primaryPhotoUri = uri
            1 -> secondaryPhotoUri = uri
            2 -> tertiaryPhotoUri = uri
        }
    }

    val stepValid = when (step) {
        1 -> fullName.isNotBlank() && gender.isNotBlank() && dob.isNotBlank() && religion.isNotBlank()
        2 -> email.isNotBlank() && mobile.isNotBlank() && country.isNotBlank() && stateName.isNotBlank() && city.isNotBlank()
        3 -> qualification.isNotBlank() && occupation.isNotBlank()
        4 -> fatherOccupation.isNotBlank() && motherOccupation.isNotBlank() && siblingsCount.isNotBlank() && familyType.isNotBlank()
        5 -> primaryPhotoUri != null && aboutMember.trim().length >= 30 && selectedInterests.isNotEmpty()
        else -> true
    }

    AgentScaffold(
        title = "Add New Member",
        selectedTab = AgentTab.Profiles,
        onOpenDashboard = onOpenDashboard,
        onOpenProfiles = onOpenProfiles,
        onOpenPlans = onOpenPlans,
        onOpenAccount = onOpenAccount,
        onDrawerDestination = onDrawerDestination,
        topBarMode = AgentTopBarMode.Close,
        showBottomBar = false,
        onBack = onBack
    ) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(AgentColorsSurface)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                StepHeader(step = step)
                when (step) {
                    1 -> AgentMemberStepOne(
                        fullName = fullName,
                        onFullNameChange = { fullName = it },
                        gender = gender,
                        onGenderChange = { gender = it },
                        dob = dob,
                        onDobChange = { dob = it },
                        religion = religion,
                        onReligionChange = { religion = it }
                    )

                    2 -> AgentMemberStepTwo(
                        email = email,
                        onEmailChange = { email = it },
                        mobile = mobile,
                        onMobileChange = { mobile = it },
                        country = country,
                        onCountryChange = { country = it },
                        stateName = stateName,
                        onStateChange = { stateName = it },
                        city = city,
                        onCityChange = { city = it }
                    )

                    3 -> AgentMemberStepThree(
                        qualification = qualification,
                        onQualificationChange = { qualification = it },
                        occupation = occupation,
                        onOccupationChange = { occupation = it },
                        companyName = companyName,
                        onCompanyNameChange = { companyName = it },
                        annualIncome = annualIncome,
                        onAnnualIncomeChange = { annualIncome = it }
                    )

                    4 -> AgentMemberStepFour(
                        fatherOccupation = fatherOccupation,
                        onFatherOccupationChange = { fatherOccupation = it },
                        motherOccupation = motherOccupation,
                        onMotherOccupationChange = { motherOccupation = it },
                        siblingsCount = siblingsCount,
                        onSiblingsCountChange = { siblingsCount = it },
                        familyType = familyType,
                        onFamilyTypeChange = { familyType = it }
                    )

                    5 -> AgentMemberStepFive(
                        primaryPhotoUri = primaryPhotoUri,
                        secondaryPhotoUri = secondaryPhotoUri,
                        tertiaryPhotoUri = tertiaryPhotoUri,
                        onPickPhoto = { slot ->
                            activePhotoSlot = slot
                            photoPicker.launch("image/*")
                        },
                        aboutMember = aboutMember,
                        onAboutMemberChange = { aboutMember = it },
                        selectedInterests = selectedInterests,
                        onToggleInterest = { label ->
                            if (selectedInterests.contains(label)) {
                                selectedInterests.remove(label)
                            } else {
                                selectedInterests.add(label)
                            }
                        }
                    )

                    6 -> AgentMemberStepSix(
                        fullName = fullName,
                        city = city,
                        stateName = stateName,
                        religion = religion,
                        qualification = qualification,
                        occupation = occupation,
                        annualIncome = annualIncome,
                        aboutMember = aboutMember,
                        primaryPhotoUri = primaryPhotoUri,
                        publishProfile = publishProfile,
                        onPublishProfileChange = { publishProfile = it }
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFFFCFA),
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, Color(0xFFE7D9D4))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (step == 1) onBack() else step -= 1
                        },
                        modifier = Modifier.weight(1f).height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF8F0EE),
                            contentColor = Color(0xFF866F1A)
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Previous Step")
                    }
                    Button(
                        onClick = {
                            if (step < 6) {
                                step += 1
                            } else {
                                val nameParts = splitFullName(fullName)
                                val interestsSummary = if (selectedInterests.isEmpty()) "" else "\nInterests: ${selectedInterests.joinToString()}"
                                val companySummary = companyName.takeIf { it.isNotBlank() }?.let { "\nCompany: $it" }.orEmpty()
                                vm.createManagedProfile(
                                    AgentManagedProfileCreateRequest(
                                        firstName = nameParts.first,
                                        lastName = nameParts.second,
                                        dob = dob,
                                        gender = gender.lowercase(),
                                        religion = religion,
                                        educationLevel = qualification,
                                        occupation = occupation,
                                        annualIncome = annualIncome,
                                        city = city,
                                        state = stateName,
                                        mobile = mobile,
                                        email = email,
                                        fatherOccupation = fatherOccupation,
                                        motherOccupation = motherOccupation,
                                        numBrothers = siblingsCount.toIntOrNull() ?: 0,
                                        numSisters = 0,
                                        familyType = familyType.lowercase(),
                                        aboutMe = buildString {
                                            append(aboutMember.trim())
                                            append(companySummary)
                                            append(interestsSummary)
                                            if (!publishProfile) {
                                                append("\nVisibility: hold for review")
                                            }
                                        }
                                    ),
                                    onCompleted = onSaved
                                )
                            }
                        },
                        modifier = Modifier.weight(1f).height(54.dp),
                        enabled = stepValid && !state.saving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AgentColorsAccent,
                            contentColor = Color.White,
                            disabledContainerColor = AgentColorsAccent.copy(alpha = 0.45f)
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        if (state.saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text(if (step == 6) "Publish Profile" else "Continue")
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepHeader(step: Int) {
    val title = when (step) {
        1 -> "Basic Personal\nInformation"
        2 -> "Contact &\nLocation"
        3 -> "Education &\nCareer"
        4 -> "Family Details"
        5 -> "Lifestyle & Photos"
        else -> "Review & Publish"
    }
    val subtitle = when (step) {
        1 -> "Let us begin by capturing the fundamental details of the member's profile."
        2 -> "Share how matches and families can reach the member while keeping everything secure."
        3 -> "Document the professional background to support better matching and filtering."
        4 -> "Capture a simple family snapshot so profile context feels complete and respectful."
        5 -> "Add photos, a thoughtful introduction, and a few interests to make the profile feel human."
        else -> "Review the captured details before sending this member profile into verification."
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                "STEP $step OF 6",
                color = Color(0xFF8D7416),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .width(220.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFE7E1DE))
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth(step / 6f)
                        .height(6.dp)
                        .background(AgentColorsAccent)
                )
            }
        }
        Text(
            title,
            style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                lineHeight = 52.sp
            ),
            color = Color(0xFF52071E)
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF5B454A),
            lineHeight = 29.sp
        )
        DecorativeDivider()
    }
}

@Composable
private fun AgentMemberStepOne(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    gender: String,
    onGenderChange: (String) -> Unit,
    dob: String,
    onDobChange: (String) -> Unit,
    religion: String,
    onReligionChange: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = AgentShapesCard,
        border = BorderStroke(1.dp, Color(0xFFEEE0DB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SoulTextField("Full Name", fullName, onFullNameChange, placeholder = "Enter legal name")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Gender", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("Male", "Female", "Other").forEach { option ->
                        SelectablePill(
                            label = option,
                            selected = gender == option,
                            onClick = { onGenderChange(option) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            SoulTextField(
                "Date of Birth",
                dob,
                onDobChange,
                placeholder = "YYYY-MM-DD",
                leadingIcon = Icons.Outlined.CalendarMonth
            )
            SoulTextField(
                "Religion / Faith",
                religion,
                onReligionChange,
                placeholder = "Select faith tradition"
            )
        }
    }
}

@Composable
private fun AgentMemberStepTwo(
    email: String,
    onEmailChange: (String) -> Unit,
    mobile: String,
    onMobileChange: (String) -> Unit,
    country: String,
    onCountryChange: (String) -> Unit,
    stateName: String,
    onStateChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = AgentShapesCard,
        border = BorderStroke(1.dp, Color(0xFFEEE0DB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Direct Contact", style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif), color = Color(0xFF52071E))
                SoulTextField("Email Address", email, onEmailChange, placeholder = "youremail@example.com", leadingIcon = Icons.Outlined.Email)
                SoulTextField("Mobile Number", mobile, onMobileChange, placeholder = "+91 98765 43210", leadingIcon = Icons.Outlined.Phone)
            }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Current Location", style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif), color = Color(0xFF52071E))
                DropdownField("Country", country, listOf("India", "United Arab Emirates", "United Kingdom", "United States"), onCountryChange)
                SoulTextField("State / Province", stateName, onStateChange, placeholder = "e.g. Karnataka")
                SoulTextField("City", city, onCityChange, placeholder = "e.g. Bengaluru", leadingIcon = Icons.Outlined.LocationOn)
            }
        }
    }
}

@Composable
private fun AgentMemberStepThree(
    qualification: String,
    onQualificationChange: (String) -> Unit,
    occupation: String,
    onOccupationChange: (String) -> Unit,
    companyName: String,
    onCompanyNameChange: (String) -> Unit,
    annualIncome: String,
    onAnnualIncomeChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DropdownField(
            label = "Highest Qualification",
            value = qualification,
            options = listOf("Graduate", "Post Graduate", "MBA", "Doctorate", "Professional Degree"),
            onValueChange = onQualificationChange
        )
        SoulTextField("Occupation", occupation, onOccupationChange, placeholder = "e.g. Architect, Physician, Entrepreneur", leadingIcon = Icons.Outlined.WorkOutline)
        SoulTextField("Company Name", companyName, onCompanyNameChange, placeholder = "Where do they work?", leadingIcon = Icons.Outlined.Apartment)
        DropdownField(
            label = "Annual Income (Optional)",
            value = annualIncome,
            options = listOf("Below 5 LPA", "5L - 10L", "10L - 15L", "15L - 25L", "25L+"),
            onValueChange = onAnnualIncomeChange
        )
        Text(
            "This information is kept strictly confidential.",
            color = AgentColorsMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun AgentMemberStepFour(
    fatherOccupation: String,
    onFatherOccupationChange: (String) -> Unit,
    motherOccupation: String,
    onMotherOccupationChange: (String) -> Unit,
    siblingsCount: String,
    onSiblingsCountChange: (String) -> Unit,
    familyType: String,
    onFamilyTypeChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = AgentShapesCard,
            border = BorderStroke(1.dp, Color(0xFFEEE0DB))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SoulTextField("Father's Occupation", fatherOccupation, onFatherOccupationChange, placeholder = "e.g. Engineer, Retired")
                SoulTextField("Mother's Occupation", motherOccupation, onMotherOccupationChange, placeholder = "e.g. Teacher, Homemaker")
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = AgentShapesCard,
            border = BorderStroke(1.dp, Color(0xFFEEE0DB))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DropdownField(
                    label = "Number of Siblings",
                    value = siblingsCount,
                    options = listOf("0", "1", "2", "3", "4", "5+"),
                    onValueChange = onSiblingsCountChange
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Family Type", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("Nuclear", "Joint").forEach { option ->
                            SelectablePill(
                                label = option,
                                selected = familyType == option,
                                onClick = { onFamilyTypeChange(option) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AgentMemberStepFive(
    primaryPhotoUri: Uri?,
    secondaryPhotoUri: Uri?,
    tertiaryPhotoUri: Uri?,
    onPickPhoto: (Int) -> Unit,
    aboutMember: String,
    onAboutMemberChange: (String) -> Unit,
    selectedInterests: List<String>,
    onToggleInterest: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Your Portraits",
                style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif),
                color = Color(0xFF52071E)
            )
            Text(
                "Upload at least 1 photo. We recommend clear, well-lit portraits that reflect the member naturally.",
                color = AgentColorsMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        PhotoUploadTile(
            title = "Upload Primary Photo",
            uri = primaryPhotoUri,
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
            onClick = { onPickPhoto(0) }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            PhotoUploadTile(
                title = "Add Photo",
                uri = secondaryPhotoUri,
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp),
                onClick = { onPickPhoto(1) }
            )
            PhotoUploadTile(
                title = "Add Photo",
                uri = tertiaryPhotoUri,
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp),
                onClick = { onPickPhoto(2) }
            )
        }
        DecorativeDivider()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "About Member",
                style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif),
                color = Color(0xFF52071E)
            )
            Text(
                "Write a few heartfelt words about values, aspirations, and the kind of partnership being sought.",
                color = AgentColorsMuted,
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = aboutMember,
                onValueChange = onAboutMemberChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                maxLines = 7,
                placeholder = { Text("I value honesty, quiet evenings, and a shared appreciation for art...") },
                shape = RoundedCornerShape(22.dp)
            )
        }
        DecorativeDivider()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Interests & Pursuits",
                style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif),
                color = Color(0xFF52071E)
            )
            Text(
                "Select topics that resonate with the member lifestyle.",
                color = AgentColorsMuted,
                style = MaterialTheme.typography.bodyMedium
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Classical Music", "Culinary Arts", "Literature", "Fine Art", "Travel", "Philanthropy").forEach { interest ->
                    InterestChip(
                        label = interest,
                        selected = selectedInterests.contains(interest),
                        onClick = { onToggleInterest(interest) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentMemberStepSix(
    fullName: String,
    city: String,
    stateName: String,
    religion: String,
    qualification: String,
    occupation: String,
    annualIncome: String,
    aboutMember: String,
    primaryPhotoUri: Uri?,
    publishProfile: Boolean,
    onPublishProfileChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = AgentShapesCard,
            border = BorderStroke(1.dp, Color(0xFFEEE0DB))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(88.dp),
                        shape = CircleShape,
                        color = Color(0xFFFFF0F4),
                        border = BorderStroke(1.dp, Color(0xFFE7D9D4))
                    ) {
                        if (primaryPhotoUri != null) {
                            AsyncImage(
                                model = primaryPhotoUri,
                                contentDescription = fullName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = AgentColorsAccent)
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                        Text(
                            fullName.ifBlank { "Member Profile" },
                            style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif),
                            color = Color(0xFF241618)
                        )
                        Text(
                            listOf(city, stateName).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "Location not added yet" },
                            color = AgentColorsMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Surface(color = Color(0xFFFFFCFA), border = BorderStroke(1.dp, Color(0xFFEEE0DB))) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SummaryMetric("Faith / Values", religion.ifBlank { "To be added" })
                        SummaryMetric("Education", qualification.ifBlank { "To be added" })
                        SummaryMetric("Occupation", occupation.ifBlank { "To be added" })
                        SummaryMetric("Income", annualIncome.ifBlank { "Not disclosed" })
                        SummaryMetric("Biography Excerpt", aboutMember.ifBlank { "The biography will appear here once added." })
                    }
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = AgentShapesCard,
            color = Color(0xFFF6F1EE),
            border = BorderStroke(1.dp, Color(0xFFE7D9D4))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Profile Visibility",
                        style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif),
                        color = Color(0xFF52071E)
                    )
                    Text(
                        "Enable this to send the profile forward for review immediately after submission.",
                        color = AgentColorsMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Switch(checked = publishProfile, onCheckedChange = onPublishProfileChange)
            }
        }
    }
}

@Composable
private fun DecorativeDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .width(70.dp)
                .height(1.dp)
                .background(Color(0xFFD9C98E))
        )
        Surface(
            modifier = Modifier.padding(horizontal = 14.dp),
            shape = CircleShape,
            color = Color(0xFFFFFCFA)
        ) {
            Text("♡", color = Color(0xFFD0B14A), modifier = Modifier.padding(horizontal = 3.dp))
        }
        Spacer(
            modifier = Modifier
                .width(70.dp)
                .height(1.dp)
                .background(Color(0xFFD9C98E))
        )
    }
}

@Composable
private fun SoulTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = Color(0xFFC4B6B3)) },
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null, tint = AgentColorsMuted) } },
        singleLine = true,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE7D9D4))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    value.ifBlank { "Select option" },
                    color = if (value.isBlank()) Color(0xFFB3A7A3) else Color(0xFF201517),
                    style = MaterialTheme.typography.bodyLarge
                )
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = AgentColorsMuted)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SelectablePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) Color(0xFFFFF0F4) else Color.White,
        border = BorderStroke(1.dp, if (selected) AgentColorsAccent else Color(0xFFE9DCD7))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                color = if (selected) AgentColorsAccent else Color(0xFF47383D),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PhotoUploadTile(
    title: String,
    uri: Uri?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE7D9D4))
    ) {
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = AgentColorsMuted, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(10.dp))
                Text(title, textAlign = TextAlign.Center, color = Color(0xFF433539))
            }
        }
    }
}

@Composable
private fun InterestChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (selected) Color(0xFFFFF0F4) else Color(0xFFF7F1ED),
        border = BorderStroke(1.dp, if (selected) AgentColorsAccent else Color(0xFFE7D9D4)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            color = if (selected) AgentColorsAccent else Color(0xFF55484A),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SummaryMetric(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label.uppercase(),
            color = Color(0xFF7E6D70),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            value,
            color = Color(0xFF211517),
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 28.sp,
            maxLines = if (label == "Biography Excerpt") 4 else 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun splitFullName(value: String): Pair<String, String> {
    val parts = value.trim().split(" ").filter { it.isNotBlank() }
    if (parts.isEmpty()) return "" to ""
    if (parts.size == 1) return parts.first() to ""
    return parts.first() to parts.drop(1).joinToString(" ")
}

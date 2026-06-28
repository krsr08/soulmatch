package com.soulmatch.app.ui.screens.auth

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.data.models.PartnerPreferencesData
import com.soulmatch.app.data.models.ProfileData
import com.soulmatch.app.ui.components.premium.ChipTone
import com.soulmatch.app.ui.components.premium.FilterChoiceChip
import com.soulmatch.app.ui.components.premium.LabeledProgress
import com.soulmatch.app.ui.components.premium.PremiumCard
import com.soulmatch.app.ui.components.premium.PremiumScreen
import com.soulmatch.app.ui.components.premium.SignalChip
import com.soulmatch.app.ui.components.premium.SignalChips
import com.soulmatch.app.ui.design.SoulMatchTokens
import com.soulmatch.app.ui.theme.Success
import com.soulmatch.app.ui.titleCase
import com.soulmatch.app.ui.viewmodels.ProfileViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.roundToInt

private data class WizardStepCopy(
    val title: String,
    val eyebrow: String,
    val subtitle: String,
    val helper: String,
    val info: String
)

private val wizardCopy = mapOf(
    1 to WizardStepCopy(
        title = "Basic details",
        eyebrow = "Step 1 of 6",
        subtitle = "Start with identity basics first.",
        helper = "Name, date of birth, and gender are required.",
        info = "Basic details help SoulMatch create your member identity and decide what screen comes next."
    ),
    2 to WizardStepCopy(
        title = "Religious and community",
        eyebrow = "Step 2 of 6",
        subtitle = "Keep this respectful and accurate for family-led matching.",
        helper = "Religion, community, mother tongue, and marital status are required.",
        info = "These details help family filtering, search relevance, and partner preference matching."
    ),
    3 to WizardStepCopy(
        title = "Education and career",
        eyebrow = "Step 3 of 6",
        subtitle = "Add professional context that supports serious introductions.",
        helper = "Career details power search, ranking, and partner preference matching.",
        info = "Education and work details directly affect search, ranking, and shortlist quality."
    ),
    4 to WizardStepCopy(
        title = "Family details",
        eyebrow = "Step 4 of 6",
        subtitle = "Share the family context most parents look for first.",
        helper = "Parent occupations, siblings, family type, and family city are required.",
        info = "Family details help parents review profile fit faster and with more trust."
    ),
    5 to WizardStepCopy(
        title = "Lifestyle details",
        eyebrow = "Step 5 of 6",
        subtitle = "Help matches understand daily habits and communication style.",
        helper = "Diet, habits, and a thoughtful about section help serious members decide with confidence.",
        info = "Lifestyle details give families and matches a clearer idea of daily compatibility."
    ),
    6 to WizardStepCopy(
        title = "Partner preferences",
        eyebrow = "Step 6 of 6",
        subtitle = "Set the match basics you care about most before profile review.",
        helper = "These values go to the backend and power recommendations, filters, and shortlist quality.",
        info = "Partner preferences directly drive recommendation quality and shortlist relevance."
    )
)

private val indianStates = listOf(
    "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh", "Goa", "Gujarat",
    "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka", "Kerala", "Madhya Pradesh",
    "Maharashtra", "Manipur", "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Punjab",
    "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura", "Uttar Pradesh",
    "Uttarakhand", "West Bengal", "Andaman and Nicobar Islands", "Chandigarh",
    "Dadra and Nagar Haveli and Daman and Diu", "Delhi", "Jammu and Kashmir", "Ladakh",
    "Lakshadweep", "Puducherry"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileWizardScreen(
    step: Int,
    isSectionEdit: Boolean = false,
    onNextStep: (Int) -> Unit,
    onBack: () -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val currentStep = step.coerceIn(1, 6)
    val profile by vm.profile.collectAsStateWithLifecycle()
    val isSaving by vm.isSaving.collectAsStateWithLifecycle()
    val errorMessage by vm.errorMessage.collectAsStateWithLifecycle()
    val loadMessage by vm.loadMessage.collectAsStateWithLifecycle()
    var isCurrentStepValid by remember(currentStep) { mutableStateOf(false) }
    var showInfo by remember(currentStep) { mutableStateOf(false) }
    val copy = wizardCopy.getValue(currentStep)

    if (showInfo) {
        ModalBottomSheet(onDismissRequest = { showInfo = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(copy.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = SoulMatchTokens.Text)
                Text(copy.info, style = MaterialTheme.typography.bodyMedium, color = SoulMatchTokens.Muted)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(copy.title, fontWeight = FontWeight.Bold, color = SoulMatchTokens.Text)
                        Text(copy.eyebrow, style = MaterialTheme.typography.bodySmall, color = SoulMatchTokens.Muted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SoulMatchTokens.Bg,
                    navigationIconContentColor = SoulMatchTokens.Tangerine,
                    titleContentColor = SoulMatchTokens.Text
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showInfo = true }) {
                        Icon(Icons.Filled.Info, contentDescription = "Info")
                    }
                }
            )
        }
    ) { padding ->
        PremiumScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp), containerColor = SoulMatchTokens.TangerineSoft) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            LabeledProgress(
                                label = if (isSectionEdit) "Section edit" else "Profile readiness",
                                value = profileSectionCompletionPercent(profile, currentStep),
                                detail = if (isSectionEdit) {
                                    "Save this section to return to My Profile. Other completed sections remain marked complete."
                                } else {
                                    "Complete all required fields in this section to keep your profile eligible for recommendations."
                                }
                            )
                            StepRail(currentStep = currentStep, profile = profile)
                            Text(copy.subtitle, style = MaterialTheme.typography.bodyMedium, color = SoulMatchTokens.Tangerine)
                            Text(copy.helper, style = MaterialTheme.typography.bodySmall, color = SoulMatchTokens.Muted)
                        }
                    }
                    if (!loadMessage.isNullOrBlank()) {
                        PremiumCard(modifier = Modifier.padding(horizontal = 16.dp), containerColor = MaterialTheme.colorScheme.errorContainer) {
                            Text(loadMessage ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (currentStep) {
                            1 -> Step1BasicInfo(profile, vm) { isCurrentStepValid = it }
                            2 -> Step2ReligiousCommunity(profile, vm) { isCurrentStepValid = it }
                            3 -> Step3Education(profile, vm) { isCurrentStepValid = it }
                            4 -> Step4Family(profile, vm) { isCurrentStepValid = it }
                            5 -> Step5Lifestyle(profile, vm) { isCurrentStepValid = it }
                            6 -> Step6PartnerPreferences(profile, vm) { isCurrentStepValid = it }
                        }
                        if (!errorMessage.isNullOrBlank()) {
                            PremiumCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                                Text(errorMessage ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.weight(1f).height(54.dp),
                            shape = RoundedCornerShape(SoulMatchTokens.PillRadius),
                            border = BorderStroke(1.dp, SoulMatchTokens.Border),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SoulMatchTokens.Text)
                        ) {
                            Text("Back")
                        }
                    }
                    Button(
                        onClick = {
                            vm.clearError()
                            vm.saveStep(currentStep) { onNextStep(currentStep + 1) }
                        },
                        modifier = Modifier.weight(1f).height(54.dp),
                        enabled = isCurrentStepValid && !isSaving,
                        shape = RoundedCornerShape(SoulMatchTokens.PillRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoulMatchTokens.Tangerine,
                            contentColor = androidx.compose.ui.graphics.Color.White,
                            disabledContainerColor = SoulMatchTokens.GoldSoft,
                            disabledContentColor = androidx.compose.ui.graphics.Color.White
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(
                                when {
                                    isSectionEdit -> "Save section"
                                    currentStep == 6 -> "Continue"
                                    else -> "Continue"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepRail(currentStep: Int, profile: ProfileData?) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        (1..6).forEach { index ->
            val complete = isProfileSectionComplete(profile, index)
            val active = index == currentStep
            Surface(
                modifier = Modifier.weight(1f).height(38.dp),
                shape = RoundedCornerShape(16.dp),
                color = when {
                    active && complete -> SoulMatchTokens.TangerineSoft
                    active -> SoulMatchTokens.TangerineSoft
                    complete -> SoulMatchTokens.Ivory
                    else -> SoulMatchTokens.Card
                },
                border = BorderStroke(
                    1.dp,
                    when {
                        active && complete -> SoulMatchTokens.Tangerine
                        active -> SoulMatchTokens.Tangerine
                        else -> SoulMatchTokens.Border
                    }
                )
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    if (complete) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (active) SoulMatchTokens.Tangerine else Success
                        )
                    } else {
                        Text(
                            index.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) SoulMatchTokens.Tangerine else SoulMatchTokens.Muted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun profileSectionCompletionPercent(profile: ProfileData?, currentStep: Int): Int {
    val completedSections = (1..6).count { isProfileSectionComplete(profile, it) }
    val currentSectionBoost = if (profile != null && !isProfileSectionComplete(profile, currentStep)) 1 else 0
    return ((completedSections + currentSectionBoost) * 100 / 6).coerceIn(0, 100)
}

private fun isProfileSectionComplete(profile: ProfileData?, section: Int): Boolean {
    val resolved = profile ?: return false
    return when (section) {
        1 -> listOf(
            resolved.firstName,
            resolved.lastName,
            resolved.dob.orEmpty(),
            resolved.gender
        ).all { it.isNotBlank() }
        2 -> resolved.religion.isNotBlank() &&
            resolved.caste.isNotBlank() &&
            resolved.motherTongue.isNotBlank() &&
            resolved.maritalStatus.isNotBlank()
        3 -> (resolved.noEducation || resolved.educationLevel.isNotBlank()) &&
            (!resolved.isEmployed || (
                resolved.occupation.isNotBlank() &&
                    resolved.annualIncome.isNotBlank() &&
                    resolved.workingCity.isNotBlank() &&
                    resolved.workingState.isNotBlank() &&
                    resolved.workingPincode.length == 6
                ))
        4 -> resolved.fatherOccupation.isNotBlank() &&
            resolved.motherOccupation.isNotBlank() &&
            resolved.numBrothers != null &&
            resolved.numSisters != null &&
            resolved.familyType.isNotBlank() &&
            resolved.familyCity.isNotBlank()
        5 -> resolved.diet.isNotBlank() &&
            resolved.smoking.isNotBlank() &&
            resolved.drinking.isNotBlank() &&
            resolved.aboutMe.trim().length >= 30
        6 -> resolved.isPartnerPrefSet
        else -> false
    }
}

@Composable
private fun Step1BasicInfo(existing: ProfileData?, vm: ProfileViewModel, onValidityChange: (Boolean) -> Unit) {
    var firstName by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.firstName.orEmpty()) }
    var lastName by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.lastName.orEmpty()) }
    var dob by rememberSaveable(existing?.profileId) { mutableStateOf(sanitizeDobForDisplay(existing?.dob).orEmpty()) }
    var gender by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.gender.orEmpty()) }
    val normalizedDob = normalizeDateOfBirth(dob)
    val dobHasError = dob.isNotBlank() && normalizedDob == null
    val firstNameError = firstName.isNotBlank() && firstName.trim().length < 2
    val lastNameError = lastName.isNotBlank() && lastName.trim().length < 2

    val isValid = !firstNameError &&
        !lastNameError &&
        listOf(firstName, lastName, gender).all { it.isNotBlank() } &&
        normalizedDob != null
    LaunchedEffect(firstName, lastName, normalizedDob, dobHasError, gender) {
        vm.updateStep1Data(
            mapOf(
                "firstName" to firstName,
                "lastName" to lastName,
                "dob" to (normalizedDob ?: dob.trim()),
                "gender" to gender
            )
        )
        onValidityChange(isValid)
    }

    PremiumCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionLead("Basic details", "These details help families understand identity, background, and location.")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RequiredTextField(
                    firstName,
                    { firstName = it },
                    "First name",
                    Modifier.weight(1f),
                    isError = firstNameError,
                    supportingText = if (firstNameError) "Enter at least 2 characters." else null
                )
                RequiredTextField(
                    lastName,
                    { lastName = it },
                    "Last name",
                    Modifier.weight(1f),
                    isError = lastNameError,
                    supportingText = if (lastNameError) "Enter at least 2 characters." else null
                )
            }
            DatePickerField(
                dob,
                onValueChange = { dob = it },
                "Date of birth",
                isError = dobHasError,
                supportingText = if (dobHasError) "Use DD-MM-YYYY. Age must be between 18 and 80 years." else "Format: DD-MM-YYYY. Age must be 18 or above."
            )
            SelectionField(label = "Gender", value = gender.titleCase(), options = listOf("Male", "Female"), onSelect = { gender = it.lowercase() })
        }
    }
}

private fun heightFeetFromCm(heightCm: Int?): String {
    if (heightCm == null || heightCm <= 0) return ""
    val totalInches = (heightCm / 2.54).roundToInt()
    return (totalInches / 12).toString()
}

private fun heightInchesFromCm(heightCm: Int?): String {
    if (heightCm == null || heightCm <= 0) return ""
    val totalInches = (heightCm / 2.54).roundToInt()
    return (totalInches % 12).toString()
}

private fun heightCmFromFeetInches(feet: String, inches: String): Int? {
    val feetValue = feet.toIntOrNull() ?: return null
    val inchesValue = inches.toIntOrNull() ?: return null
    return ((feetValue * 12 + inchesValue) * 2.54).roundToInt()
}

@Composable
private fun Step2ReligiousCommunity(existing: ProfileData?, vm: ProfileViewModel, onValidityChange: (Boolean) -> Unit) {
    var religion by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.religion.orEmpty()) }
    var caste by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.caste.orEmpty()) }
    var motherTongue by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.motherTongue.orEmpty()) }
    var maritalStatus by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.maritalStatus.orEmpty()) }

    val isValid = listOf(religion, caste, motherTongue, maritalStatus).all { it.isNotBlank() }
    LaunchedEffect(religion, caste, motherTongue, maritalStatus) {
        vm.updateStep2Data(
            mapOf(
                "religion" to religion,
                "caste" to caste,
                "motherTongue" to motherTongue,
                "maritalStatus" to maritalStatus
            )
        )
        onValidityChange(isValid)
    }

    PremiumCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionLead("Religious and community", "Keep this respectful and accurate for family-led matching.")
            SelectionField(label = "Religion", value = religion, options = listOf("Hindu", "Muslim", "Christian", "Sikh", "Jain", "Buddhist"), onSelect = { religion = it })
            RequiredTextField(caste, { caste = it }, "Community / caste")
            SelectionField(label = "Mother tongue", value = motherTongue, options = listOf("Hindi", "English", "Telugu", "Tamil", "Malayalam", "Kannada", "Gujarati", "Bengali", "Marathi"), onSelect = { motherTongue = it })
            SelectionField(label = "Marital status", value = maritalStatus.replace('_', ' ').titleCase(), options = listOf("Never married", "Divorced", "Widowed"), onSelect = {
                maritalStatus = it.lowercase().replace(' ', '_')
            })
            SignalChips(listOf("Used in family search", "Used in partner preference matching"), tone = ChipTone.Info)
        }
    }
}

@Composable
private fun Step3Education(existing: ProfileData?, vm: ProfileViewModel, onValidityChange: (Boolean) -> Unit) {
    var educationLevel by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.educationLevel.orEmpty()) }
    var noEducation by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.noEducation ?: false) }
    var isEmployed by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.isEmployed ?: false) }
    var occupation by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.occupation.orEmpty()) }
    var annualIncome by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.annualIncome.orEmpty()) }
    var workingCity by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.workingCity.orEmpty()) }
    var workingState by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.workingState.orEmpty()) }
    var workingPincode by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.workingPincode.orEmpty()) }

    val workingPincodeError = workingPincode.isNotBlank() && workingPincode.length != 6
    val isValid = (noEducation || educationLevel.isNotBlank()) &&
        (
            !isEmployed ||
                (
                    occupation.isNotBlank() &&
                        annualIncome.isNotBlank() &&
                        workingCity.isNotBlank() &&
                        workingState.isNotBlank() &&
                        workingPincode.length == 6
                    )
            )
    
    LaunchedEffect(noEducation, educationLevel, isEmployed, occupation, annualIncome, workingCity, workingState, workingPincode) {
        vm.updateStep3Data(
            mapOf(
                "educationLevel" to if (noEducation) "" else educationLevel,
                "noEducation" to noEducation,
                "isEmployed" to isEmployed,
                "occupation" to (if (isEmployed) occupation else ""),
                "annualIncome" to (if (isEmployed) annualIncome else ""),
                "workingCity" to (if (isEmployed) workingCity else ""),
                "workingState" to (if (isEmployed) workingState else ""),
                "workingPincode" to (if (isEmployed) workingPincode else "")
            )
        )
        onValidityChange(isValid)
    }

    PremiumCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionLead("Professional snapshot", "These are among the highest-intent search filters in a matrimony app.")
            ChipRow("Education level", listOf("No Education", "Graduate", "Post Graduate", "Doctorate", "MBA", "Professional"), if (noEducation) "No Education" else educationLevel) { selected ->
                noEducation = selected == "No Education"
                educationLevel = if (noEducation) "" else selected
            }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SoulMatchTokens.Ivory,
                border = BorderStroke(1.dp, SoulMatchTokens.Border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Currently employed", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Turn this on to capture work details for search and shortlist quality.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SoulMatchTokens.Muted,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(12.dp))
                        Switch(checked = isEmployed, onCheckedChange = { isEmployed = it })
                    }
                }
            }
            if (isEmployed) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, SoulMatchTokens.Border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        RequiredTextField(occupation, { occupation = it }, "Occupation")
                        ChipRow("Annual income", listOf("< 3 LPA", "3-5 LPA", "5-10 LPA", "10-20 LPA", "20+ LPA"), annualIncome) { annualIncome = it }
                        RequiredTextField(workingCity, { workingCity = it }, "Working city")
                        SelectionField(
                            label = "Working state",
                            value = workingState,
                            options = indianStates,
                            onSelect = { workingState = it }
                        )
                        NumberField(
                            workingPincode,
                            { workingPincode = it.filter(Char::isDigit).take(6) },
                            "Pincode",
                            isError = workingPincodeError,
                            supportingText = if (workingPincodeError) "Enter a valid 6-digit pincode." else null
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SoulMatchTokens.Ivory,
                    border = BorderStroke(1.dp, SoulMatchTokens.Border)
                ) {
                    Text(
                        "Work details stay hidden until you switch on employment. Education alone is enough to complete this section for non-working members.",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = SoulMatchTokens.Muted
                    )
                }
            }
            PremiumCard(containerColor = SoulMatchTokens.TangerineSoft, contentPadding = PaddingValues(14.dp)) {
                Text("This section syncs with Discover and ranking, so it should be specific instead of generic.", style = MaterialTheme.typography.bodySmall, color = SoulMatchTokens.Tangerine)
            }
        }
    }
}

@Composable
private fun Step4Family(existing: ProfileData?, vm: ProfileViewModel, onValidityChange: (Boolean) -> Unit) {
    var fatherOccupation by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.fatherOccupation.orEmpty()) }
    var motherOccupation by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.motherOccupation.orEmpty()) }
    var numBrothers by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.numBrothers?.toString().orEmpty()) }
    var numSisters by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.numSisters?.toString().orEmpty()) }
    var familyType by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.familyType.orEmpty()) }
    var familyCity by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.familyCity.orEmpty()) }
    var familyState by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.familyState.orEmpty()) }
    var familyLocality by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.familyLocality.orEmpty()) }
    var familyPincode by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.familyPincode.orEmpty()) }
    val familyPincodeError = familyPincode.isNotBlank() && familyPincode.length != 6

    val isValid = listOf(fatherOccupation, motherOccupation, numBrothers, numSisters, familyType, familyCity).all { it.isNotBlank() }
    LaunchedEffect(fatherOccupation, motherOccupation, numBrothers, numSisters, familyType, familyCity, familyState, familyLocality, familyPincode) {
        vm.updateStep4Data(
            mapOf(
                "fatherOccupation" to fatherOccupation,
                "motherOccupation" to motherOccupation,
                "numBrothers" to numBrothers.toIntOrNull().orZero(),
                "numSisters" to numSisters.toIntOrNull().orZero(),
                "familyType" to familyType,
                "familyCity" to familyCity,
                "familyState" to familyState,
                "familyLocality" to familyLocality,
                "familyPincode" to familyPincode
            )
        )
        onValidityChange(isValid)
    }

    PremiumCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionLead("Family context", "This is where SoulMatch should feel like a serious matrimonial product.")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RequiredTextField(fatherOccupation, { fatherOccupation = it }, "Father occupation", Modifier.weight(1f))
                RequiredTextField(motherOccupation, { motherOccupation = it }, "Mother occupation", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(numBrothers, { numBrothers = it.filter(Char::isDigit) }, "Brothers", Modifier.weight(1f))
                NumberField(numSisters, { numSisters = it.filter(Char::isDigit) }, "Sisters", Modifier.weight(1f))
            }
            ChipRow("Family type", listOf("Nuclear", "Joint"), familyType) { familyType = it }
            RequiredTextField(familyCity, { familyCity = it }, "Family city")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SelectionField(
                    label = "State",
                    value = familyState,
                    options = indianStates,
                    onSelect = { familyState = it },
                    modifier = Modifier.weight(1f)
                )
                NumberField(
                    familyPincode,
                    { familyPincode = it.filter(Char::isDigit).take(6) },
                    "Pincode",
                    Modifier.weight(1f),
                    isError = familyPincodeError,
                    supportingText = if (familyPincodeError) "Enter a valid 6-digit pincode." else null
                )
            }
            RequiredTextField(familyLocality, { familyLocality = it }, "Locality / area")
            SignalChip("Families can scan this section quickly", tone = ChipTone.Gold)
        }
    }
}

@Composable
private fun Step5Lifestyle(existing: ProfileData?, vm: ProfileViewModel, onValidityChange: (Boolean) -> Unit) {
    var diet by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.diet.orEmpty()) }
    var smoking by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.smoking?.ifBlank { "never" } ?: "never") }
    var drinking by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.drinking?.ifBlank { "never" } ?: "never") }
    var aboutMe by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.aboutMe.orEmpty()) }
    val bioSuggestions by vm.bioSuggestions.collectAsStateWithLifecycle()
    val isGeneratingBio by vm.isGeneratingBioSuggestions.collectAsStateWithLifecycle()

    val isValid = diet.isNotBlank() && smoking.isNotBlank() && drinking.isNotBlank() && aboutMe.trim().length >= 30
    LaunchedEffect(diet, smoking, drinking, aboutMe) {
        vm.updateStep5Data(
            mapOf(
                "diet" to diet,
                "smoking" to smoking,
                "drinking" to drinking,
                "aboutMe" to aboutMe
            )
        )
        onValidityChange(isValid)
    }

    PremiumCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionLead("Lifestyle snapshot", "A warm, specific profile usually gets better response quality.")
            ChipRow("Diet", listOf("vegetarian", "jain", "eggetarian", "non_vegetarian"), diet) { diet = it }
            ChipRow("Smoking", listOf("never", "occasionally"), smoking) { smoking = it }
            ChipRow("Drinking", listOf("never", "socially", "occasionally"), drinking) { drinking = it }
            OutlinedTextField(
                value = aboutMe,
                onValueChange = { aboutMe = it },
                label = { Text("About me *") },
                supportingText = { Text("${aboutMe.trim().length}/30 minimum") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5
            )
            OutlinedButton(
                onClick = { vm.requestBioSuggestions(aboutMe) },
                enabled = !isGeneratingBio,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isGeneratingBio) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(8.dp))
                }
                Text(if (isGeneratingBio) "Preparing suggestions" else "Suggest better profile intro")
            }
            bioSuggestions.forEach { suggestion ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            aboutMe = suggestion
                            vm.clearBioSuggestions()
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = SoulMatchTokens.Ivory,
                    border = BorderStroke(1.dp, SoulMatchTokens.Border)
                ) {
                    Text(
                        suggestion,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoulMatchTokens.Tangerine
                    )
                }
            }
            PremiumCard(containerColor = SoulMatchTokens.TangerineSoft, contentPadding = PaddingValues(14.dp)) {
                Text("Prompt idea: mention your family rhythm, future city flexibility, hobbies, and what kind of partnership you want.", style = MaterialTheme.typography.bodySmall, color = SoulMatchTokens.Tangerine)
            }
        }
    }
}

@Composable
private fun Step6PartnerPreferences(existing: ProfileData?, vm: ProfileViewModel, onValidityChange: (Boolean) -> Unit) {
    val existingPreferences by vm.partnerPreferences.collectAsStateWithLifecycle()
    var ageMin by rememberSaveable(existing?.profileId) { mutableStateOf(existingPreferences.ageMin.toString()) }
    var ageMax by rememberSaveable(existing?.profileId) { mutableStateOf(existingPreferences.ageMax.toString()) }
    var religion by rememberSaveable(existing?.profileId) { mutableStateOf(existingPreferences.religion.orEmpty()) }
    var education by rememberSaveable(existing?.profileId) { mutableStateOf(existingPreferences.educationLevels.joinToString(", ")) }
    var occupations by rememberSaveable(existing?.profileId) { mutableStateOf(existingPreferences.occupations.joinToString(", ")) }
    var locations by rememberSaveable(existing?.profileId) { mutableStateOf(existingPreferences.locations.joinToString(", ")) }
    var diet by rememberSaveable(existing?.profileId) { mutableStateOf(existingPreferences.dietPrefs.joinToString(", ")) }
    var timeline by rememberSaveable(existing?.profileId) { mutableStateOf(existingPreferences.timeline.orEmpty()) }

    val isValid = ageMin.toIntOrNull() != null &&
        ageMax.toIntOrNull() != null &&
        ageMin.toInt() < ageMax.toInt() &&
        locations.trim().isNotBlank()

    LaunchedEffect(ageMin, ageMax, religion, education, occupations, locations, diet, timeline) {
        vm.updatePartnerPreferences(
            PartnerPreferencesData(
                ageMin = ageMin.toIntOrNull() ?: 24,
                ageMax = ageMax.toIntOrNull() ?: 32,
                religion = religion.trim().ifBlank { null },
                educationLevels = education.split(",").map { it.trim() }.filter { it.isNotBlank() },
                occupations = occupations.split(",").map { it.trim() }.filter { it.isNotBlank() },
                locations = locations.split(",").map { it.trim() }.filter { it.isNotBlank() },
                dietPrefs = diet.split(",").map { it.trim() }.filter { it.isNotBlank() },
                timeline = timeline.trim().ifBlank { null },
                maritalStatuses = listOfNotNull(existing?.maritalStatus?.takeIf { it.isNotBlank() }),
                familyTypes = listOfNotNull(existing?.familyType?.takeIf { it.isNotBlank() })
            )
        )
        onValidityChange(isValid)
    }

    PremiumCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionLead("Partner preferences", "Match recommendations use these values first.")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(ageMin, { ageMin = it.filter(Char::isDigit).take(2) }, "Age min", Modifier.weight(1f))
                NumberField(ageMax, { ageMax = it.filter(Char::isDigit).take(2) }, "Age max", Modifier.weight(1f))
            }
            RequiredTextField(religion, { religion = it }, "Religion", supportingText = "Leave blank if open to all")
            RequiredTextField(education, { education = it }, "Education", supportingText = "Comma separated")
            RequiredTextField(occupations, { occupations = it }, "Occupation", supportingText = "Comma separated")
            RequiredTextField(locations, { locations = it }, "Preferred locations", supportingText = "City names separated by comma")
            RequiredTextField(diet, { diet = it }, "Diet preference", supportingText = "Example: vegetarian, eggetarian")
            RequiredTextField(timeline, { timeline = it }, "Marriage timeline", supportingText = "Example: within 1 year")
            SignalChips(listOf("Backend linked", "Drives recommendations", "Used in shortlist filtering"), tone = ChipTone.Info)
        }
    }
}

@Composable
private fun SectionLead(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SoulMatchTokens.Text)
        Text(description, style = MaterialTheme.typography.bodySmall, color = SoulMatchTokens.Muted)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(title: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$title *", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = SoulMatchTokens.Text)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selected.replace('_', ' ').titleCase(),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = RoundedCornerShape(SoulMatchTokens.CardRadius),
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.replace('_', ' ').titleCase()) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RequiredTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("$label *") },
        isError = isError,
        supportingText = supportingText?.let { message -> { Text(message) } },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(SoulMatchTokens.CardRadius),
        singleLine = true
    )
}

@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    NumberField(value, onValueChange, label, modifier, false, null)
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("$label *") },
        isError = isError,
        supportingText = supportingText?.let { message -> { Text(message) } },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(SoulMatchTokens.CardRadius),
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionField(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text("$label *") },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(SoulMatchTokens.CardRadius),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun Int?.orZero(): Int = this ?: 0

private fun normalizeDateOfBirth(value: String): String? {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return null
    val normalizedSeparator = trimmed.replace('/', '-')
    val dob = try {
        LocalDate.parse(normalizedSeparator, DateTimeFormatter.ofPattern("dd-MM-uuuu"))
    } catch (_: DateTimeParseException) {
        return null
    }
    val today = LocalDate.now()
    val oldestAllowed = today.minusYears(80)
    val youngestAllowed = today.minusYears(18)
    if (dob.isBefore(oldestAllowed) || dob.isAfter(youngestAllowed)) return null
    return dob.format(DateTimeFormatter.ofPattern("dd-MM-uuuu"))
}

private fun sanitizeDobForDisplay(value: String?): String? {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isBlank()) return null
    normalizeDateOfBirth(trimmed)?.let { return it }
    val rawDate = trimmed.substringBefore('T')
    normalizeDateOfBirth(rawDate)?.let { return it }
    return if (rawDate.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
        val parts = rawDate.split('-')
        "${parts[2]}-${parts[1]}-${parts[0]}"
    } else {
        trimmed
    }
}

private fun formatDateInput(value: String): String {
    val digits = value.filter(Char::isDigit).take(8)
    return buildString {
        digits.forEachIndexed { index, c ->
            append(c)
            if ((index == 1 || index == 3) && index != digits.lastIndex) append('-')
        }
    }
}

@Composable
private fun DatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false,
    supportingText: String? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val calendarValue = remember(value) {
        sanitizeDobForDisplay(value)?.let {
            runCatching { LocalDate.parse(it, DateTimeFormatter.ofPattern("dd-MM-uuuu")) }.getOrNull()
        }
    } ?: LocalDate.of(1995, 1, 1)
    val picker = remember(value) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onValueChange(
                    LocalDate.of(year, month + 1, dayOfMonth)
                        .format(DateTimeFormatter.ofPattern("dd-MM-uuuu"))
                )
            },
            calendarValue.year,
            calendarValue.monthValue - 1,
            calendarValue.dayOfMonth
        ).apply {
            datePicker.maxDate = Instant.now().toEpochMilli()
            datePicker.minDate = LocalDate.now().minusYears(80).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    }
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text("$label *") },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { picker.show() },
        readOnly = true,
        isError = isError,
        supportingText = supportingText?.let { message -> { Text(message) } },
        shape = RoundedCornerShape(SoulMatchTokens.CardRadius),
        trailingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SoulMatchTokens.Tangerine) }
    )
}


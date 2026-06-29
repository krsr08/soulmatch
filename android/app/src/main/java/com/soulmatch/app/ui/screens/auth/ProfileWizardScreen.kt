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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
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
        eyebrow = "Step 1 of 10",
        subtitle = "Start with identity basics first.",
        helper = "Full name, gender, date of birth, height, marital status, mother tongue, and current city are required.",
        info = "Basic details help SoulMatch create your member identity and decide what screen comes next."
    ),
    2 to WizardStepCopy(
        title = "Religious and community",
        eyebrow = "Step 2 of 10",
        subtitle = "Keep this respectful and accurate for family-led matching.",
        helper = "Religion and community details help families filter and review profiles quickly.",
        info = "These details help family filtering, search relevance, and partner preference matching."
    ),
    3 to WizardStepCopy(
        title = "Education and career",
        eyebrow = "Step 3 of 10",
        subtitle = "Add professional context that supports serious introductions.",
        helper = "Education, occupation, and annual income are required.",
        info = "Education and work details directly affect search, ranking, and shortlist quality."
    ),
    4 to WizardStepCopy(
        title = "Family details",
        eyebrow = "Step 4 of 10",
        subtitle = "Share the family context most parents look for first.",
        helper = "Parent occupations and family type are required.",
        info = "Family details help parents review profile fit faster and with more trust."
    ),
    5 to WizardStepCopy(
        title = "Lifestyle details",
        eyebrow = "Step 5 of 10",
        subtitle = "Help matches understand daily habits and communication style.",
        helper = "Diet, smoking, and drinking details are required.",
        info = "Lifestyle details give families and matches a clearer idea of daily compatibility."
    ),
    6 to WizardStepCopy(
        title = "Partner preferences",
        eyebrow = "Step 6 of 10",
        subtitle = "Set the match basics you care about most before profile review.",
        helper = "Height range, religion/community, education, and occupation preferences power match quality.",
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

private val profileHeightOptions = buildList {
    for (feet in 4..7) {
        for (inches in 0..11) {
            add("${feet}'${inches}\"")
        }
    }
}

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
        ProfileWizardInfoBottomSheet(
            title = "Why complete your profile?",
            body = copy.info,
            onDismiss = { showInfo = false }
        )
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
            resolved.dob.orEmpty(),
            resolved.gender,
            resolved.motherTongue,
            resolved.maritalStatus,
            resolved.workingCity
        ).all { it.isNotBlank() } &&
            resolved.lastName.isNotBlank() &&
            resolved.heightCm != null
        2 -> resolved.religion.isNotBlank() && resolved.caste.isNotBlank()
        3 -> resolved.educationLevel.isNotBlank() &&
            resolved.occupation.isNotBlank() &&
            resolved.annualIncome.isNotBlank()
        4 -> resolved.fatherOccupation.isNotBlank() &&
            resolved.motherOccupation.isNotBlank() &&
            resolved.familyType.isNotBlank()
        5 -> resolved.diet.isNotBlank() &&
            resolved.smoking.isNotBlank() &&
            resolved.drinking.isNotBlank()
        6 -> resolved.isPartnerPrefSet
        else -> false
    }
}

private fun splitFullName(value: String): Pair<String, String> {
    val tokens = value.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (tokens.isEmpty()) return "" to ""
    if (tokens.size == 1) return tokens.first() to ""
    return tokens.first() to tokens.drop(1).joinToString(" ")
}

private fun heightLabelFromCm(heightCm: Int?): String {
    if (heightCm == null || heightCm <= 0) return ""
    val totalInches = (heightCm / 2.54).roundToInt()
    val feet = totalInches / 12
    val inches = totalInches % 12
    return "${feet}'${inches}\""
}

private fun heightCmFromLabel(value: String): Int? {
    val match = Regex("""(\d+)'\s*(\d+)\"""").matchEntire(value.trim()) ?: return null
    val feet = match.groupValues[1].toIntOrNull() ?: return null
    val inches = match.groupValues[2].toIntOrNull() ?: return null
    return ((feet * 12 + inches) * 2.54).roundToInt()
}

@Composable
private fun Step1BasicInfo(existing: ProfileData?, vm: ProfileViewModel, onValidityChange: (Boolean) -> Unit) {
    var fullName by rememberSaveable(existing?.profileId) { mutableStateOf(listOf(existing?.firstName, existing?.lastName).filterNotNull().filter { it.isNotBlank() }.joinToString(" ")) }
    var dob by rememberSaveable(existing?.profileId) { mutableStateOf(sanitizeDobForDisplay(existing?.dob).orEmpty()) }
    var gender by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.gender.orEmpty()) }
    var heightLabel by rememberSaveable(existing?.profileId) { mutableStateOf(heightLabelFromCm(existing?.heightCm)) }
    var maritalStatus by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.maritalStatus.orEmpty()) }
    var motherTongue by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.motherTongue.orEmpty()) }
    var currentCity by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.workingCity.orEmpty()) }
    val normalizedDob = normalizeDateOfBirth(dob)
    val dobHasError = dob.isNotBlank() && normalizedDob == null
    val (firstName, lastName) = splitFullName(fullName)
    val fullNameError = fullName.isNotBlank() && (firstName.trim().length < 2 || lastName.trim().length < 2)
    val heightCm = heightCmFromLabel(heightLabel)

    val isValid = !fullNameError &&
        normalizedDob != null &&
        heightCm != null &&
        listOf(firstName, lastName, gender, maritalStatus, motherTongue, currentCity).all { it.isNotBlank() }
    LaunchedEffect(fullName, normalizedDob, dobHasError, gender, heightCm, maritalStatus, motherTongue, currentCity) {
        vm.updateStep1Data(
            mapOf(
                "firstName" to firstName,
                "lastName" to lastName,
                "dob" to (normalizedDob ?: dob.trim()),
                "gender" to gender,
                "heightCm" to (heightCm ?: 0),
                "maritalStatus" to maritalStatus,
                "motherTongue" to motherTongue,
                "workingCity" to currentCity
            )
        )
        onValidityChange(isValid)
    }

    PremiumCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionLead("Basic details", "These details help families understand identity, background, and location.")
            RequiredTextField(
                fullName,
                { fullName = it },
                "Full name",
                isError = fullNameError,
                supportingText = if (fullNameError) "Enter first and last name." else null
            )
            SelectionField(label = "Gender", value = titleCase(gender), options = listOf("Male", "Female"), onSelect = { gender = it.lowercase() })
            DatePickerField(
                dob,
                onValueChange = { dob = it },
                "Date of birth",
                isError = dobHasError,
                supportingText = if (dobHasError) "Use DD-MM-YYYY. Age must be between 18 and 80 years." else "Format: DD-MM-YYYY. Age must be 18 or above."
            )
            SelectionField(label = "Height", value = heightLabel, options = profileHeightOptions, onSelect = { heightLabel = it })
            SelectionField(
                label = "Marital status",
                value = titleCase(maritalStatus.replace('_', ' ')),
                options = listOf("Never married", "Divorced", "Widowed"),
                onSelect = { maritalStatus = it.lowercase().replace(' ', '_') }
            )
            SelectionField(
                label = "Mother tongue",
                value = motherTongue,
                options = listOf("Hindi", "English", "Telugu", "Tamil", "Malayalam", "Kannada", "Gujarati", "Bengali", "Marathi"),
                onSelect = { motherTongue = it }
            )
            RequiredTextField(currentCity, { currentCity = it }, "Current city")
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

    val isValid = religion.isNotBlank() && caste.isNotBlank()
    LaunchedEffect(religion, caste) {
        vm.updateStep2Data(
            mapOf(
                "religion" to religion,
                "caste" to caste
            )
        )
        onValidityChange(isValid)
    }

    PremiumCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionLead("Religious and community", "Keep this respectful and accurate for family-led matching.")
            SelectionField(label = "Religion", value = religion, options = listOf("Hindu", "Muslim", "Christian", "Sikh", "Jain", "Buddhist"), onSelect = { religion = it })
            RequiredTextField(caste, { caste = it }, "Community / caste")
            SignalChips(listOf("Used in family search", "Used in partner preference matching"), tone = ChipTone.Info)
        }
    }
}

@Composable
private fun Step3Education(existing: ProfileData?, vm: ProfileViewModel, onValidityChange: (Boolean) -> Unit) {
    var educationLevel by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.educationLevel.orEmpty()) }
    var occupation by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.occupation.orEmpty()) }
    var annualIncome by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.annualIncome.orEmpty()) }
    val workingCity = existing?.workingCity.orEmpty()
    val workingState = existing?.workingState.orEmpty()
    val workingPincode = existing?.workingPincode.orEmpty()

    val isValid = educationLevel.isNotBlank() &&
        occupation.isNotBlank() &&
        annualIncome.isNotBlank()

    LaunchedEffect(educationLevel, occupation, annualIncome, workingCity, workingState, workingPincode) {
        vm.updateStep3Data(
            mapOf(
                "educationLevel" to educationLevel,
                "occupation" to occupation,
                "annualIncome" to annualIncome,
                "workingCity" to workingCity,
                "workingState" to workingState,
                "workingPincode" to workingPincode
            )
        )
        onValidityChange(isValid)
    }

    PremiumCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionLead("Education and career", "These details shape how serious recommendations and family shortlists feel.")
            SelectionField(
                label = "Highest qualification",
                value = educationLevel,
                options = listOf("High School", "Diploma", "Graduate", "Post Graduate", "MBA", "Doctorate", "Professional"),
                onSelect = { educationLevel = it }
            )
            RequiredTextField(occupation, { occupation = it }, "Occupation")
            SelectionField(
                label = "Annual income",
                value = annualIncome,
                options = listOf("< 3 LPA", "3-5 LPA", "5-10 LPA", "10-20 LPA", "20-35 LPA", "35+ LPA"),
                onSelect = { annualIncome = it }
            )
            PremiumCard(containerColor = SoulMatchTokens.TangerineSoft, contentPadding = PaddingValues(14.dp)) {
                Text("Current city stays linked from the first step and continues into matching.", style = MaterialTheme.typography.bodySmall, color = SoulMatchTokens.Tangerine)
            }
        }
    }
}

@Composable
private fun Step4Family(existing: ProfileData?, vm: ProfileViewModel, onValidityChange: (Boolean) -> Unit) {
    var fatherOccupation by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.fatherOccupation.orEmpty()) }
    var motherOccupation by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.motherOccupation.orEmpty()) }
    var familyType by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.familyType.orEmpty()) }
    val familyCity = existing?.familyCity.orEmpty()
    val familyState = existing?.familyState.orEmpty()
    val familyLocality = existing?.familyLocality.orEmpty()
    val familyPincode = existing?.familyPincode.orEmpty()

    val isValid = fatherOccupation.isNotBlank() &&
        motherOccupation.isNotBlank() &&
        familyType.isNotBlank()
    LaunchedEffect(fatherOccupation, motherOccupation, familyType, familyCity, familyState, familyLocality, familyPincode) {
        vm.updateStep4Data(
            mapOf(
                "fatherOccupation" to fatherOccupation,
                "motherOccupation" to motherOccupation,
                "numBrothers" to (existing?.numBrothers ?: 0),
                "numSisters" to (existing?.numSisters ?: 0),
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
            SectionLead("Family details", "Parents usually scan this block fast, so clarity matters more than length.")
            SelectionField(
                label = "Family type",
                value = familyType,
                options = listOf("Nuclear", "Joint"),
                onSelect = { familyType = it }
            )
            RequiredTextField(fatherOccupation, { fatherOccupation = it }, "Father occupation")
            RequiredTextField(motherOccupation, { motherOccupation = it }, "Mother occupation")
            SignalChip("Families can scan this section quickly", tone = ChipTone.Gold)
        }
    }
}

@Composable
private fun Step5Lifestyle(existing: ProfileData?, vm: ProfileViewModel, onValidityChange: (Boolean) -> Unit) {
    var diet by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.diet.orEmpty()) }
    var smoking by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.smoking?.ifBlank { "never" } ?: "never") }
    var drinking by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.drinking?.ifBlank { "never" } ?: "never") }
    val isValid = diet.isNotBlank() && smoking.isNotBlank() && drinking.isNotBlank()
    LaunchedEffect(diet, smoking, drinking) {
        vm.updateStep5Data(
            mapOf(
                "diet" to diet,
                "smoking" to smoking,
                "drinking" to drinking
            )
        )
        onValidityChange(isValid)
    }

    PremiumCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionLead("Lifestyle details", "Simple habit choices help families understand compatibility quickly.")
            ChipRow("Diet", listOf("vegetarian", "jain", "eggetarian", "non_vegetarian"), diet) { diet = it }
            ChipRow("Smoking", listOf("never", "occasionally"), smoking) { smoking = it }
            ChipRow("Drinking", listOf("never", "socially", "occasionally"), drinking) { drinking = it }
            PremiumCard(containerColor = SoulMatchTokens.TangerineSoft, contentPadding = PaddingValues(14.dp)) {
                Text("Profile summary text can be improved later. These three lifestyle choices are the required part here.", style = MaterialTheme.typography.bodySmall, color = SoulMatchTokens.Tangerine)
            }
        }
    }
}

@Composable
private fun Step6PartnerPreferences(existing: ProfileData?, vm: ProfileViewModel, onValidityChange: (Boolean) -> Unit) {
    val existingPreferences by vm.partnerPreferences.collectAsStateWithLifecycle()
    var heightMin by rememberSaveable(existing?.profileId) { mutableStateOf(heightLabelFromCm(existingPreferences.heightMinCm)) }
    var heightMax by rememberSaveable(existing?.profileId) { mutableStateOf(heightLabelFromCm(existingPreferences.heightMaxCm)) }
    var religionCommunity by rememberSaveable(existing?.profileId) { mutableStateOf(existingPreferences.religion.orEmpty()) }
    var education by rememberSaveable(existing?.profileId) { mutableStateOf(existingPreferences.educationLevels.firstOrNull().orEmpty()) }
    var occupation by rememberSaveable(existing?.profileId) { mutableStateOf(existingPreferences.occupations.firstOrNull().orEmpty()) }

    val heightMinCm = heightCmFromLabel(heightMin)
    val heightMaxCm = heightCmFromLabel(heightMax)
    val isValid = heightMinCm != null &&
        heightMaxCm != null &&
        heightMinCm <= heightMaxCm &&
        religionCommunity.isNotBlank() &&
        education.isNotBlank() &&
        occupation.isNotBlank()

    LaunchedEffect(heightMin, heightMax, religionCommunity, education, occupation) {
        vm.updatePartnerPreferences(
            PartnerPreferencesData(
                religion = religionCommunity.trim().ifBlank { null },
                educationLevels = listOf(education).filter { it.isNotBlank() },
                occupations = listOf(occupation).filter { it.isNotBlank() },
                heightMinCm = heightMinCm,
                heightMaxCm = heightMaxCm,
                locations = listOfNotNull(existing?.workingCity?.takeIf { it.isNotBlank() }),
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
                SelectionField(
                    label = "Height min",
                    value = heightMin,
                    options = profileHeightOptions,
                    onSelect = { heightMin = it },
                    modifier = Modifier.weight(1f)
                )
                SelectionField(
                    label = "Height max",
                    value = heightMax,
                    options = profileHeightOptions,
                    onSelect = { heightMax = it },
                    modifier = Modifier.weight(1f)
                )
            }
            if (heightMin.isNotBlank() && heightMax.isNotBlank() && heightMinCm != null && heightMaxCm != null && heightMinCm > heightMaxCm) {
                Text(
                    "Height range is not valid.",
                    color = SoulMatchTokens.Error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            RequiredTextField(religionCommunity, { religionCommunity = it }, "Religion / community")
            SelectionField(
                label = "Education preference",
                value = education,
                options = listOf("Any", "Graduate", "Post Graduate", "MBA", "Doctorate", "Professional"),
                onSelect = { education = it }
            )
            RequiredTextField(occupation, { occupation = it }, "Occupation preference")
            SignalChips(listOf("Backend linked", "Drives recommendations", "Used in shortlist filtering"), tone = ChipTone.Info)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ProfileWizardInfoBottomSheet(
    title: String,
    body: String,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = SoulMatchTokens.Ivory,
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = SoulMatchTokens.Tangerine,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 38.sp
                        ),
                        color = SoulMatchTokens.Text
                    )
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = SoulMatchTokens.Muted,
                        lineHeight = 30.sp
                    )
                }
            }
            WizardInfoBullet("Verified profiles receive better responses.")
            WizardInfoBullet("Privacy controls protect contact and photo access.")
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(SoulMatchTokens.PillRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoulMatchTokens.Tangerine,
                    contentColor = Color.White
                )
            ) {
                Text("Got it", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WizardInfoBullet(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = SoulMatchTokens.Tangerine,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = SoulMatchTokens.Text
        )
    }
}

@Composable
private fun SectionLead(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SoulMatchTokens.Text)
        Text(description, style = MaterialTheme.typography.bodySmall, color = SoulMatchTokens.Muted)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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
                value = titleCase(selected.replace('_', ' ')),
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
                        text = { Text(titleCase(option.replace('_', ' '))) },
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


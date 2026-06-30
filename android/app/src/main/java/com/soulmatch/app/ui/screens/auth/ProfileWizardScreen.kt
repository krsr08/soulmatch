package com.soulmatch.app.ui.screens.auth

import android.app.DatePickerDialog
import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.soulmatch.app.ui.components.premium.FilterChoiceChip
import com.soulmatch.app.ui.components.premium.PremiumCard
import com.soulmatch.app.ui.components.premium.PremiumScreen
import com.soulmatch.app.ui.design.SoulMatchHeaderIconButton
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
    val info: String,
    val infoBullets: List<String>
)

private val wizardCopy = mapOf(
    1 to WizardStepCopy(
        title = "Basic Details",
        eyebrow = "Step 1 of 10",
        subtitle = "Start with identity basics first.",
        helper = "Full name, gender, date of birth, height, marital status, mother tongue, and current city are required.",
        info = "Basic details help SoulMatch create your member identity and decide what screen comes next.",
        infoBullets = listOf(
            "Correct identity details help profile verification.",
            "Location and language improve relevant family matches."
        )
    ),
    2 to WizardStepCopy(
        title = "Religious And Community",
        eyebrow = "Step 2 of 10",
        subtitle = "Keep this respectful and accurate for family-led matching.",
        helper = "Religion and community details help families filter and review profiles quickly.",
        info = "These details help family filtering, search relevance, and partner preference matching.",
        infoBullets = listOf(
            "Family search often uses religion and community first.",
            "Accurate values improve shortlist quality and trust."
        )
    ),
    3 to WizardStepCopy(
        title = "Education And Career",
        eyebrow = "Step 3 of 10",
        subtitle = "Add professional context that supports serious introductions.",
        helper = "Education, occupation, and annual income are required.",
        info = "Education and work details directly affect search, ranking, and shortlist quality.",
        infoBullets = listOf(
            "Career details help families assess life-stage fit.",
            "Work and education improve recommendation quality."
        )
    ),
    4 to WizardStepCopy(
        title = "Family Details",
        eyebrow = "Step 4 of 10",
        subtitle = "Share the family context most parents look for first.",
        helper = "Parent occupations and family type are required.",
        info = "Family details help parents review profile fit faster and with more trust.",
        infoBullets = listOf(
            "Family context gives stronger trust to profile reviews.",
            "Clear family details reduce back-and-forth questions."
        )
    ),
    5 to WizardStepCopy(
        title = "Lifestyle Details",
        eyebrow = "Step 5 of 10",
        subtitle = "Help matches understand daily habits and communication style.",
        helper = "Diet, smoking, and drinking details are required.",
        info = "Lifestyle details give families and matches a clearer idea of daily compatibility.",
        infoBullets = listOf(
            "Daily habit details prevent mismatch later.",
            "Lifestyle fit helps matching stay practical and honest."
        )
    ),
    6 to WizardStepCopy(
        title = "Partner Preferences",
        eyebrow = "Step 6 of 10",
        subtitle = "Set the match basics you care about most before profile review.",
        helper = "Height range, religion/community, education, and occupation preferences power match quality.",
        info = "Partner preferences directly drive recommendation quality and shortlist relevance.",
        infoBullets = listOf(
            "Good preferences improve recommendation accuracy.",
            "Balanced filters give more relevant matches."
        )
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

private val profileCityOptions = listOf(
    "Bengaluru", "Hyderabad", "Chennai", "Mumbai", "Delhi", "Pune",
    "Ahmedabad", "Jaipur", "Kolkata", "Lucknow", "Kochi", "Coimbatore"
)

private val motherTongueOptions = listOf(
    "Assamese", "Bengali", "Bodo", "Dogri", "English", "Gujarati", "Hindi", "Kannada",
    "Kashmiri", "Konkani", "Maithili", "Malayalam", "Manipuri", "Marathi", "Nepali",
    "Odia", "Punjabi", "Sanskrit", "Santali", "Sindhi", "Tamil", "Telugu", "Urdu"
)

private val hobbyOptions = listOf("Travel", "Reading", "Music", "Fitness", "Cooking", "Movies", "Photography", "Temple visits")
private val languageOptions = listOf("English", "Hindi", "Telugu", "Tamil", "Kannada", "Malayalam", "Gujarati", "Marathi")
private val personalityOptions = listOf("Calm", "Family-oriented", "Ambitious", "Spiritual", "Warm", "Practical", "Social", "Creative")
private val locationPreferenceOptions = listOf("Same city", "Same state", "South India", "Metro cities", "Tier 2 cities", "Open to relocate")
private val incomePreferenceOptions = listOf("< 5 LPA", "5-10 LPA", "10-20 LPA", "20-35 LPA", "35+ LPA")
private val lifestylePreferenceOptions = listOf("Vegetarian", "No smoking", "No drinking", "Family-focused", "Career-minded", "Traditional values")

private val profileHeightOptions = buildList {
    for (feet in 4..7) {
        for (inches in 0..11) {
            add("${feet}'${inches}\"")
        }
    }
}

private fun wizardStepProgress(stepNumber: Int): Int = stepNumber.coerceIn(1, 10) * 10
private val fullNamePattern = Regex("""^[A-Za-z][A-Za-z .'-]*$""")
private val locationPattern = Regex("""^[A-Za-z][A-Za-z ,.'-]*$""")
private val plainTextPattern = Regex("""^[A-Za-z][A-Za-z .,'-]*$""")

private fun isValidFullName(value: String): Boolean =
    value.trim().length >= 2 && fullNamePattern.matches(value.trim()) && value.trim().any(Char::isLetter)

private fun isValidLocationName(value: String): Boolean =
    locationPattern.matches(value.trim()) && value.trim().any(Char::isLetter)

private fun isValidPlainText(value: String): Boolean =
    plainTextPattern.matches(value.trim()) && value.trim().any(Char::isLetter)

private fun Map<String, Any>.stringValue(key: String, fallback: String = ""): String =
    (this[key] as? String)?.takeIf { it.isNotBlank() } ?: fallback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileWizardScreen(
    step: Int,
    isSectionEdit: Boolean = false,
    onNextStep: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vm: ProfileViewModel = hiltViewModel(context as ComponentActivity)
    val currentStep = step.coerceIn(1, 6)
    val profile by vm.profile.collectAsStateWithLifecycle()
    val isSaving by vm.isSaving.collectAsStateWithLifecycle()
    val errorMessage by vm.errorMessage.collectAsStateWithLifecycle()
    var isCurrentStepValid by remember(currentStep) { mutableStateOf(false) }
    var showInfo by remember(currentStep) { mutableStateOf(false) }
    val copy = wizardCopy.getValue(currentStep)
    val progressPercent = wizardStepProgress(currentStep)

    if (showInfo) {
        ProfileWizardInfoBottomSheet(
            title = "Why ${copy.title.lowercase()} matters",
            body = copy.info,
            bullets = copy.infoBullets,
            onDismiss = { showInfo = false }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(if (currentStep == 6) "" else titleCase(copy.title), fontWeight = FontWeight.Bold, color = SoulMatchTokens.Text)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SoulMatchTokens.Bg,
                    navigationIconContentColor = SoulMatchTokens.Tangerine,
                    titleContentColor = SoulMatchTokens.Text
                ),
                navigationIcon = {
                    SoulMatchHeaderIconButton(icon = Icons.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)
                },
                actions = {
                    SoulMatchHeaderIconButton(icon = Icons.Filled.Info, contentDescription = "Info", onClick = { showInfo = true })
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
                    WizardProgressHeader(
                        stepNumber = currentStep,
                        progressPercent = progressPercent,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            vm.clearError()
                            vm.saveStep(currentStep) { onNextStep(currentStep + 1) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
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
    val draft = vm.stepData(1)
    var fullName by rememberSaveable(existing?.profileId) {
        mutableStateOf(
            draft.stringValue(
                "firstName",
                listOf(existing?.firstName, existing?.lastName).filterNotNull().filter { it.isNotBlank() }.joinToString(" ")
            ).let { first ->
                val draftLastName = draft.stringValue("lastName", existing?.lastName.orEmpty())
                listOf(first, draftLastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank {
                    listOf(existing?.firstName, existing?.lastName).filterNotNull().filter { it.isNotBlank() }.joinToString(" ")
                }
            }
        )
    }
    var dob by rememberSaveable(existing?.profileId) { mutableStateOf(draft.stringValue("dob", sanitizeDobForDisplay(existing?.dob).orEmpty())) }
    var gender by rememberSaveable(existing?.profileId) { mutableStateOf(draft.stringValue("gender", existing?.gender.orEmpty())) }
    var heightLabel by rememberSaveable(existing?.profileId) {
        mutableStateOf(
            draft.stringValue("heightCm").toIntOrNull()?.let(::heightLabelFromCm)
                ?: heightLabelFromCm(existing?.heightCm)
        )
    }
    var maritalStatus by rememberSaveable(existing?.profileId) { mutableStateOf(draft.stringValue("maritalStatus", existing?.maritalStatus.orEmpty())) }
    var motherTongue by rememberSaveable(existing?.profileId) { mutableStateOf(draft.stringValue("motherTongue", existing?.motherTongue.orEmpty())) }
    var currentCity by rememberSaveable(existing?.profileId) { mutableStateOf(draft.stringValue("workingCity", existing?.workingCity.orEmpty())) }
    var nativePlace by rememberSaveable(existing?.profileId) {
        mutableStateOf(
            draft.stringValue(
                "nativePlace",
                existing?.nativePlace?.ifBlank { existing.birthCity.orEmpty() } ?: existing?.birthCity.orEmpty()
            )
        )
    }
    val normalizedDob = normalizeDateOfBirth(dob)
    val dobHasError = dob.isNotBlank() && normalizedDob == null
    val (firstName, lastName) = splitFullName(fullName)
    val fullNameError = fullName.isNotBlank() && !isValidFullName(fullName)
    val heightCm = heightCmFromLabel(heightLabel)
    val hasInteracted = listOf(fullName, dob, gender, heightLabel, maritalStatus, motherTongue, currentCity, nativePlace).any { it.isNotBlank() }
    val genderError = hasInteracted && gender.isBlank()
    val heightError = hasInteracted && heightLabel.isBlank()
    val maritalStatusError = hasInteracted && maritalStatus.isBlank()
    val motherTongueError = hasInteracted && motherTongue.isBlank()
    val currentCityError = (hasInteracted && currentCity.isBlank()) || (currentCity.isNotBlank() && !isValidLocationName(currentCity))
    val nativePlaceError = (hasInteracted && nativePlace.isBlank()) || (nativePlace.isNotBlank() && !isValidLocationName(nativePlace))

    val isValid = !fullNameError &&
        normalizedDob != null &&
        heightCm != null &&
        firstName.isNotBlank() &&
        listOf(gender, maritalStatus, motherTongue, currentCity, nativePlace).all { it.isNotBlank() } &&
        !currentCityError &&
        !nativePlaceError
    LaunchedEffect(fullName, normalizedDob, dobHasError, gender, heightCm, maritalStatus, motherTongue, currentCity, nativePlace) {
        vm.updateStep1Data(
            mapOf(
                "firstName" to firstName,
                "lastName" to lastName,
                "dob" to (normalizedDob ?: dob.trim()),
                "gender" to gender,
                "heightCm" to (heightCm ?: 0),
                "maritalStatus" to maritalStatus,
                "motherTongue" to motherTongue,
                "workingCity" to currentCity,
                "nativePlace" to nativePlace
            )
        )
        onValidityChange(isValid)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        RequiredTextField(
            fullName,
            { fullName = it },
            "Full name",
            leadingIcon = Icons.Filled.Person,
            isError = fullNameError,
            supportingText = if (fullNameError) "Please enter a valid name." else null
        )
        SelectionField(
            label = "Gender",
            value = titleCase(gender),
            options = listOf("Male", "Female"),
            onSelect = { gender = it.lowercase() },
            leadingIcon = Icons.Filled.People,
            isError = genderError,
            supportingText = if (genderError) "Gender is required" else null
        )
        DatePickerField(
            dob,
            onValueChange = { dob = it },
            "Date of birth",
            leadingIcon = Icons.Filled.DateRange,
            isError = dobHasError,
            supportingText = if (dobHasError) "Age must be 20 years or above." else null
        )
        SelectionField(
            label = "Height",
            value = heightLabel,
            options = profileHeightOptions,
            onSelect = { heightLabel = it },
            leadingIcon = Icons.Filled.Straighten,
            isError = heightError,
            supportingText = if (heightError) "Height is required" else null
        )
        SelectionField(
            label = "Marital status",
            value = titleCase(maritalStatus.replace('_', ' ')),
            options = listOf("Never married", "Divorced", "Widowed"),
            onSelect = { maritalStatus = it.lowercase().replace(' ', '_') },
            leadingIcon = Icons.Filled.CheckCircle,
            isError = maritalStatusError,
            supportingText = if (maritalStatusError) "Marital status is required" else null
        )
        SelectionField(
            label = "Mother tongue",
            value = motherTongue,
            options = motherTongueOptions,
            onSelect = { motherTongue = it },
            leadingIcon = Icons.Filled.Language,
            isError = motherTongueError,
            supportingText = if (motherTongueError) "Mother tongue is required" else null
        )
        RequiredTextField(
            currentCity,
            { currentCity = it },
            "Current city",
            leadingIcon = Icons.Filled.LocationOn,
            isError = currentCityError,
            supportingText = when {
                currentCity.isBlank() && hasInteracted -> "Current city is required"
                currentCity.isNotBlank() && !isValidLocationName(currentCity) -> "Please enter a valid city name"
                else -> null
            }
        )
        RequiredTextField(
            nativePlace,
            { nativePlace = it },
            "Native place",
            leadingIcon = Icons.Filled.Home,
            isError = nativePlaceError,
            supportingText = when {
                nativePlace.isBlank() && hasInteracted -> "Native place is required"
                nativePlace.isNotBlank() && !isValidLocationName(nativePlace) -> "Please enter a valid native place"
                else -> null
            }
        )
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
    val draft = vm.stepData(2)
    var religion by rememberSaveable(existing?.profileId) { mutableStateOf(draft.stringValue("religion", existing?.religion.orEmpty())) }
    var caste by rememberSaveable(existing?.profileId) { mutableStateOf(draft.stringValue("caste", existing?.caste.orEmpty())) }
    var subCaste by rememberSaveable(existing?.profileId) { mutableStateOf(draft.stringValue("subCaste", existing?.subCaste.orEmpty())) }
    var gothram by rememberSaveable(existing?.profileId) { mutableStateOf(draft.stringValue("gotra", existing?.gotra.orEmpty())) }
    var religiousValues by rememberSaveable(existing?.profileId) { mutableStateOf(draft.stringValue("religiousValues", existing?.religiousValues.orEmpty())) }
    LaunchedEffect(existing?.profileId) {
        if (religion.isBlank() && existing?.religion?.isNotBlank() == true) religion = existing.religion
        if (caste.isBlank() && existing?.caste?.isNotBlank() == true) caste = existing.caste
        if (subCaste.isBlank() && existing?.subCaste?.isNotBlank() == true) subCaste = existing.subCaste
        if (gothram.isBlank() && existing?.gotra?.isNotBlank() == true) gothram = existing.gotra
        if (religiousValues.isBlank() && existing?.religiousValues?.isNotBlank() == true) religiousValues = existing.religiousValues
    }
    val hasInteracted = listOf(religion, caste, subCaste, gothram, religiousValues).any { it.isNotBlank() }
    val religionError = (hasInteracted && religion.isBlank()) || (religion.isNotBlank() && !isValidPlainText(religion))
    val casteError = (hasInteracted && caste.isBlank()) || (caste.isNotBlank() && !isValidPlainText(caste))
    val gothramError = gothram.isNotBlank() && !isValidPlainText(gothram)

    val isValid = religion.isNotBlank() && caste.isNotBlank() && !religionError && !casteError && !gothramError
    LaunchedEffect(religion, caste, subCaste, gothram, religiousValues) {
        vm.updateStep2Data(
            mapOf(
                "religion" to religion,
                "caste" to caste,
                "subCaste" to subCaste,
                "gotra" to gothram,
                "religiousValues" to religiousValues
            )
        )
        onValidityChange(isValid)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        RequiredTextField(
            religion,
            { religion = it },
            "Religion",
            isError = religionError,
            supportingText = when {
                religion.isBlank() && hasInteracted -> "Religion is required"
                religion.isNotBlank() && !isValidPlainText(religion) -> "Please enter a valid religion"
                else -> null
            }
        )
        RequiredTextField(
            caste,
            { caste = it },
            "Community / caste",
            isError = casteError,
            supportingText = when {
                caste.isBlank() && hasInteracted -> "Community is required"
                caste.isNotBlank() && !isValidPlainText(caste) -> "Please enter a valid community"
                else -> null
            }
        )
        RequiredTextField(subCaste, { subCaste = it }, "Sub-caste")
        RequiredTextField(
            gothram,
            { gothram = it },
            "Gothram",
            isError = gothramError,
            supportingText = if (gothramError) "Please enter a valid gothram" else null
        )
        SelectionField(
            label = "Religious values",
            value = religiousValues,
            options = listOf("Traditional", "Moderate", "Spiritual", "Liberal"),
            onSelect = { religiousValues = it }
        )
    }
}

@Composable
private fun Step3Education(existing: ProfileData?, vm: ProfileViewModel, onValidityChange: (Boolean) -> Unit) {
    val draft = vm.stepData(3)
    var educationLevel by rememberSaveable(existing?.profileId) { mutableStateOf(draft.stringValue("educationLevel", existing?.educationLevel.orEmpty())) }
    var institutionName by rememberSaveable(existing?.profileId) { mutableStateOf(draft.stringValue("institutionName", existing?.institutionName.orEmpty())) }
    var occupation by rememberSaveable(existing?.profileId) { mutableStateOf(draft.stringValue("occupation", existing?.occupation.orEmpty())) }
    var companyName by rememberSaveable(existing?.profileId) { mutableStateOf(draft.stringValue("companyName", existing?.companyName.orEmpty())) }
    var annualIncome by rememberSaveable(existing?.profileId) { mutableStateOf(draft.stringValue("annualIncome", existing?.annualIncome.orEmpty())) }
    var workLocation by rememberSaveable(existing?.profileId) {
        mutableStateOf(draft.stringValue("workingCity", existing?.workLocation?.ifBlank { existing.workingCity.orEmpty() } ?: existing?.workingCity.orEmpty()))
    }
    LaunchedEffect(existing?.profileId) {
        if (educationLevel.isBlank() && existing?.educationLevel?.isNotBlank() == true) educationLevel = existing.educationLevel
        if (institutionName.isBlank() && existing?.institutionName?.isNotBlank() == true) institutionName = existing.institutionName
        if (occupation.isBlank() && existing?.occupation?.isNotBlank() == true) occupation = existing.occupation
        if (companyName.isBlank() && existing?.companyName?.isNotBlank() == true) companyName = existing.companyName
        if (annualIncome.isBlank() && existing?.annualIncome?.isNotBlank() == true) annualIncome = existing.annualIncome
        if (workLocation.isBlank()) {
            workLocation = existing?.workLocation?.ifBlank { existing.workingCity.orEmpty() } ?: existing?.workingCity.orEmpty()
        }
    }
    val workingState = existing?.workingState.orEmpty()
    val workingPincode = existing?.workingPincode.orEmpty()
    val hasInteracted = listOf(educationLevel, institutionName, occupation, companyName, annualIncome, workLocation).any { it.isNotBlank() }
    val companyError = hasInteracted && companyName.isBlank()
    val workLocationError = (hasInteracted && workLocation.isBlank()) || (workLocation.isNotBlank() && !isValidLocationName(workLocation))

    val isValid = educationLevel.isNotBlank() &&
        institutionName.isNotBlank() &&
        occupation.isNotBlank() &&
        annualIncome.isNotBlank() &&
        companyName.isNotBlank() &&
        workLocation.isNotBlank() &&
        !workLocationError

    LaunchedEffect(educationLevel, institutionName, occupation, companyName, annualIncome, workLocation, workingState, workingPincode) {
        vm.updateStep3Data(
            mapOf(
                "educationLevel" to educationLevel,
                "institutionName" to institutionName,
                "occupation" to occupation,
                "companyName" to companyName,
                "annualIncome" to annualIncome,
                "workingCity" to workLocation,
                "workingState" to workingState,
                "workingPincode" to workingPincode,
                "workLocation" to workLocation
            )
        )
        onValidityChange(isValid)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SelectionField(
            label = "Highest qualification",
            value = educationLevel,
            options = listOf("High School", "Diploma", "Graduate", "Post Graduate", "MBA", "Doctorate", "Professional"),
            onSelect = { educationLevel = it }
        )
        RequiredTextField(institutionName, { institutionName = it }, "Institution")
        RequiredTextField(occupation, { occupation = it }, "Occupation")
        RequiredTextField(
            companyName,
            { companyName = it },
            "Company",
            isError = companyError,
            supportingText = if (companyError) "Company name is required" else null
        )
        SelectionField(
            label = "Annual income",
            value = annualIncome,
            options = listOf("< 3 LPA", "3-5 LPA", "5-10 LPA", "10-20 LPA", "20-35 LPA", "35+ LPA"),
            onSelect = { annualIncome = it }
        )
        RequiredTextField(
            workLocation,
            { workLocation = it },
            "Work location",
            isError = workLocationError,
            supportingText = when {
                workLocation.isBlank() && hasInteracted -> "Work location is required"
                workLocation.isNotBlank() && !isValidLocationName(workLocation) -> "Please enter a valid work location"
                else -> null
            }
        )
    }
}

@Composable
private fun Step4Family(existing: ProfileData?, vm: ProfileViewModel, onValidityChange: (Boolean) -> Unit) {
    val draft = vm.stepData(4)
    var familyStatus by rememberSaveable(existing?.profileId) { mutableStateOf(draft.stringValue("familyStatus", existing?.familyStatus.orEmpty())) }
    var fatherOccupation by rememberSaveable(existing?.profileId) { mutableStateOf(draft.stringValue("fatherOccupation", existing?.fatherOccupation.orEmpty())) }
    var motherOccupation by rememberSaveable(existing?.profileId) { mutableStateOf(draft.stringValue("motherOccupation", existing?.motherOccupation.orEmpty())) }
    var familyType by rememberSaveable(existing?.profileId) { mutableStateOf(draft.stringValue("familyType", existing?.familyType.orEmpty())) }
    var numBrothers by rememberSaveable(existing?.profileId) { mutableStateOf(draft["numBrothers"]?.toString() ?: existing?.numBrothers?.toString().orEmpty()) }
    var numSisters by rememberSaveable(existing?.profileId) { mutableStateOf(draft["numSisters"]?.toString() ?: existing?.numSisters?.toString().orEmpty()) }
    var aboutFamily by rememberSaveable(existing?.profileId) {
        mutableStateOf(
            draft.stringValue(
                "aboutFamily",
                existing?.aboutFamily?.ifBlank { existing.familyLocality.orEmpty() } ?: existing?.familyLocality.orEmpty()
            )
        )
    }
    LaunchedEffect(existing?.profileId) {
        if (familyStatus.isBlank() && existing?.familyStatus?.isNotBlank() == true) familyStatus = existing.familyStatus
        if (fatherOccupation.isBlank() && existing?.fatherOccupation?.isNotBlank() == true) fatherOccupation = existing.fatherOccupation
        if (motherOccupation.isBlank() && existing?.motherOccupation?.isNotBlank() == true) motherOccupation = existing.motherOccupation
        if (familyType.isBlank() && existing?.familyType?.isNotBlank() == true) familyType = existing.familyType
        if (numBrothers.isBlank() && existing?.numBrothers != null) numBrothers = existing.numBrothers.toString()
        if (numSisters.isBlank() && existing?.numSisters != null) numSisters = existing.numSisters.toString()
        if (aboutFamily.isBlank()) {
            aboutFamily = existing?.aboutFamily?.ifBlank { existing.familyLocality.orEmpty() } ?: existing?.familyLocality.orEmpty()
        }
    }
    val hasInteracted = listOf(familyStatus, fatherOccupation, motherOccupation, familyType, numBrothers, numSisters, aboutFamily).any { it.isNotBlank() }
    val aboutFamilyError = aboutFamily.isNotBlank() && aboutFamily.trim().length < 40

    val isValid = familyStatus.isNotBlank() &&
        fatherOccupation.isNotBlank() &&
        motherOccupation.isNotBlank() &&
        familyType.isNotBlank() &&
        aboutFamily.isNotBlank() &&
        !aboutFamilyError
    LaunchedEffect(familyStatus, fatherOccupation, motherOccupation, familyType, numBrothers, numSisters, aboutFamily) {
        vm.updateStep4Data(
            mapOf(
                "familyStatus" to familyStatus,
                "fatherOccupation" to fatherOccupation,
                "motherOccupation" to motherOccupation,
                "numBrothers" to (numBrothers.toIntOrNull() ?: 0),
                "numSisters" to (numSisters.toIntOrNull() ?: 0),
                "familyType" to familyType,
                "familyCity" to existing?.familyCity.orEmpty(),
                "familyState" to existing?.familyState.orEmpty(),
                "familyLocality" to aboutFamily.take(120),
                "familyPincode" to existing?.familyPincode.orEmpty(),
                "aboutFamily" to aboutFamily
            )
        )
        onValidityChange(isValid)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SelectionField(
            label = "Family status",
            value = familyStatus,
            options = listOf("Middle class", "Upper middle class", "Affluent", "Simple and grounded"),
            onSelect = { familyStatus = it }
        )
        SelectionField(
            label = "Family type",
            value = familyType,
            options = listOf("Nuclear", "Joint"),
            onSelect = { familyType = it }
        )
        RequiredTextField(fatherOccupation, { fatherOccupation = it }, "Father occupation")
        RequiredTextField(motherOccupation, { motherOccupation = it }, "Mother occupation")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SelectionField(
                label = "Brothers",
                value = numBrothers,
                options = listOf("0", "1", "2", "3", "4+"),
                onSelect = { numBrothers = it.filter(Char::isDigit) },
                modifier = Modifier.weight(1f)
            )
            SelectionField(
                label = "Sisters",
                value = numSisters,
                options = listOf("0", "1", "2", "3", "4+"),
                onSelect = { numSisters = it.filter(Char::isDigit) },
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = aboutFamily,
            onValueChange = { aboutFamily = it },
            label = { Text("About family *") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            isError = aboutFamilyError,
            supportingText = {
                if (aboutFamilyError) {
                    Text("About family needs at least 40 characters.")
                } else if (hasInteracted && aboutFamily.isBlank()) {
                    Text("About family is required")
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                errorContainerColor = Color.White
            )
        )
    }
}

@Composable
private fun Step5Lifestyle(existing: ProfileData?, vm: ProfileViewModel, onValidityChange: (Boolean) -> Unit) {
    val draft = vm.stepData(5)
    var diet by rememberSaveable(existing?.profileId) { mutableStateOf(draft.stringValue("diet", existing?.diet.orEmpty())) }
    var smoking by rememberSaveable(existing?.profileId) { mutableStateOf(draft.stringValue("smoking", existing?.smoking?.ifBlank { "never" } ?: "never")) }
    var drinking by rememberSaveable(existing?.profileId) { mutableStateOf(draft.stringValue("drinking", existing?.drinking?.ifBlank { "never" } ?: "never")) }
    var hobbies by rememberSaveable(existing?.profileId) { mutableStateOf((draft["hobbies"] as? List<*>)?.filterIsInstance<String>() ?: (existing?.hobbies ?: emptyList())) }
    var languagesKnown by rememberSaveable(existing?.profileId) { mutableStateOf((draft["languagesKnown"] as? List<*>)?.filterIsInstance<String>() ?: (existing?.languagesKnown ?: emptyList())) }
    var personalityTraits by rememberSaveable(existing?.profileId) { mutableStateOf((draft["personalityTraits"] as? List<*>)?.filterIsInstance<String>() ?: (existing?.personalityTraits ?: emptyList())) }
    LaunchedEffect(existing?.profileId) {
        if (diet.isBlank() && existing?.diet?.isNotBlank() == true) diet = existing.diet
        if (smoking.isBlank() && existing?.smoking?.isNotBlank() == true) smoking = existing.smoking
        if (drinking.isBlank() && existing?.drinking?.isNotBlank() == true) drinking = existing.drinking
        if (hobbies.isEmpty() && !existing?.hobbies.isNullOrEmpty()) hobbies = existing?.hobbies ?: emptyList()
        if (languagesKnown.isEmpty() && !existing?.languagesKnown.isNullOrEmpty()) languagesKnown = existing?.languagesKnown ?: emptyList()
        if (personalityTraits.isEmpty() && !existing?.personalityTraits.isNullOrEmpty()) personalityTraits = existing?.personalityTraits ?: emptyList()
    }
    val isValid = diet.isNotBlank() &&
        smoking.isNotBlank() &&
        drinking.isNotBlank() &&
        hobbies.isNotEmpty() &&
        languagesKnown.isNotEmpty() &&
        personalityTraits.isNotEmpty()
    LaunchedEffect(diet, smoking, drinking, hobbies, languagesKnown, personalityTraits) {
        vm.updateStep5Data(
            mapOf(
                "diet" to diet,
                "smoking" to smoking,
                "drinking" to drinking,
                "hobbies" to hobbies,
                "languagesKnown" to languagesKnown,
                "personalityTraits" to personalityTraits
            )
        )
        onValidityChange(isValid)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ChipRow("Diet", listOf("vegetarian", "jain", "eggetarian", "non_vegetarian"), diet) { diet = it }
        ChipRow("Smoking", listOf("never", "occasionally"), smoking) { smoking = it }
        ChipRow("Drinking", listOf("never", "socially", "occasionally"), drinking) { drinking = it }
        MultiSelectChipField("Hobbies", hobbyOptions, hobbies) { hobbies = it }
        MultiSelectChipField("Languages known", languageOptions, languagesKnown) { languagesKnown = it }
        MultiSelectChipField("Personality traits", personalityOptions, personalityTraits) { personalityTraits = it }
    }
}

@Composable
private fun Step6PartnerPreferences(existing: ProfileData?, vm: ProfileViewModel, onValidityChange: (Boolean) -> Unit) {
    val existingPreferences by vm.partnerPreferences.collectAsStateWithLifecycle()
    var ageMin by rememberSaveable(existing?.profileId) { mutableStateOf("") }
    var ageMax by rememberSaveable(existing?.profileId) { mutableStateOf("") }
    var heightMin by rememberSaveable(existing?.profileId) { mutableStateOf("") }
    var heightMax by rememberSaveable(existing?.profileId) { mutableStateOf("") }
    var religionCommunity by rememberSaveable(existing?.profileId) { mutableStateOf("") }
    var locationPreferences by rememberSaveable(existing?.profileId) { mutableStateOf(emptyList<String>()) }
    var education by rememberSaveable(existing?.profileId) { mutableStateOf(emptyList<String>()) }
    var occupation by rememberSaveable(existing?.profileId) { mutableStateOf(emptyList<String>()) }
    var incomePreferences by rememberSaveable(existing?.profileId) { mutableStateOf(emptyList<String>()) }
    var lifestylePreferences by rememberSaveable(existing?.profileId) { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(existing?.profileId, existingPreferences) {
        if (ageMin.isBlank() && existingPreferences.ageMin > 0) ageMin = existingPreferences.ageMin.toString()
        if (ageMax.isBlank() && existingPreferences.ageMax > 0) ageMax = existingPreferences.ageMax.toString()
        if (heightMin.isBlank() && existingPreferences.heightMinCm != null) heightMin = heightLabelFromCm(existingPreferences.heightMinCm)
        if (heightMax.isBlank() && existingPreferences.heightMaxCm != null) heightMax = heightLabelFromCm(existingPreferences.heightMaxCm)
        if (religionCommunity.isBlank() && !existingPreferences.religion.isNullOrBlank()) religionCommunity = existingPreferences.religion.orEmpty()
        if (locationPreferences.isEmpty()) {
            locationPreferences = existingPreferences.locationPreferences.ifEmpty { existing?.locationPreferences ?: emptyList() }
        }
        if (education.isEmpty()) education = existingPreferences.educationLevels
        if (occupation.isEmpty()) occupation = existingPreferences.occupations
        if (incomePreferences.isEmpty()) {
            incomePreferences = existingPreferences.incomePreferences.ifEmpty { existing?.incomePreferences ?: emptyList() }
        }
        if (lifestylePreferences.isEmpty()) {
            lifestylePreferences = existingPreferences.lifestylePreferences.ifEmpty { existing?.lifestylePreferences ?: emptyList() }
        }
    }

    val ageMinValue = ageMin.toIntOrNull()
    val ageMaxValue = ageMax.toIntOrNull()
    val heightMinCm = heightCmFromLabel(heightMin)
    val heightMaxCm = heightCmFromLabel(heightMax)
    val isTooRestrictive = (ageMinValue != null && ageMaxValue != null && ageMaxValue - ageMinValue <= 3) &&
        education.size <= 1 &&
        occupation.size <= 1 &&
        locationPreferences.size <= 1
    val isValid = ageMinValue != null &&
        ageMaxValue != null &&
        ageMinValue < ageMaxValue &&
        heightMinCm != null &&
        heightMaxCm != null &&
        heightMinCm <= heightMaxCm &&
        religionCommunity.isNotBlank() &&
        education.isNotEmpty() &&
        occupation.isNotEmpty() &&
        locationPreferences.isNotEmpty() &&
        incomePreferences.isNotEmpty() &&
        lifestylePreferences.isNotEmpty()

    LaunchedEffect(ageMin, ageMax, heightMin, heightMax, religionCommunity, locationPreferences, education, occupation, incomePreferences, lifestylePreferences) {
        vm.updatePartnerPreferences(
            PartnerPreferencesData(
                ageMin = ageMinValue ?: 23,
                ageMax = ageMaxValue ?: 31,
                religion = religionCommunity.trim().ifBlank { null },
                educationLevels = education,
                occupations = occupation,
                heightMinCm = heightMinCm,
                heightMaxCm = heightMaxCm,
                locations = locationPreferences,
                locationPreferences = locationPreferences,
                incomePreferences = incomePreferences,
                lifestylePreferences = lifestylePreferences,
                maritalStatuses = listOfNotNull(existing?.maritalStatus?.takeIf { it.isNotBlank() }),
                familyTypes = listOfNotNull(existing?.familyType?.takeIf { it.isNotBlank() })
            )
        )
        onValidityChange(isValid)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField(ageMin, { ageMin = it.filter(Char::isDigit).take(2) }, "Age min", Modifier.weight(1f))
            NumberField(ageMax, { ageMax = it.filter(Char::isDigit).take(2) }, "Age max", Modifier.weight(1f))
        }
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
        MultiSelectChipField("Location preference", locationPreferenceOptions, locationPreferences) { locationPreferences = it }
        MultiSelectChipField("Education preference", listOf("Graduate", "Post Graduate", "MBA", "Doctorate", "Professional"), education) { education = it }
        MultiSelectChipField("Occupation preference", listOf("Engineer", "Doctor", "Business", "Teacher", "Government", "Designer"), occupation) { occupation = it }
        MultiSelectChipField("Income preference", incomePreferenceOptions, incomePreferences) { incomePreferences = it }
        MultiSelectChipField("Lifestyle preference", lifestylePreferenceOptions, lifestylePreferences) { lifestylePreferences = it }
        if (isTooRestrictive) {
            ValidationBanner("Preferences look very restrictive. Match count may reduce.")
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ProfileWizardInfoBottomSheet(
    title: String,
    body: String,
    bullets: List<String>,
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
            bullets.forEach { bullet ->
                WizardInfoBullet(bullet)
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(SoulMatchTokens.PillRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoulMatchTokens.Tangerine,
                    contentColor = Color.White
                )
            ) {
                Text("Got it", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
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

@Composable
private fun WizardProgressHeader(
    stepNumber: Int,
    progressPercent: Int,
    modifier: Modifier = Modifier
) {
    val progress = (progressPercent.coerceIn(0, 100)) / 100f
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Step $stepNumber of 10",
                color = SoulMatchTokens.Tangerine,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$progressPercent% complete",
                color = SoulMatchTokens.Muted,
                fontWeight = FontWeight.Bold
            )
        }
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = SoulMatchTokens.Tangerine,
            trackColor = Color(0xFFF3E5DE)
        )
    }
}

@Composable
private fun ValidationBanner(message: String) {
    PremiumCard(containerColor = MaterialTheme.colorScheme.errorContainer, contentPadding = PaddingValues(14.dp)) {
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
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
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(titleCase(option.replace('_', ' '))) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                    if (index < options.lastIndex) {
                        Divider(color = SoulMatchTokens.Border)
                    }
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
    leadingIcon: ImageVector? = null,
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
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null, tint = SoulMatchTokens.Tangerine) } },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            errorContainerColor = Color.White
        ),
        shape = RoundedCornerShape(SoulMatchTokens.CardRadius),
        singleLine = true
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MultiSelectChipField(
    title: String,
    options: List<String>,
    selected: List<String>,
    onChange: (List<String>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$title *", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = SoulMatchTokens.Text)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChoiceChip(
                    label = option,
                    selected = selected.contains(option),
                    onClick = {
                        onChange(if (selected.contains(option)) selected - option else selected + option)
                    }
                )
            }
        }
    }
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
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            errorContainerColor = Color.White
        ),
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
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    isError: Boolean = false,
    supportingText: String? = null
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
            isError = isError,
            supportingText = supportingText?.let { message -> { Text(message) } },
            leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null, tint = SoulMatchTokens.Tangerine) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                errorContainerColor = Color.White
            ),
            shape = RoundedCornerShape(SoulMatchTokens.CardRadius),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .exposedDropdownSize(matchTextFieldWidth = true)
                .background(Color.White)
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
                if (index < options.lastIndex) {
                    Divider(color = SoulMatchTokens.Border)
                }
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
    val youngestAllowed = today.minusYears(20)
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
    leadingIcon: ImageVector? = null,
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
            datePicker.maxDate = LocalDate.now().minusYears(20).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            datePicker.minDate = LocalDate.now().minusYears(80).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    }
    OutlinedTextField(
        value = value,
        onValueChange = { raw -> onValueChange(formatDateInput(raw)) },
        label = { Text("$label *") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = isError,
        supportingText = supportingText?.let { message -> { Text(message) } },
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null, tint = SoulMatchTokens.Tangerine) } },
        trailingIcon = {
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = "Open calendar",
                tint = SoulMatchTokens.Tangerine,
                modifier = Modifier.clickable { picker.show() }
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            errorContainerColor = Color.White
        ),
        shape = RoundedCornerShape(SoulMatchTokens.CardRadius),
        singleLine = true
    )
}


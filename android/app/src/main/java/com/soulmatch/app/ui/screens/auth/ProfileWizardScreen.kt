package com.soulmatch.app.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.soulmatch.app.data.models.ProfileData
import com.soulmatch.app.ui.components.ChipTone
import com.soulmatch.app.ui.components.FilterChoiceChip
import com.soulmatch.app.ui.components.LabeledProgress
import com.soulmatch.app.ui.components.PremiumCard
import com.soulmatch.app.ui.components.PremiumHeader
import com.soulmatch.app.ui.components.PremiumScreen
import com.soulmatch.app.ui.components.SignalChip
import com.soulmatch.app.ui.components.SignalChips
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.PrimaryDark
import com.soulmatch.app.ui.theme.Success
import com.soulmatch.app.ui.theme.SurfaceSoft
import com.soulmatch.app.ui.theme.SurfaceWarm
import com.soulmatch.app.ui.theme.TextSecondary
import com.soulmatch.app.ui.titleCase
import com.soulmatch.app.ui.viewmodels.ProfileViewModel

private data class WizardStepCopy(
    val title: String,
    val eyebrow: String,
    val subtitle: String,
    val helper: String
)

private val wizardCopy = mapOf(
    1 to WizardStepCopy(
        title = "Basic identity",
        eyebrow = "Step 1 of 6",
        subtitle = "Start with details families and search filters expect to see first.",
        helper = "Name, date of birth, religion, community, language, gender, and marital status are required."
    ),
    2 to WizardStepCopy(
        title = "Physical profile",
        eyebrow = "Step 2 of 6",
        subtitle = "These details make shortlisting and preference checks easier.",
        helper = "Height, weight, complexion, body type, and blood group are treated as important profile signals."
    ),
    3 to WizardStepCopy(
        title = "Education and career",
        eyebrow = "Step 3 of 6",
        subtitle = "Make profession, education, income, and work city easy to scan.",
        helper = "Career details power search, ranking, and partner preference matching."
    ),
    4 to WizardStepCopy(
        title = "Family background",
        eyebrow = "Step 4 of 6",
        subtitle = "A matrimony profile should make family context feel clear and respectful.",
        helper = "Parent occupations, siblings, family type, and family city are required for trust."
    ),
    5 to WizardStepCopy(
        title = "Lifestyle and about me",
        eyebrow = "Step 5 of 6",
        subtitle = "Turn the profile from a checklist into a real introduction.",
        helper = "Diet, habits, and a thoughtful about section help serious members decide with confidence."
    ),
    6 to WizardStepCopy(
        title = "Horoscope and traditions",
        eyebrow = "Step 6 of 6",
        subtitle = "Optional for some users, essential for others.",
        helper = "Horoscope fields stay optional, but completing them improves compatibility context."
    )
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
    val copy = wizardCopy.getValue(currentStep)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(copy.title, fontWeight = FontWeight.Bold)
                        Text(copy.eyebrow, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
                    PremiumHeader(
                        eyebrow = copy.eyebrow,
                        title = copy.title,
                        subtitle = copy.subtitle
                    )
                    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp), containerColor = SurfaceWarm) {
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
                            Text(copy.helper, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
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
                            2 -> Step2Physical(profile, vm) { isCurrentStepValid = it }
                            3 -> Step3Education(profile, vm) { isCurrentStepValid = it }
                            4 -> Step4Family(profile, vm) { isCurrentStepValid = it }
                            5 -> Step5Lifestyle(profile, vm) { isCurrentStepValid = it }
                            6 -> Step6Horoscope(profile, vm) { isCurrentStepValid = it }
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
                        OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(54.dp)) {
                            Text("Back")
                        }
                    }
                    Button(
                        onClick = {
                            vm.clearError()
                            vm.saveStep(currentStep) { onNextStep(currentStep + 1) }
                        },
                        modifier = Modifier.weight(1f).height(54.dp),
                        enabled = isCurrentStepValid && !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(
                                when {
                                    isSectionEdit -> "Save section"
                                    currentStep == 6 -> "Publish profile"
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
                    active -> MaterialTheme.colorScheme.primary
                    complete -> SurfaceSoft
                    else -> MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary else Divider)
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    if (complete) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp), tint = Success)
                    } else {
                        Text(
                            index.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) MaterialTheme.colorScheme.onPrimary else TextSecondary,
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
            resolved.gender,
            resolved.religion,
            resolved.caste,
            resolved.motherTongue,
            resolved.maritalStatus
        ).all { it.isNotBlank() }
        2 -> (resolved.heightCm ?: 0) > 0 &&
            (resolved.weightKg ?: 0) > 0 &&
            resolved.complexion.isNotBlank() &&
            resolved.bodyType.isNotBlank() &&
            resolved.bloodGroup.isNotBlank()
        3 -> resolved.educationLevel.isNotBlank() &&
            resolved.occupation.isNotBlank() &&
            resolved.annualIncome.isNotBlank() &&
            resolved.workingCity.isNotBlank()
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
        6 -> resolved.rashi.isNotBlank() ||
            resolved.nakshatra.isNotBlank() ||
            resolved.birthCity.isNotBlank() ||
            resolved.gotra.isNotBlank() ||
            resolved.isManglik
        else -> false
    }
}

@Composable
private fun Step1BasicInfo(existing: ProfileData?, vm: ProfileViewModel, onValidityChange: (Boolean) -> Unit) {
    var firstName by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.firstName.orEmpty()) }
    var lastName by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.lastName.orEmpty()) }
    var dob by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.dob.orEmpty()) }
    var religion by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.religion.orEmpty()) }
    var caste by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.caste.orEmpty()) }
    var motherTongue by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.motherTongue.orEmpty()) }
    var maritalStatus by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.maritalStatus?.ifBlank { "never_married" } ?: "never_married") }
    var gender by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.gender?.ifBlank { "male" } ?: "male") }
    var profileCreatedBy by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.profileCreatedBy?.ifBlank { "self" } ?: "self") }

    val isValid = listOf(firstName, lastName, dob, gender, religion, caste, motherTongue, maritalStatus).all { it.isNotBlank() }
    LaunchedEffect(firstName, lastName, dob, gender, religion, caste, motherTongue, maritalStatus, profileCreatedBy) {
        vm.updateStep1Data(
            mapOf(
                "firstName" to firstName,
                "lastName" to lastName,
                "dob" to dob,
                "gender" to gender,
                "religion" to religion,
                "caste" to caste,
                "motherTongue" to motherTongue,
                "maritalStatus" to maritalStatus,
                "profileCreatedBy" to profileCreatedBy
            )
        )
        onValidityChange(isValid)
    }

    PremiumCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionLead("Identity fields", "These details make the profile searchable and understandable to families.")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RequiredTextField(firstName, { firstName = it }, "First name", Modifier.weight(1f))
                RequiredTextField(lastName, { lastName = it }, "Last name", Modifier.weight(1f))
            }
            RequiredTextField(dob, { dob = it }, "Date of birth (YYYY-MM-DD)", keyboardType = KeyboardType.Text)
            ChipRow("Gender", listOf("male", "female"), gender) { gender = it }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RequiredTextField(religion, { religion = it }, "Religion", Modifier.weight(1f))
                RequiredTextField(caste, { caste = it }, "Community / caste", Modifier.weight(1f))
            }
            RequiredTextField(motherTongue, { motherTongue = it }, "Mother tongue")
            ChipRow("Marital status", listOf("never_married", "divorced", "widowed"), maritalStatus) { maritalStatus = it }
            ChipRow("Profile created by", listOf("self", "mediator"), profileCreatedBy) { profileCreatedBy = it }
        }
    }
}

@Composable
private fun Step2Physical(existing: ProfileData?, vm: ProfileViewModel, onValidityChange: (Boolean) -> Unit) {
    var heightCm by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.heightCm?.toString().orEmpty()) }
    var weightKg by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.weightKg?.toString().orEmpty()) }
    var complexion by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.complexion.orEmpty()) }
    var bodyType by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.bodyType.orEmpty()) }
    var bloodGroup by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.bloodGroup.orEmpty()) }

    val isValid = heightCm.isNotBlank() && weightKg.isNotBlank() && complexion.isNotBlank() && bodyType.isNotBlank() && bloodGroup.isNotBlank()
    LaunchedEffect(heightCm, weightKg, complexion, bodyType, bloodGroup) {
        vm.updateStep2Data(
            mapOf(
                "heightCm" to heightCm.toIntOrNull().orZero(),
                "weightKg" to weightKg.toIntOrNull().orZero(),
                "complexion" to complexion,
                "bodyType" to bodyType,
                "bloodGroup" to bloodGroup
            )
        )
        onValidityChange(isValid)
    }

    PremiumCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionLead("Physical details", "Keep these easy to filter without making the user hunt through the profile.")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(heightCm, { heightCm = it.filter(Char::isDigit) }, "Height in cm", Modifier.weight(1f))
                NumberField(weightKg, { weightKg = it.filter(Char::isDigit) }, "Weight in kg", Modifier.weight(1f))
            }
            ChipRow("Complexion", listOf("Fair", "Wheatish", "Dusky"), complexion) { complexion = it }
            ChipRow("Body type", listOf("Slim", "Average", "Athletic"), bodyType) { bodyType = it }
            ChipRow("Blood group", listOf("A+", "B+", "O+", "AB+"), bloodGroup) { bloodGroup = it }
            SignalChips(listOf("Improves filter accuracy", "Visible in full profile"), tone = ChipTone.Info)
        }
    }
}

@Composable
private fun Step3Education(existing: ProfileData?, vm: ProfileViewModel, onValidityChange: (Boolean) -> Unit) {
    var educationLevel by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.educationLevel.orEmpty()) }
    var occupation by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.occupation.orEmpty()) }
    var annualIncome by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.annualIncome.orEmpty()) }
    var workingCity by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.workingCity.orEmpty()) }

    val isValid = educationLevel.isNotBlank() && occupation.isNotBlank() && annualIncome.isNotBlank() && workingCity.isNotBlank()
    LaunchedEffect(educationLevel, occupation, annualIncome, workingCity) {
        vm.updateStep3Data(
            mapOf(
                "educationLevel" to educationLevel,
                "occupation" to occupation,
                "annualIncome" to annualIncome,
                "workingCity" to workingCity
            )
        )
        onValidityChange(isValid)
    }

    PremiumCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionLead("Education and work", "These are among the highest-intent search filters in a matrimony app.")
            ChipRow("Education level", listOf("Graduate", "Post Graduate", "Doctorate", "MBA", "Professional"), educationLevel) { educationLevel = it }
            RequiredTextField(occupation, { occupation = it }, "Occupation")
            ChipRow("Annual income", listOf("< 3 LPA", "3-5 LPA", "5-10 LPA", "10-20 LPA", "20+ LPA"), annualIncome) { annualIncome = it }
            RequiredTextField(workingCity, { workingCity = it }, "Working city")
            PremiumCard(containerColor = SurfaceWarm, contentPadding = PaddingValues(14.dp)) {
                Text("This section syncs with Smart Search and ranking, so it should be specific instead of generic.", style = MaterialTheme.typography.bodySmall, color = PrimaryDark)
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

    val isValid = listOf(fatherOccupation, motherOccupation, numBrothers, numSisters, familyType, familyCity).all { it.isNotBlank() }
    LaunchedEffect(fatherOccupation, motherOccupation, numBrothers, numSisters, familyType, familyCity) {
        vm.updateStep4Data(
            mapOf(
                "fatherOccupation" to fatherOccupation,
                "motherOccupation" to motherOccupation,
                "numBrothers" to numBrothers.toIntOrNull().orZero(),
                "numSisters" to numSisters.toIntOrNull().orZero(),
                "familyType" to familyType,
                "familyCity" to familyCity
            )
        )
        onValidityChange(isValid)
    }

    PremiumCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionLead("Family background", "This is where SoulMatch should feel like a serious matrimonial product.")
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
            SectionLead("Lifestyle and introduction", "A warm, specific profile usually gets better response quality.")
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
            PremiumCard(containerColor = SurfaceWarm, contentPadding = PaddingValues(14.dp)) {
                Text("Prompt idea: mention your family rhythm, future city flexibility, hobbies, and what kind of partnership you want.", style = MaterialTheme.typography.bodySmall, color = PrimaryDark)
            }
        }
    }
}

@Composable
private fun Step6Horoscope(existing: ProfileData?, vm: ProfileViewModel, onValidityChange: (Boolean) -> Unit) {
    var rashi by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.rashi.orEmpty()) }
    var nakshatra by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.nakshatra.orEmpty()) }
    var isManglik by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.isManglik ?: false) }
    var birthCity by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.birthCity.orEmpty()) }
    var gotra by rememberSaveable(existing?.profileId) { mutableStateOf(existing?.gotra.orEmpty()) }

    LaunchedEffect(rashi, nakshatra, isManglik, birthCity, gotra) {
        vm.updateStep6Data(
            mapOf(
                "rashi" to rashi,
                "nakshatra" to nakshatra,
                "isManglik" to isManglik,
                "birthCity" to birthCity,
                "gotra" to gotra
            )
        )
        onValidityChange(true)
    }

    PremiumCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionLead("Horoscope and traditions", "Optional details stay respectful but visible for families who care about them.")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(rashi, { rashi = it }, label = { Text("Rashi") }, modifier = Modifier.weight(1f))
                OutlinedTextField(nakshatra, { nakshatra = it }, label = { Text("Nakshatra") }, modifier = Modifier.weight(1f))
            }
            ChipRow("Manglik", listOf("yes", "no"), if (isManglik) "yes" else "no") { isManglik = it == "yes" }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(birthCity, { birthCity = it }, label = { Text("Birth city") }, modifier = Modifier.weight(1f))
                OutlinedTextField(gotra, { gotra = it }, label = { Text("Gotra") }, modifier = Modifier.weight(1f))
            }
            SignalChips(listOf("Optional section", "Adds tradition context", "Improves compatibility notes"), tone = ChipTone.Info)
        }
    }
}

@Composable
private fun SectionLead(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(title: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$title *", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChoiceChip(
                    label = titleCase(option),
                    selected = selected.equals(option, ignoreCase = true),
                    onClick = { onSelect(option) }
                )
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
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("$label *") },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true
    )
}

@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("$label *") },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}

private fun Int?.orZero(): Int = this ?: 0

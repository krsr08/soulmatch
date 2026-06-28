package com.soulmatch.app.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.data.models.PartnerPreferencesData
import com.soulmatch.app.data.models.ProfileData
import com.soulmatch.app.ui.screens.design.DesignHotspot
import com.soulmatch.app.ui.screens.design.ExactDesignScreen
import com.soulmatch.app.ui.screens.design.backHotspot
import com.soulmatch.app.ui.viewmodels.ProfileViewModel

@Composable
fun ExactProfileWizardScreen(
    step: Int,
    onNextStep: (Int) -> Unit,
    onBack: () -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val profile by vm.profile.collectAsStateWithLifecycle()
    val preferences by vm.partnerPreferences.collectAsStateWithLifecycle()
    val isSaving by vm.isSaving.collectAsStateWithLifecycle()
    val errorMessage by vm.errorMessage.collectAsStateWithLifecycle()
    var showEditor by remember(step) { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        errorMessage?.takeIf { it.isNotBlank() }?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            vm.clearError()
        }
    }

    val assetName = when (step.coerceIn(1, 6)) {
        1 -> "11_basic_details_screen.png"
        2 -> "12_religious_and_community_details_screen.png"
        3 -> "13_education_and_career_screen.png"
        4 -> "14_family_details_screen.png"
        5 -> "15_lifestyle_details_screen.png"
        else -> "16_partner_preferences_screen.png"
    }

    ExactDesignScreen(
        assetName = assetName,
        hotspots = listOf(
            backHotspot(onBack),
            DesignHotspot(18f, 214f, 354f, 470f) { showEditor = true },
            DesignHotspot(28f, 754f, 334f, 58f) {
                vm.saveStep(step.coerceIn(1, 6)) { onNextStep(step.coerceIn(1, 6) + 1) }
            }
        )
    )

    if (showEditor) {
        when (step.coerceIn(1, 6)) {
            1 -> Step1Editor(profile, onDismiss = { showEditor = false }) {
                vm.updateStep1Data(it)
                showEditor = false
            }
            2 -> Step2Editor(profile, onDismiss = { showEditor = false }) {
                vm.updateStep2Data(it)
                showEditor = false
            }
            3 -> Step3Editor(profile, onDismiss = { showEditor = false }) {
                vm.updateStep3Data(it)
                showEditor = false
            }
            4 -> Step4Editor(profile, onDismiss = { showEditor = false }) {
                vm.updateStep4Data(it)
                showEditor = false
            }
            5 -> Step5Editor(profile, onDismiss = { showEditor = false }) {
                vm.updateStep5Data(it)
                showEditor = false
            }
            else -> Step6Editor(profile, preferences, onDismiss = { showEditor = false }) {
                vm.updatePartnerPreferences(it)
                showEditor = false
            }
        }
    }

    if (isSaving) {
        Toast.makeText(context, "Saving profile...", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun Step1Editor(
    profile: ProfileData?,
    onDismiss: () -> Unit,
    onSave: (Map<String, Any>) -> Unit
) {
    var firstName by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.firstName.orEmpty()) }
    var lastName by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.lastName.orEmpty()) }
    var dob by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.dob.orEmpty()) }
    var gender by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.gender.orEmpty()) }
    StepDialog(
        title = "Basic details",
        onDismiss = onDismiss,
        onSave = {
            onSave(
                mapOf(
                    "firstName" to firstName.trim(),
                    "lastName" to lastName.trim(),
                    "dob" to dob.trim(),
                    "gender" to gender.trim()
                )
            )
        }
    ) {
        Field("First name", firstName) { firstName = it }
        Field("Last name", lastName) { lastName = it }
        Field("Date of birth (DD-MM-YYYY)", dob) { dob = it }
        Field("Gender", gender) { gender = it }
    }
}

@Composable
private fun Step2Editor(
    profile: ProfileData?,
    onDismiss: () -> Unit,
    onSave: (Map<String, Any>) -> Unit
) {
    var religion by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.religion.orEmpty()) }
    var caste by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.caste.orEmpty()) }
    var motherTongue by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.motherTongue.orEmpty()) }
    var maritalStatus by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.maritalStatus.orEmpty()) }
    StepDialog(
        title = "Religious and community",
        onDismiss = onDismiss,
        onSave = {
            onSave(
                mapOf(
                    "religion" to religion.trim(),
                    "caste" to caste.trim(),
                    "motherTongue" to motherTongue.trim(),
                    "maritalStatus" to maritalStatus.trim()
                )
            )
        }
    ) {
        Field("Religion", religion) { religion = it }
        Field("Community / caste", caste) { caste = it }
        Field("Mother tongue", motherTongue) { motherTongue = it }
        Field("Marital status", maritalStatus) { maritalStatus = it }
    }
}

@Composable
private fun Step3Editor(
    profile: ProfileData?,
    onDismiss: () -> Unit,
    onSave: (Map<String, Any>) -> Unit
) {
    var educationLevel by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.educationLevel.orEmpty()) }
    var isEmployed by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.isEmployed ?: true) }
    var occupation by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.occupation.orEmpty()) }
    var annualIncome by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.annualIncome.orEmpty()) }
    var workingCity by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.workingCity.orEmpty()) }
    var workingState by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.workingState.orEmpty()) }
    var workingPincode by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.workingPincode.orEmpty()) }
    StepDialog(
        title = "Education and career",
        onDismiss = onDismiss,
        onSave = {
            onSave(
                mapOf(
                    "educationLevel" to educationLevel.trim(),
                    "isEmployed" to isEmployed,
                    "occupation" to occupation.trim(),
                    "annualIncome" to annualIncome.trim(),
                    "workingCity" to workingCity.trim(),
                    "workingState" to workingState.trim(),
                    "workingPincode" to workingPincode.trim()
                )
            )
        }
    ) {
        Field("Education level", educationLevel) { educationLevel = it }
        ToggleField("Currently employed", isEmployed) { isEmployed = it }
        Field("Occupation", occupation) { occupation = it }
        Field("Annual income", annualIncome) { annualIncome = it }
        Field("Working city", workingCity) { workingCity = it }
        Field("Working state", workingState) { workingState = it }
        Field("Working pincode", workingPincode, KeyboardType.Number) { workingPincode = it.filter(Char::isDigit).take(6) }
    }
}

@Composable
private fun Step4Editor(
    profile: ProfileData?,
    onDismiss: () -> Unit,
    onSave: (Map<String, Any>) -> Unit
) {
    var familyType by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.familyType.orEmpty()) }
    var fatherOccupation by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.fatherOccupation.orEmpty()) }
    var motherOccupation by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.motherOccupation.orEmpty()) }
    var numBrothers by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.numBrothers?.toString().orEmpty()) }
    var numSisters by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.numSisters?.toString().orEmpty()) }
    var familyCity by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.familyCity.orEmpty()) }
    var familyState by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.familyState.orEmpty()) }
    var familyLocality by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.familyLocality.orEmpty()) }
    var familyPincode by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.familyPincode.orEmpty()) }
    StepDialog(
        title = "Family details",
        onDismiss = onDismiss,
        onSave = {
            onSave(
                mapOf(
                    "familyType" to familyType.trim(),
                    "fatherOccupation" to fatherOccupation.trim(),
                    "motherOccupation" to motherOccupation.trim(),
                    "numBrothers" to (numBrothers.toIntOrNull() ?: 0),
                    "numSisters" to (numSisters.toIntOrNull() ?: 0),
                    "familyCity" to familyCity.trim(),
                    "familyState" to familyState.trim(),
                    "familyLocality" to familyLocality.trim(),
                    "familyPincode" to familyPincode.trim()
                )
            )
        }
    ) {
        Field("Family type", familyType) { familyType = it }
        Field("Father occupation", fatherOccupation) { fatherOccupation = it }
        Field("Mother occupation", motherOccupation) { motherOccupation = it }
        Field("Brothers", numBrothers, KeyboardType.Number) { numBrothers = it.filter(Char::isDigit).take(2) }
        Field("Sisters", numSisters, KeyboardType.Number) { numSisters = it.filter(Char::isDigit).take(2) }
        Field("Family city", familyCity) { familyCity = it }
        Field("Family state", familyState) { familyState = it }
        Field("Locality", familyLocality) { familyLocality = it }
        Field("Pincode", familyPincode, KeyboardType.Number) { familyPincode = it.filter(Char::isDigit).take(6) }
    }
}

@Composable
private fun Step5Editor(
    profile: ProfileData?,
    onDismiss: () -> Unit,
    onSave: (Map<String, Any>) -> Unit
) {
    var diet by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.diet.orEmpty()) }
    var smoking by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.smoking.orEmpty()) }
    var drinking by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.drinking.orEmpty()) }
    var aboutMe by rememberSaveable(profile?.profileId) { mutableStateOf(profile?.aboutMe.orEmpty()) }
    StepDialog(
        title = "Lifestyle details",
        onDismiss = onDismiss,
        onSave = {
            onSave(
                mapOf(
                    "diet" to diet.trim(),
                    "smoking" to smoking.trim(),
                    "drinking" to drinking.trim(),
                    "aboutMe" to aboutMe.trim()
                )
            )
        }
    ) {
        Field("Diet", diet) { diet = it }
        Field("Smoking", smoking) { smoking = it }
        Field("Drinking", drinking) { drinking = it }
        Field("About me", aboutMe, singleLine = false, minLines = 4) { aboutMe = it }
    }
}

@Composable
private fun Step6Editor(
    profile: ProfileData?,
    preferences: PartnerPreferencesData,
    onDismiss: () -> Unit,
    onSave: (PartnerPreferencesData) -> Unit
) {
    var ageMin by rememberSaveable(profile?.profileId) { mutableStateOf(preferences.ageMin.toString()) }
    var ageMax by rememberSaveable(profile?.profileId) { mutableStateOf(preferences.ageMax.toString()) }
    var religion by rememberSaveable(profile?.profileId) { mutableStateOf(preferences.religion.orEmpty()) }
    var education by rememberSaveable(profile?.profileId) { mutableStateOf(preferences.educationLevels.joinToString(", ")) }
    var locations by rememberSaveable(profile?.profileId) { mutableStateOf(preferences.locations.joinToString(", ")) }
    var timeline by rememberSaveable(profile?.profileId) { mutableStateOf(preferences.timeline.orEmpty()) }
    StepDialog(
        title = "Partner preferences",
        onDismiss = onDismiss,
        onSave = {
            onSave(
                PartnerPreferencesData(
                    ageMin = ageMin.toIntOrNull() ?: 24,
                    ageMax = ageMax.toIntOrNull() ?: 32,
                    religion = religion.trim().ifBlank { null },
                    educationLevels = education.split(",").map { it.trim() }.filter { it.isNotBlank() },
                    locations = locations.split(",").map { it.trim() }.filter { it.isNotBlank() },
                    timeline = timeline.trim().ifBlank { null },
                    maritalStatuses = listOfNotNull(profile?.maritalStatus?.takeIf { it.isNotBlank() }),
                    familyTypes = listOfNotNull(profile?.familyType?.takeIf { it.isNotBlank() })
                )
            )
        }
    ) {
        Field("Age min", ageMin, KeyboardType.Number) { ageMin = it.filter(Char::isDigit).take(2) }
        Field("Age max", ageMax, KeyboardType.Number) { ageMax = it.filter(Char::isDigit).take(2) }
        Field("Religion", religion) { religion = it }
        Field("Education", education) { education = it }
        Field("Preferred locations", locations) { locations = it }
        Field("Timeline", timeline) { timeline = it }
    }
}

@Composable
private fun StepDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                content()
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun Field(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        minLines = minLines,
        textStyle = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun ToggleField(
    label: String,
    value: Boolean,
    onChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = value, onCheckedChange = onChange)
    }
}

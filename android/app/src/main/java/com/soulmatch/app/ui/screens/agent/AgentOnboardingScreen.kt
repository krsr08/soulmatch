package com.soulmatch.app.ui.screens.agent

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.data.models.AgentOnboardingRequest
import com.soulmatch.app.data.models.AgentProfileUpsertRequest
import com.soulmatch.app.data.models.AgentKycDocumentInput
import com.soulmatch.app.data.models.OrderData
import com.razorpay.Checkout
import com.soulmatch.app.ui.viewmodels.AgentViewModel
import java.util.Locale
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@Composable
fun AgentOnboardingScreen(
    onBack: () -> Unit,
    onCompleted: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenAccount: () -> Unit,
    onDrawerDestination: (AgentDrawerDestination) -> Unit,
    vm: AgentViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val agentProfile = state.agentProfile

    var step by remember { mutableStateOf(1) }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var stateName by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }
    var languagesText by remember { mutableStateOf("") }
    var yearsExperience by remember { mutableStateOf("3") }
    var termsAccepted by remember { mutableStateOf(false) }
    var aadhaarUri by remember { mutableStateOf<Uri?>(null) }
    var panUri by remember { mutableStateOf<Uri?>(null) }
    var chequeUri by remember { mutableStateOf<Uri?>(null) }
    var startPennyAfterDraftSave by remember { mutableStateOf(false) }

    val termsScrollState = rememberScrollState()
    var termsHasReachedBottom by remember { mutableStateOf(false) }
    LaunchedEffect(termsScrollState.value, termsScrollState.maxValue) {
        if (termsScrollState.maxValue > 0 && termsScrollState.value >= termsScrollState.maxValue - 4) {
            termsHasReachedBottom = true
        }
    }

    LaunchedEffect(agentProfile?.advisorId, agentProfile?.phone, agentProfile?.email) {
        if (agentProfile == null) return@LaunchedEffect
        fullName = agentProfile.fullName
        phone = agentProfile.phone
        email = agentProfile.email
        city = agentProfile.city
        stateName = agentProfile.state
        businessName = agentProfile.businessName
        referralCode = agentProfile.referralCode
        languagesText = agentProfile.languages.joinToString(", ")
        yearsExperience = agentProfile.yearsExperience.takeIf { it > 0 }?.toString().orEmpty().ifBlank { "3" }
        termsAccepted = !agentProfile.termsAcceptedAt.isNullOrBlank()
    }

    val aadhaarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        aadhaarUri = uri
    }
    val panPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        panUri = uri
    }
    val chequePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        chequeUri = uri
    }

    val existingDocumentTypes = agentProfile?.kycDocuments.orEmpty()
        .map { it.documentType.lowercase(Locale.US) }
        .toSet()
    val aadhaarReady = aadhaarUri != null || existingDocumentTypes.contains("aadhaar")
    val panReady = panUri != null || existingDocumentTypes.contains("pan")
    val chequeReady = chequeUri != null || existingDocumentTypes.contains("cancelled_cheque")
    val termsReady = termsAccepted || !agentProfile?.termsAcceptedAt.isNullOrBlank()
    val pennyDropComplete = agentProfile?.pennyDropStatus in listOf("paid", "verified")
    val canStartPennyDrop = agentProfile?.advisorId != null && chequeReady && !pennyDropComplete && !state.processingPennyDrop

    val languagesReady = languagesText.split(',').map { it.trim() }.any { it.isNotBlank() }
    val yearsExperienceReady = yearsExperience.toIntOrNull()?.let { it in 0..50 } == true
    val detailsReady = fullName.isNotBlank() &&
        phone.isNotBlank() &&
        city.isNotBlank() &&
        stateName.isNotBlank() &&
        businessName.isNotBlank() &&
        yearsExperienceReady &&
        languagesReady

    fun existingKycInputs(): List<AgentKycDocumentInput> = agentProfile?.kycDocuments
        ?.mapNotNull { document ->
            document.fileUrl.takeIf { it.isNotBlank() }?.let {
                AgentKycDocumentInput(
                    documentType = document.documentType,
                    documentSide = document.documentSide,
                    fileUrl = it
                )
            }
        }
        .orEmpty()

    fun onboardingRequest(acceptTerms: Boolean) = AgentOnboardingRequest(
        fullName = fullName,
        phone = phone,
        email = email,
        city = city,
        state = stateName,
        businessName = businessName,
        referralCode = referralCode,
        yearsExperience = yearsExperience.toIntOrNull() ?: 0,
        languages = languagesText.toCsvList(),
        termsAccepted = acceptTerms,
        kycDocuments = existingKycInputs()
    )

    fun documentParts(): List<MultipartBody.Part> = listOfNotNull(
        aadhaarUri?.let { context.toAgentDocumentPart(it, "documents", 0) },
        panUri?.let { context.toAgentDocumentPart(it, "documents", 1) },
        chequeUri?.let { context.toAgentDocumentPart(it, "documents", 2) }
    )

    fun documentMeta(): List<Map<String, String>> = buildList {
        aadhaarUri?.let { add(mapOf("documentType" to "aadhaar", "documentSide" to "single")) }
        panUri?.let { add(mapOf("documentType" to "pan", "documentSide" to "single")) }
        chequeUri?.let { add(mapOf("documentType" to "cancelled_cheque", "documentSide" to "single")) }
    }

    fun submitOnboardingStep(acceptTerms: Boolean, onSaved: () -> Unit) {
        val documents = documentParts()
        val request = onboardingRequest(acceptTerms)
        if (documents.isNotEmpty()) {
            vm.submitOnboardingWithDocuments(
                request = request,
                documents = documents,
                documentMeta = documentMeta(),
                onCompleted = onSaved
            )
        } else {
            vm.submitOnboarding(request, onCompleted = onSaved)
        }
    }

    LaunchedEffect(canStartPennyDrop, startPennyAfterDraftSave) {
        if (startPennyAfterDraftSave && canStartPennyDrop) {
            startPennyAfterDraftSave = false
            vm.startPennyDropVerification()
        }
    }

    LaunchedEffect(state.pennyCheckout?.order?.orderId) {
        val checkout = state.pennyCheckout ?: return@LaunchedEffect
        if (checkout.order.gateway == "mock") {
            vm.failPennyCheckout("Mock bank verification is not enabled for agent onboarding.")
            return@LaunchedEffect
        }
        val activity = context as? Activity
        if (activity == null) {
            vm.failPennyCheckout("Could not open the payment window.")
            return@LaunchedEffect
        }
        if (!checkout.order.keyId.startsWith("rzp_")) {
            vm.failPennyCheckout("SoulMatch bank verification payments are not configured correctly.")
            return@LaunchedEffect
        }
        try {
            Checkout()
                .apply { setKeyID(checkout.order.keyId) }
                .open(activity, agentPennyDropCheckoutOptions(checkout.order))
            vm.markPennyCheckoutConsumed()
        } catch (_: Exception) {
            vm.failPennyCheckout("Payment checkout could not be started.")
        }
    }

    val stepValid = when (step) {
        1 -> detailsReady
        2 -> aadhaarReady && panReady
        3 -> chequeReady
        4 -> true
        5 -> termsReady
        else -> true
    }

    fun primaryAction() {
        when (step) {
            1, 2 -> step += 1
            3 -> submitOnboardingStep(acceptTerms = false) { step = 4 }
            4 -> {
                when {
                    pennyDropComplete -> step = 5
                    canStartPennyDrop -> vm.startPennyDropVerification()
                    else -> {
                        startPennyAfterDraftSave = true
                        submitOnboardingStep(acceptTerms = false) { step = 4 }
                    }
                }
            }
            5 -> submitOnboardingStep(acceptTerms = true) { step = 6 }
        }
    }

    AgentScaffold(
        title = "SoulMatch",
        selectedTab = AgentTab.Account,
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
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                AgentRegistrationProgress(step = step)
                when (step) {
                    1 -> AgentRegistrationBasicStep(
                        fullName = fullName,
                        onFullNameChange = { fullName = it },
                        phone = phone,
                        onPhoneChange = { phone = it },
                        email = email,
                        onEmailChange = { email = it },
                        city = city,
                        onCityChange = { city = it },
                        stateName = stateName,
                        onStateChange = { stateName = it },
                        businessName = businessName,
                        onBusinessNameChange = { businessName = it },
                        referralCode = referralCode,
                        onReferralCodeChange = { referralCode = it },
                        yearsExperience = yearsExperience,
                        onYearsExperienceChange = { yearsExperience = it },
                        languagesText = languagesText,
                        onLanguagesChange = { languagesText = it }
                    )
                    2 -> AgentRegistrationKycStep(
                        aadhaarReady = aadhaarReady,
                        panReady = panReady,
                        aadhaarLabel = aadhaarUri?.let { context.fileLabelFor(it) }
                            ?: currentDocumentLabel(agentProfile?.kycDocuments?.firstOrNull { it.documentType == "aadhaar" }),
                        panLabel = panUri?.let { context.fileLabelFor(it) }
                            ?: currentDocumentLabel(agentProfile?.kycDocuments?.firstOrNull { it.documentType == "pan" }),
                        onPickAadhaar = { aadhaarPicker.launch(arrayOf("image/*", "application/pdf")) },
                        onPickPan = { panPicker.launch(arrayOf("image/*", "application/pdf")) }
                    )
                    3 -> AgentRegistrationChequeStep(
                        chequeReady = chequeReady,
                        chequeLabel = chequeUri?.let { context.fileLabelFor(it) }
                            ?: currentDocumentLabel(agentProfile?.kycDocuments?.firstOrNull { it.documentType == "cancelled_cheque" }),
                        bankName = agentProfile?.bankName.orEmpty(),
                        accountLast4 = agentProfile?.bankAccountLast4.orEmpty(),
                        ifsc = agentProfile?.bankIfsc.orEmpty(),
                        bankStatus = agentProfile?.bankVerificationStatus ?: "not_started",
                        onPickCheque = { chequePicker.launch(arrayOf("image/*", "application/pdf")) }
                    )
                    4 -> AgentRegistrationPennyStep(
                        pennyDropComplete = pennyDropComplete,
                        canStartPennyDrop = canStartPennyDrop,
                        processing = state.processingPennyDrop || state.saving,
                        orderId = agentProfile?.pennyDropOrderId.orEmpty()
                    )
                    5 -> AgentRegistrationTermsStep(
                        accepted = termsAccepted,
                        locked = !agentProfile?.termsAcceptedAt.isNullOrBlank(),
                        scrollState = termsScrollState,
                        canAccept = termsHasReachedBottom || termsAccepted,
                        onAcceptedChange = { checked ->
                            if (termsHasReachedBottom || termsAccepted) termsAccepted = checked
                        }
                    )
                    6 -> AgentRegistrationSuccessStep(
                        city = city,
                        aadhaarVerified = agentProfile?.aadhaarVerificationStatus == "verified" || aadhaarReady,
                        bankVerified = pennyDropComplete || agentProfile?.bankVerificationStatus == "verified",
                        onOpenDashboard = onCompleted,
                        onOpenProfile = onOpenAccount
                    )
                }

                state.error?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = AgentColorsAccent, fontWeight = FontWeight.SemiBold)
                }
                state.saveMessage?.takeIf { it.isNotBlank() && step != 6 }?.let {
                    Text(it, color = Color(0xFF0F8D57), fontWeight = FontWeight.Medium)
                }
            }

            if (step < 6) {
                AgentRegistrationFooter(
                    step = step,
                    primaryLabel = when {
                        state.saving -> "Saving..."
                        step == 2 -> "Verify Documents"
                        step == 3 -> "Confirm Bank Details"
                        step == 4 && pennyDropComplete -> "Continue"
                        step == 4 -> "Pay ₹1 via Razorpay"
                        step == 5 -> "I Agree & Continue"
                        else -> "Continue"
                    },
                    enabled = stepValid && !state.saving && !state.processingPennyDrop,
                    onBack = {
                        if (step == 1) onBack() else step -= 1
                    },
                    onPrimary = ::primaryAction
                )
            }
        }
    }
}

@Composable
private fun AgentRegistrationProgress(step: Int) {
    val progress = step.coerceIn(1, 6) / 6f
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "STEP $step OF 6",
                color = AgentColorsAccent,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                if (step == 6) "100% Complete" else "${(progress * 100).toInt()}% Complete",
                color = AgentColorsMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp),
            color = AgentColorsAccent,
            trackColor = Color(0xFFEADBD6)
        )
    }
}

@Composable
private fun AgentRegistrationBasicStep(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    stateName: String,
    onStateChange: (String) -> Unit,
    businessName: String,
    onBusinessNameChange: (String) -> Unit,
    referralCode: String,
    onReferralCodeChange: (String) -> Unit,
    yearsExperience: String,
    onYearsExperienceChange: (String) -> Unit,
    languagesText: String,
    onLanguagesChange: (String) -> Unit
) {
    val languageOptions = listOf("Hindi", "English", "Telugu", "Tamil", "Kannada", "Marathi")
    val selectedLanguages = languagesText.toCsvList().map { it.lowercase(Locale.US) }.toSet()

    AgentRegistrationHero(
        eyebrow = "Trusted agents, real connections",
        title = "Join as a SoulMatch Agent",
        body = "Complete official verification once, then manage member profiles with confidence."
    )

    RegistrationCard {
        AgentTextField("Full Name", fullName, onValueChange = onFullNameChange)
        AgentTextField(
            label = "Mobile Number",
            value = phone,
            keyboardType = KeyboardType.Phone,
            onValueChange = { value -> onPhoneChange(value.filter { it.isDigit() }.take(10)) }
        )
        AgentTextField(
            label = "Professional Email",
            value = email,
            keyboardType = KeyboardType.Email,
            onValueChange = onEmailChange
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                AgentTextField("City", city, onValueChange = onCityChange)
            }
            Box(modifier = Modifier.weight(1f)) {
                AgentTextField("State", stateName, onValueChange = onStateChange)
            }
        }
        AgentTextField("Business / Agency Name", businessName, onValueChange = onBusinessNameChange)
        AgentTextField("Referral Code (Optional)", referralCode, onValueChange = onReferralCodeChange)

        Text("Years of experience", color = Color(0xFF2C1810), fontWeight = FontWeight.SemiBold)
        ExperienceStepper(
            value = yearsExperience.toIntOrNull() ?: 0,
            onChange = { onYearsExperienceChange(it.coerceIn(0, 50).toString()) }
        )

        Text("Languages spoken", color = Color(0xFF2C1810), fontWeight = FontWeight.SemiBold)
        FlowChipRows(
            options = languageOptions,
            selected = selectedLanguages,
            onToggle = { language ->
                val current = languagesText.toCsvList().toMutableList()
                val index = current.indexOfFirst { it.equals(language, ignoreCase = true) }
                if (index >= 0) current.removeAt(index) else current.add(language)
                onLanguagesChange(current.joinToString(", "))
            }
        )
    }
}

@Composable
private fun AgentRegistrationKycStep(
    aadhaarReady: Boolean,
    panReady: Boolean,
    aadhaarLabel: String,
    panLabel: String,
    onPickAadhaar: () -> Unit,
    onPickPan: () -> Unit
) {
    AgentRegistrationHero(
        eyebrow = "Identity Verification",
        title = "Upload Aadhaar and PAN",
        body = "These documents help SoulMatch block impersonation and unsafe agent accounts."
    )
    RegistrationInfoBanner("Your documents are encrypted and stored securely. They are never shown publicly.")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AgentRegistrationUploadCard(
            modifier = Modifier.weight(1f),
            title = "Aadhaar Card",
            subtitle = "Tap to upload",
            selectedLabel = aadhaarLabel,
            ready = aadhaarReady,
            onPick = onPickAadhaar
        )
        AgentRegistrationUploadCard(
            modifier = Modifier.weight(1f),
            title = "PAN Card",
            subtitle = "Tap to upload",
            selectedLabel = panLabel,
            ready = panReady,
            onPick = onPickPan
        )
    }
}

@Composable
private fun AgentRegistrationChequeStep(
    chequeReady: Boolean,
    chequeLabel: String,
    bankName: String,
    accountLast4: String,
    ifsc: String,
    bankStatus: String,
    onPickCheque: () -> Unit
) {
    AgentRegistrationHero(
        eyebrow = "Bank Account Verification",
        title = "Upload cancelled cheque",
        body = "We extract bank details securely and validate the name against KYC records."
    )
    AgentRegistrationUploadCard(
        modifier = Modifier.fillMaxWidth(),
        title = "Cancelled Cheque",
        subtitle = "Printed name, JPG/PNG/PDF",
        selectedLabel = chequeLabel,
        ready = chequeReady,
        large = true,
        onPick = onPickCheque
    )
    RegistrationCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Auto-extracted details", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            RegistrationStatusPill(
                label = if (bankStatus == "verified") "Details verified" else "Review pending",
                success = bankStatus == "verified"
            )
        }
        ReadOnlyValueRow("Account holder", "Available after OCR review")
        ReadOnlyValueRow("Bank name", bankName.ifBlank { "Pending OCR" })
        ReadOnlyValueRow("Account number", accountLast4.ifBlank { "XXXX----" }.let { "XXXX$it" })
        ReadOnlyValueRow("IFSC code", ifsc.ifBlank { "Pending OCR" })
    }
}

@Composable
private fun AgentRegistrationPennyStep(
    pennyDropComplete: Boolean,
    canStartPennyDrop: Boolean,
    processing: Boolean,
    orderId: String
) {
    AgentRegistrationHero(
        eyebrow = "Penny Drop Verification",
        title = "One-tap bank verification",
        body = "Pay Rs 1 through Razorpay so we can confirm the bank account is real, active, and controlled by you."
    )
    RegistrationCard(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(Color(0xFFFFF2EC), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("Rs", color = AgentColorsAccent, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        }
        Text(
            if (pennyDropComplete) "Bank verified successfully" else "Pay Rs 1 to SoulMatch Verification",
            color = Color(0xFF2C1810),
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 23.sp,
            textAlign = TextAlign.Center
        )
        Text(
            when {
                pennyDropComplete && orderId.isNotBlank() -> "Transaction reference: $orderId"
                pennyDropComplete -> "Your bank verification is complete."
                processing -> "Opening secure payment window..."
                canStartPennyDrop -> "This one-time verification fee is not refundable."
                else -> "Save bank details first, then complete the Rs 1 verification."
            },
            color = AgentColorsMuted,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        RegistrationStatusPill(
            label = if (pennyDropComplete) "Bank verified" else "Verification pending",
            success = pennyDropComplete
        )
    }
}

@Composable
private fun AgentRegistrationTermsStep(
    accepted: Boolean,
    locked: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    canAccept: Boolean,
    onAcceptedChange: (Boolean) -> Unit
) {
    AgentRegistrationHero(
        eyebrow = "Agreement & Terms",
        title = "Read and sign digitally",
        body = "Acceptance is logged with timestamp, IP, device details, and active terms version."
    )
    RegistrationCard {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 260.dp, max = 360.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFFCFA),
            border = BorderStroke(1.dp, Color(0xFFE8D8D4))
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TermsClauseRow("Platform Disclaimer", "SoulMatch lists verified agents and member profiles. Offline promises, negotiations, and commitments remain the agent's responsibility.", expanded = true)
                TermsClauseRow("Advance Fee Ban", "No unapproved advance fee collection from members or families outside SoulMatch policy.")
                TermsClauseRow("KYC Data Usage", "Aadhaar, PAN, cheque and payment metadata are used for verification, fraud prevention, audit, and legal retention.")
                TermsClauseRow("Grievance Officer Contact", "All complaints must be routed through the official support and grievance channel inside the app.")
                TermsClauseRow("Data Retention Policy", "KYC records are retained under DPDP-aligned policy and removed according to offboarding rules.")
                Text(
                    "By agreeing, you accept full legal responsibility for your conduct as a SoulMatch agent.",
                    color = AgentColorsAccent,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = accepted,
                enabled = !locked && canAccept,
                onCheckedChange = onAcceptedChange
            )
            Text(
                when {
                    locked -> "Terms are already signed for this profile."
                    canAccept -> "I agree to SoulMatch agent terms."
                    else -> "Scroll to the bottom to activate agreement."
                },
                color = AgentColorsMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun AgentRegistrationSuccessStep(
    city: String,
    aadhaarVerified: Boolean,
    bankVerified: Boolean,
    onOpenDashboard: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE8D8D4)),
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .background(Color(0xFFFFF5DE), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFFB8960C), modifier = Modifier.size(48.dp))
            }
            Text(
                "Welcome aboard!",
                color = Color(0xFF2C1810),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                textAlign = TextAlign.Center
            )
            Text(
                "Your profile is now live for admin review. Once approved, users in ${city.ifBlank { "your city" }} can discover your agent profile.",
                color = AgentColorsMuted,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RegistrationStatusPill(if (aadhaarVerified) "ID uploaded" else "ID pending", aadhaarVerified)
                RegistrationStatusPill(if (bankVerified) "Bank linked" else "Bank pending", bankVerified)
            }
            Button(
                onClick = onOpenDashboard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AgentColorsAccent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Go to Dashboard", fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Outlined.ArrowForward, contentDescription = null)
            }
            Text(
                "Complete my profile first",
                modifier = Modifier.clickable(onClick = onOpenProfile),
                color = AgentColorsAccent,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AgentRegistrationFooter(
    step: Int,
    primaryLabel: String,
    enabled: Boolean,
    onBack: () -> Unit,
    onPrimary: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, Color(0xFFE8D8D4))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .height(52.dp)
                    .weight(0.36f)
                    .clickable(onClick = onBack),
                shape = RoundedCornerShape(14.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, Color(0xFFE5D6D2))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (step == 1) "Close" else "Back", color = AgentColorsAccent, fontWeight = FontWeight.Bold)
                }
            }
            Button(
                onClick = onPrimary,
                enabled = enabled,
                modifier = Modifier
                    .height(52.dp)
                    .weight(0.64f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AgentColorsAccent,
                    disabledContainerColor = Color(0xFFE8DDE0),
                    contentColor = Color.White,
                    disabledContentColor = Color(0xFF9B8D90)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(primaryLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AgentRegistrationHero(
    eyebrow: String,
    title: String,
    body: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(82.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFFFF5DE), Color(0xFFFFE9E1))
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("SM", color = AgentColorsAccent, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
        }
        Text(
            eyebrow,
            color = Color(0xFFB8960C),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center
        )
        Text(
            title,
            color = Color(0xFF2C1810),
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            textAlign = TextAlign.Center
        )
        Text(
            body,
            color = Color(0xFF6B4C3B),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun RegistrationCard(
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2D5CB)),
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun RegistrationInfoBanner(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFFF1D7),
        border = BorderStroke(1.dp, Color(0xFFEBD5A4))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Verified, contentDescription = null, tint = Color(0xFFB8960C), modifier = Modifier.size(20.dp))
            Text(text, color = Color(0xFF6B4C3B), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AgentRegistrationUploadCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    selectedLabel: String,
    ready: Boolean,
    large: Boolean = false,
    onPick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(if (large) 154.dp else 138.dp)
            .clickable(onClick = onPick),
        shape = RoundedCornerShape(18.dp),
        color = if (ready) Color(0xFFFFFCFA) else Color(0xFFFFF8F5),
        border = BorderStroke(1.2.dp, if (ready) Color(0xFFC17E6B) else Color(0xFFE6CFC7)),
        shadowElevation = if (ready) 3.dp else 0.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (ready) Icons.Outlined.CheckCircle else Icons.Outlined.CloudUpload,
                    contentDescription = null,
                    tint = if (ready) Color(0xFF4A7C59) else AgentColorsAccent,
                    modifier = Modifier.size(if (large) 32.dp else 26.dp)
                )
                Text(title, color = Color(0xFF2C1810), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(
                    if (ready) selectedLabel else subtitle,
                    color = AgentColorsMuted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
            if (ready) {
                Icon(
                    Icons.Outlined.Verified,
                    contentDescription = null,
                    tint = Color(0xFF4A7C59),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun RegistrationStatusPill(label: String, success: Boolean) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (success) Color(0xFFE8F7EE) else Color(0xFFFFF1D7),
        border = BorderStroke(1.dp, if (success) Color(0xFFCDE6D4) else Color(0xFFEBD5A4))
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = if (success) Color(0xFF2E6B3D) else Color(0xFF8A5F00),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ReadOnlyValueRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label.uppercase(Locale.US), color = AgentColorsMuted, style = MaterialTheme.typography.labelSmall, letterSpacing = 0.8.sp)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFFDF8F4),
            border = BorderStroke(1.dp, Color(0xFFE2D5CB))
        ) {
            Text(value, modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp), color = Color(0xFF2C1810))
        }
    }
}

@Composable
private fun TermsClauseRow(title: String, body: String, expanded: Boolean = false) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE8D8D4))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color(0xFF2C1810), fontWeight = FontWeight.SemiBold)
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = AgentColorsMuted)
            }
            if (expanded) {
                Text(body, color = AgentColorsMuted, style = MaterialTheme.typography.bodySmall, lineHeight = 19.sp)
            }
        }
    }
}

@Composable
private fun ExperienceStepper(value: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepperButton("-", enabled = value > 0) { onChange(value - 1) }
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFFDF8F4),
            border = BorderStroke(1.dp, Color(0xFFE2D5CB))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    if (value >= 30) "30+ years" else "$value years",
                    color = Color(0xFF2C1810),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        StepperButton("+", enabled = value < 50) { onChange(value + 1) }
    }
}

@Composable
private fun StepperButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(48.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) AgentColorsAccent else Color(0xFFE8DDE0)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = if (enabled) Color.White else Color(0xFF9B8D90), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }
}

@Composable
private fun FlowChipRows(
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { option ->
                    val isSelected = selected.contains(option.lowercase(Locale.US))
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clickable { onToggle(option) },
                        shape = RoundedCornerShape(999.dp),
                        color = if (isSelected) AgentColorsAccent else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) AgentColorsAccent else Color(0xFFE2D5CB))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                option,
                                color = if (isSelected) Color.White else Color(0xFF6B4C3B),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun LegacyAgentOnboardingScreen(
    onBack: () -> Unit,
    onCompleted: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenAccount: () -> Unit,
    onDrawerDestination: (AgentDrawerDestination) -> Unit,
    vm: AgentViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val agentProfile = state.agentProfile
    val isRegisteredAgent = agentProfile?.advisorId != null

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var stateName by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }
    var languagesText by remember { mutableStateOf("") }
    var yearsExperience by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }
    var aadhaarUri by remember { mutableStateOf<Uri?>(null) }
    var panUri by remember { mutableStateOf<Uri?>(null) }
    var chequeUri by remember { mutableStateOf<Uri?>(null) }
    val termsScrollState = rememberScrollState()
    val termsScrolledToBottom by remember {
        derivedStateOf { termsScrollState.maxValue == 0 || termsScrollState.value >= termsScrollState.maxValue - 4 }
    }

    LaunchedEffect(agentProfile?.advisorId, agentProfile?.phone, agentProfile?.email) {
        if (agentProfile == null) return@LaunchedEffect
        fullName = agentProfile.fullName
        phone = agentProfile.phone
        email = agentProfile.email
        city = agentProfile.city
        stateName = agentProfile.state
        businessName = agentProfile.businessName
        referralCode = agentProfile.referralCode
        languagesText = agentProfile.languages.joinToString(", ")
        yearsExperience = agentProfile.yearsExperience.takeIf { it > 0 }?.toString().orEmpty()
        termsAccepted = !agentProfile.termsAcceptedAt.isNullOrBlank()
    }

    val aadhaarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        aadhaarUri = uri
    }
    val panPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        panUri = uri
    }
    val chequePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        chequeUri = uri
    }
    val existingDocumentTypes = agentProfile?.kycDocuments.orEmpty()
        .map { it.documentType.lowercase(Locale.US) }
        .toSet()
    val aadhaarReady = aadhaarUri != null || existingDocumentTypes.contains("aadhaar")
    val panReady = panUri != null || existingDocumentTypes.contains("pan")
    val chequeReady = chequeUri != null || existingDocumentTypes.contains("cancelled_cheque")
    val termsReady = termsAccepted || !agentProfile?.termsAcceptedAt.isNullOrBlank()
    val reviewRejected = agentProfile?.kycStatus.equals("rejected", ignoreCase = true) ||
        agentProfile?.onboardingStatus.equals("rejected", ignoreCase = true)
    val aadhaarRejected = agentProfile?.aadhaarVerificationStatus.equals("rejected", ignoreCase = true)
    val panRejected = agentProfile?.panVerificationStatus.equals("rejected", ignoreCase = true)
    val bankRejected = agentProfile?.bankVerificationStatus.equals("rejected", ignoreCase = true)
    val aadhaarEditable = !isRegisteredAgent || reviewRejected || aadhaarRejected || !aadhaarReady
    val panEditable = !isRegisteredAgent || reviewRejected || panRejected || !panReady
    val chequeEditable = !isRegisteredAgent || reviewRejected || bankRejected || !chequeReady
    val termsEditable = !termsReady
    val docsEditable = aadhaarEditable || panEditable || chequeEditable
    val languagesReady = isRegisteredAgent || languagesText.split(',').map { it.trim() }.any { it.isNotBlank() }
    val yearsExperienceReady = yearsExperience.isBlank() || (yearsExperience.toIntOrNull()?.let { it >= 0 } == true)
    val detailsReady = fullName.isNotBlank() &&
        phone.isNotBlank() &&
        city.isNotBlank() &&
        stateName.isNotBlank() &&
        businessName.isNotBlank() &&
        yearsExperienceReady &&
        languagesReady
    val requiresFraudSubmit = !isRegisteredAgent || reviewRejected || !aadhaarReady || !panReady || !chequeReady || !termsReady
    val canSubmit = !state.saving && detailsReady && (!requiresFraudSubmit || (aadhaarReady && panReady && chequeReady && termsReady))
    val pennyCheckout = state.pennyCheckout
    val pennyDropComplete = agentProfile?.pennyDropStatus in listOf("paid", "verified")
    val canStartPennyDrop = agentProfile?.advisorId != null && chequeReady && !pennyDropComplete && !state.processingPennyDrop
    val afterSuccessfulSave = {
        if (!isRegisteredAgent) onCompleted()
    }

    LaunchedEffect(pennyCheckout?.order?.orderId) {
        val checkout = pennyCheckout ?: return@LaunchedEffect
        if (checkout.order.gateway == "mock") {
            vm.failPennyCheckout("Mock bank verification is not enabled for agent onboarding.")
            return@LaunchedEffect
        }
        val activity = context as? Activity
        if (activity == null) {
            vm.failPennyCheckout("Could not open the payment window.")
            return@LaunchedEffect
        }
        if (!checkout.order.keyId.startsWith("rzp_")) {
            vm.failPennyCheckout("SoulMatch bank verification payments are not configured correctly.")
            return@LaunchedEffect
        }
        try {
            Checkout()
                .apply { setKeyID(checkout.order.keyId) }
                .open(activity, agentPennyDropCheckoutOptions(checkout.order))
            vm.markPennyCheckoutConsumed()
        } catch (_: Exception) {
            vm.failPennyCheckout("Payment checkout could not be started.")
        }
    }

    AgentScaffold(
        title = "SoulMatch",
        selectedTab = AgentTab.Account,
        onOpenDashboard = onOpenDashboard,
        onOpenProfiles = onOpenProfiles,
        onOpenPlans = onOpenPlans,
        onOpenAccount = onOpenAccount,
        onDrawerDestination = onDrawerDestination
    ) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = AgentShapesCard,
                border = BorderStroke(1.dp, Color(0xFFF0E0DB))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        if (agentProfile == null) "Agent Registration" else "Update Agent Profile",
                        color = Color(0xFF2A1D20),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        "Keep your business details and KYC documents current so your dashboard and approvals stay in sync.",
                        color = AgentColorsMuted
                    )

                    AgentStatusSummary(
                        onboardingStatus = agentProfile?.onboardingStatus,
                        kycStatus = agentProfile?.kycStatus,
                        aadhaarStatus = agentProfile?.aadhaarVerificationStatus,
                        bankStatus = agentProfile?.bankVerificationStatus,
                        rejectionReason = agentProfile?.onboardingRejectionReason
                    )

                    AgentTextField("Full Name", fullName) { fullName = it }
                    AgentTextField("Business/Agency Name", businessName) { businessName = it }
                    AgentTextField("Professional Email", email) { email = it }
                    AgentTextField("Mobile Number", phone) { phone = it }
                    AgentTextField("City", city) { city = it }
                    AgentTextField("State", stateName) { stateName = it }
                    AgentTextField("Languages Spoken", languagesText) { languagesText = it }
                    AgentTextField(
                        label = "Years of Matrimony Experience",
                        value = yearsExperience,
                        keyboardType = KeyboardType.Number
                    ) { yearsExperience = it.filter { char -> char.isDigit() }.take(2) }
                    AgentTextField("Referral Code (Optional)", referralCode) { referralCode = it }

                    AgentFraudChecklist(
                        profile = agentProfile,
                        aadhaarReady = aadhaarReady,
                        panReady = panReady,
                        chequeReady = chequeReady,
                        termsReady = termsReady
                    )
                    if (agentProfile?.advisorId != null && chequeReady && !pennyDropComplete) {
                        Button(
                            onClick = { vm.startPennyDropVerification() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C071C)),
                            enabled = canStartPennyDrop
                        ) {
                            if (state.processingPennyDrop) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            } else {
                                Text("Verify Bank Access with ₹1", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("KYC Verification", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text(
                            if (docsEditable || termsEditable) {
                                "Upload Aadhaar, PAN, and a cancelled cheque. Files are encrypted before storage and cheque details are never shown publicly."
                            } else {
                                "Documents are locked while verification is in progress or approved. You can upload again if the review is rejected."
                            },
                            color = AgentColorsMuted
                        )
                    }

                    AgentDocumentCard(
                        title = "Aadhaar Card",
                        subtitle = currentDocumentLabel(agentProfile?.kycDocuments?.firstOrNull { it.documentType == "aadhaar" }),
                        selectedName = aadhaarUri?.let { context.fileLabelFor(it) },
                        highlighted = true,
                        enabled = aadhaarEditable,
                        onPick = { aadhaarPicker.launch(arrayOf("image/*", "application/pdf")) }
                    )
                    AgentDocumentCard(
                        title = "PAN Card",
                        subtitle = currentDocumentLabel(agentProfile?.kycDocuments?.firstOrNull { it.documentType == "pan" }),
                        selectedName = panUri?.let { context.fileLabelFor(it) },
                        highlighted = false,
                        enabled = panEditable,
                        onPick = { panPicker.launch(arrayOf("image/*", "application/pdf")) }
                    )
                    AgentDocumentCard(
                        title = "Cancelled Cheque",
                        subtitle = currentDocumentLabel(agentProfile?.kycDocuments?.firstOrNull { it.documentType == "cancelled_cheque" }),
                        selectedName = chequeUri?.let { context.fileLabelFor(it) },
                        highlighted = false,
                        enabled = chequeEditable,
                        onPick = { chequePicker.launch(arrayOf("image/*", "application/pdf")) }
                    )
                    UploadDropZone(
                        aadhaarReady = aadhaarReady,
                        panReady = panReady,
                        chequeReady = chequeReady,
                        aadhaarEnabled = aadhaarEditable,
                        panEnabled = panEditable,
                        chequeEnabled = chequeEditable,
                        onPickAadhaar = { aadhaarPicker.launch(arrayOf("image/*", "application/pdf")) },
                        onPickPan = { panPicker.launch(arrayOf("image/*", "application/pdf")) },
                        onPickCheque = { chequePicker.launch(arrayOf("image/*", "application/pdf")) }
                    )

                    TermsAcceptanceCard(
                        accepted = termsAccepted,
                        locked = termsReady,
                        scrollState = termsScrollState,
                        canAccept = termsScrolledToBottom || termsAccepted,
                        onAcceptedChange = { checked ->
                            if (termsScrolledToBottom || termsAccepted) termsAccepted = checked
                        }
                    )

                    state.saveMessage?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = Color(0xFF0F8D57), fontWeight = FontWeight.Medium)
                    }
                    state.error?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = AgentColorsAccent, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val documents = listOfNotNull(
                                aadhaarUri?.let { context.toAgentDocumentPart(it, "documents", 0) },
                                panUri?.let { context.toAgentDocumentPart(it, "documents", 1) },
                                chequeUri?.let { context.toAgentDocumentPart(it, "documents", 2) }
                            )
                            if (documents.isEmpty()) {
                                vm.saveAgentProfile(
                                    AgentProfileUpsertRequest(
                                        fullName = fullName,
                                        email = email,
                                        phone = phone,
                                        city = city,
                                        state = stateName,
                                        businessName = businessName,
                                        referralCode = referralCode,
                                        yearsExperience = yearsExperience.toIntOrNull() ?: 0,
                                        languages = languagesText.toCsvList(),
                                        serviceLabel = agentProfile?.serviceLabel?.ifBlank { "SoulMatch Agent" } ?: "SoulMatch Agent",
                                        termsAccepted = termsAccepted
                                    ),
                                    onCompleted = afterSuccessfulSave
                                )
                            } else {
                                val request = AgentOnboardingRequest(
                                    fullName = fullName,
                                    phone = phone,
                                    email = email,
                                    city = city,
                                    state = stateName,
                                    businessName = businessName,
                                    referralCode = referralCode,
                                    yearsExperience = yearsExperience.toIntOrNull() ?: 0,
                                    languages = languagesText.toCsvList(),
                                    termsAccepted = termsAccepted,
                                    kycDocuments = agentProfile?.kycDocuments
                                        ?.mapNotNull { document ->
                                            document.fileUrl.takeIf { it.isNotBlank() }?.let {
                                                AgentKycDocumentInput(
                                                    documentType = document.documentType,
                                                    documentSide = document.documentSide,
                                                    fileUrl = it
                                                )
                                            }
                                        }
                                        .orEmpty()
                                )
                                val documentMeta = buildList {
                                    aadhaarUri?.let { add(mapOf("documentType" to "aadhaar", "documentSide" to "single")) }
                                    panUri?.let { add(mapOf("documentType" to "pan", "documentSide" to "single")) }
                                    chequeUri?.let { add(mapOf("documentType" to "cancelled_cheque", "documentSide" to "single")) }
                                }
                                vm.submitOnboardingWithDocuments(
                                    request = request,
                                    documents = documents,
                                    documentMeta = documentMeta,
                                    onCompleted = afterSuccessfulSave
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(top = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AgentColorsAccent),
                        enabled = canSubmit
                    ) {
                        if (state.saving) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        } else {
                            Text(resolveAgentCtaLabel(isRegisteredAgent, agentProfile?.onboardingStatus), fontWeight = FontWeight.Bold)
                        }
                    }

                    if (!isRegisteredAgent) {
                        Text(
                            "Phone OTP is verified during login. By continuing, you confirm the documents are valid and agree to SoulMatch agent terms.",
                            color = AgentColorsMuted,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (!canSubmit) {
                        Text(
                            "Complete business details, Aadhaar, PAN, cancelled cheque, and terms acceptance to submit for review.",
                            color = AgentColorsMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentStatusSummary(
    onboardingStatus: String?,
    kycStatus: String?,
    aadhaarStatus: String?,
    bankStatus: String?,
    rejectionReason: String?
) {
    val onboardingLabel = when (onboardingStatus) {
        "approved" -> "Approved"
        "rejected" -> "Needs update"
        "under_review" -> "Under review"
        else -> "Pending review"
    }
    val onboardingColor = when (onboardingStatus) {
        "approved" -> Color(0xFF0F8D57)
        "rejected" -> AgentColorsAccent
        "under_review" -> Color(0xFF9D5B00)
        else -> Color(0xFF5D4A52)
    }
    val kycLabel = when (kycStatus) {
        "approved" -> "KYC verified"
        "rejected" -> "KYC declined"
        "under_review" -> "KYC under review"
        else -> "KYC in progress"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFFAF8),
        border = BorderStroke(1.dp, Color(0xFFF0E2DE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(999.dp), color = onboardingColor.copy(alpha = 0.12f)) {
                        Text(
                            onboardingLabel,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = onboardingColor,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFF7EEE8)) {
                        Text(
                            kycLabel,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = Color(0xFF6D4B53),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (aadhaarStatus == "verified") {
                    Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFE8F7EE)) {
                        Text(
                            "Aadhaar verified",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = Color(0xFF0F8D57),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                    if (bankStatus == "verified") {
                    Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFE8F7EE)) {
                        Text(
                            "Bank verified",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = Color(0xFF0F8D57),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                }
            }
            if (!rejectionReason.isNullOrBlank()) {
                Text(rejectionReason, color = AgentColorsMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AgentFraudChecklist(
    profile: com.soulmatch.app.data.models.AgentProfileData?,
    aadhaarReady: Boolean,
    panReady: Boolean,
    chequeReady: Boolean,
    termsReady: Boolean
) {
    val completed = listOf(
        true,
        aadhaarReady,
        panReady,
        chequeReady,
        profile?.pennyDropStatus in listOf("paid", "verified"),
        termsReady
    ).count { it }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFFF7F3),
        border = BorderStroke(1.dp, Color(0xFFF0DED8))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Fraud-safe onboarding", fontWeight = FontWeight.Bold, color = Color(0xFF2A1D20))
                    Text("Phone, KYC, bank access, and terms", color = AgentColorsMuted, style = MaterialTheme.typography.bodySmall)
                }
                Text("$completed/6", color = AgentColorsAccent, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = completed / 6f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp),
                color = AgentColorsAccent,
                trackColor = Color(0xFFE9DDD9)
            )
            ChecklistRow("Phone OTP verified", "Completed during login", true)
            ChecklistRow("Aadhaar + PAN upload", "Encrypted storage, name cross-match status: ${profile?.kycNameMatchStatus.statusLabel()}", aadhaarReady && panReady)
            ChecklistRow("Cancelled cheque upload", "Bank details encrypted and hidden from public view", chequeReady)
            ChecklistRow("Reverse ₹1 bank check", "Razorpay order ready after cheque review", profile?.pennyDropStatus in listOf("paid", "verified"))
            ChecklistRow("Digital terms signature", "Scroll-gated agreement with IP and timestamp logging", termsReady)
        }
    }
}

@Composable
private fun ChecklistRow(title: String, subtitle: String, done: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = if (done) Color(0xFFE7F7EE) else Color(0xFFF3ECE8)
        ) {
            Text(
                if (done) "✓" else "•",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = if (done) Color(0xFF0F8D57) else AgentColorsMuted,
                fontWeight = FontWeight.Bold
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = Color(0xFF322429))
            Text(subtitle, color = AgentColorsMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TermsAcceptanceCard(
    accepted: Boolean,
    locked: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    canAccept: Boolean,
    onAcceptedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFFFAF8),
        border = BorderStroke(1.dp, Color(0xFFF0DED8))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Agent Terms of Service", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 180.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFEADDD8))
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Safe Harbour: SoulMatch provides the platform and directory. Agents remain responsible for offline commitments, representations, and conduct.", color = Color(0xFF3C2B30), style = MaterialTheme.typography.bodySmall)
                    Text("Advance Fee Ban: Agents must not collect advance service fees from members outside approved SoulMatch policies and invoices.", color = Color(0xFF3C2B30), style = MaterialTheme.typography.bodySmall)
                    Text("Data Usage: Aadhaar, PAN, cheque, and profile data are used only for verification, fraud prevention, audit, and legally required retention.", color = Color(0xFF3C2B30), style = MaterialTheme.typography.bodySmall)
                    Text("Intermediary Disclaimer: SoulMatch does not guarantee marriage, payment outcomes, offline negotiations, or family decisions made outside the platform.", color = Color(0xFF3C2B30), style = MaterialTheme.typography.bodySmall)
                    Text("Grievance Officer: Complaints may be raised through the SoulMatch support and grievance channel listed in the app.", color = Color(0xFF3C2B30), style = MaterialTheme.typography.bodySmall)
                    Text("Acceptance is stored with timestamp, IP address, device user-agent, and the active terms version.", color = Color(0xFF3C2B30), style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = accepted,
                    enabled = !locked && canAccept,
                    onCheckedChange = onAcceptedChange
                )
                Text(
                    when {
                        locked -> "Terms already accepted for this agent profile."
                        canAccept -> "I have read and agree to the agent terms."
                        else -> "Scroll to the bottom to enable agreement."
                    },
                    color = AgentColorsMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun AgentTextField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

private fun String?.statusLabel(): String {
    return when (this?.lowercase(Locale.US)) {
        "matched" -> "matched"
        "mismatch" -> "mismatch"
        "manual_review" -> "manual review"
        "vendor_unavailable" -> "vendor pending"
        else -> "pending"
    }
}

@Composable
private fun AgentDocumentCard(
    title: String,
    subtitle: String,
    selectedName: String?,
    highlighted: Boolean,
    enabled: Boolean,
    onPick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onPick),
        color = if (enabled) {
            if (highlighted) Color(0xFFFFF8F9) else Color(0xFFFFFBF9)
        } else {
            Color(0xFFF8F5F3)
        },
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, if (highlighted && enabled) AgentColorsAccent else Color(0xFFE9D9D4))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Badge, contentDescription = null, tint = AgentColorsAccent)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(selectedName ?: subtitle, color = AgentColorsMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (selectedName != null) {
                Icon(Icons.Outlined.Verified, contentDescription = null, tint = AgentColorsAccent)
            } else if (!enabled) {
                Text("Locked", color = AgentColorsMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun UploadDropZone(
    aadhaarReady: Boolean,
    panReady: Boolean,
    chequeReady: Boolean,
    aadhaarEnabled: Boolean,
    panEnabled: Boolean,
    chequeEnabled: Boolean,
    onPickAadhaar: () -> Unit,
    onPickPan: () -> Unit,
    onPickCheque: () -> Unit
) {
    val enabled = aadhaarEnabled || panEnabled || chequeEnabled
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = if (enabled) Color(0xFFFFF8F7) else Color(0xFFF8F5F3),
        border = BorderStroke(1.dp, if (enabled) Color(0xFFF0B8C5) else Color(0xFFE3D7D3))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Outlined.CloudUpload, contentDescription = null, tint = AgentColorsAccent, modifier = Modifier.size(30.dp))
            Text(if (enabled) "Upload missing or rejected documents" else "Document uploads are locked", fontWeight = FontWeight.Bold)
            Text(
                if (enabled) "SVG, PNG, JPG or PDF (max. 10MB)" else "Only missing or rejected documents can be uploaded here.",
                color = AgentColorsMuted,
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniUploadStatus("Aadhaar", aadhaarReady, aadhaarEnabled, onPickAadhaar)
                MiniUploadStatus("PAN", panReady, panEnabled, onPickPan)
            }
            MiniUploadStatus("Cheque", chequeReady, chequeEnabled, onPickCheque)
        }
    }
}

@Composable
private fun MiniUploadStatus(label: String, uploaded: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        color = if (uploaded) Color(0xFFFFEEF3) else Color.White,
        border = BorderStroke(1.dp, if (uploaded) AgentColorsAccent else Color(0xFFE3D7D3)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Description, contentDescription = null, tint = AgentColorsAccent, modifier = Modifier.size(16.dp))
            Text(
                when {
                    uploaded -> "$label added"
                    enabled -> "Upload $label"
                    else -> "$label locked"
                },
                color = AgentColorsAccent,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun currentDocumentLabel(document: com.soulmatch.app.data.models.AgentKycDocumentData?): String {
    if (document == null) return "No document uploaded yet"
    return when (document.status.lowercase(Locale.US)) {
        "verified" -> "Verified document on file"
        "rejected" -> document.reviewComment.ifBlank { "Rejected - upload a clearer copy" }
        "under_review" -> "Submitted and under review"
        else -> "Uploaded and waiting for review"
    }
}

private fun Context.fileLabelFor(uri: Uri): String {
    val name = contentResolver.getType(uri)?.substringAfter('/')?.uppercase(Locale.US)
    return name?.let { "$it selected" } ?: "Document selected"
}

private fun Context.toAgentDocumentPart(uri: Uri, formKey: String, index: Int): MultipartBody.Part? {
    val contentType = contentResolver.getType(uri) ?: "application/octet-stream"
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType)?.lowercase(Locale.US)
        ?: if (contentType == "application/pdf") "pdf" else "jpg"
    val fileName = "agent-document-${System.currentTimeMillis()}-$index.$extension"
    val body = bytes.toRequestBody(contentType.toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(formKey, fileName, body)
}

private fun agentPennyDropCheckoutOptions(order: OrderData): JSONObject = JSONObject().apply {
    put("name", "SoulMatch")
    put("description", "Agent bank verification")
    put("currency", order.currency.ifBlank { "INR" })
    put("amount", order.amount)
    put("order_id", order.orderId)
    put("theme", JSONObject().put("color", "#B0004B"))
    put("prefill", JSONObject())
    put("notes", JSONObject().put("purpose", "agent_reverse_penny_drop"))
}

private fun String.toCsvList(): List<String> = split(',')
    .map { it.trim() }
    .filter { it.isNotBlank() }

private fun resolveAgentCtaLabel(isRegisteredAgent: Boolean, onboardingStatus: String?): String = when {
    !isRegisteredAgent -> "Register as Agent"
    onboardingStatus == "rejected" -> "Resubmit for Review"
    onboardingStatus == "approved" -> "Save Changes"
    else -> "Update Profile"
}

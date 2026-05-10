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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Description
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
    val isRegisteredAgent = agentProfile?.advisorId != null
    val docsEditable = !isRegisteredAgent ||
        agentProfile?.kycStatus.equals("rejected", ignoreCase = true) ||
        agentProfile?.onboardingStatus.equals("rejected", ignoreCase = true)

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
        derivedStateOf { termsScrollState.maxValue > 0 && termsScrollState.value >= termsScrollState.maxValue - 4 }
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
    val detailsReady = fullName.isNotBlank() &&
        phone.isNotBlank() &&
        city.isNotBlank() &&
        stateName.isNotBlank() &&
        businessName.isNotBlank() &&
        (yearsExperience.toIntOrNull()?.let { it >= 0 } == true) &&
        languagesText.split(',').map { it.trim() }.any { it.isNotBlank() }
    val requiresFraudSubmit = docsEditable && (!isRegisteredAgent || agentProfile?.onboardingStatus.equals("rejected", true) || agentProfile?.kycStatus.equals("rejected", true))
    val canSubmit = !state.saving && detailsReady && (!requiresFraudSubmit || (aadhaarReady && panReady && chequeReady && termsReady))
    val pennyCheckout = state.pennyCheckout
    val pennyDropComplete = agentProfile?.pennyDropStatus in listOf("paid", "verified")
    val canStartPennyDrop = agentProfile?.advisorId != null && chequeReady && !pennyDropComplete && !state.processingPennyDrop

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
                            if (docsEditable) {
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
                        enabled = docsEditable,
                        onPick = { aadhaarPicker.launch(arrayOf("image/*", "application/pdf")) }
                    )
                    AgentDocumentCard(
                        title = "PAN Card",
                        subtitle = currentDocumentLabel(agentProfile?.kycDocuments?.firstOrNull { it.documentType == "pan" }),
                        selectedName = panUri?.let { context.fileLabelFor(it) },
                        highlighted = false,
                        enabled = docsEditable,
                        onPick = { panPicker.launch(arrayOf("image/*", "application/pdf")) }
                    )
                    AgentDocumentCard(
                        title = "Cancelled Cheque",
                        subtitle = currentDocumentLabel(agentProfile?.kycDocuments?.firstOrNull { it.documentType == "cancelled_cheque" }),
                        selectedName = chequeUri?.let { context.fileLabelFor(it) },
                        highlighted = false,
                        enabled = docsEditable,
                        onPick = { chequePicker.launch(arrayOf("image/*", "application/pdf")) }
                    )
                    UploadDropZone(
                        aadhaarReady = aadhaarReady,
                        panReady = panReady,
                        chequeReady = chequeReady,
                        enabled = docsEditable,
                        onPickAadhaar = { aadhaarPicker.launch(arrayOf("image/*", "application/pdf")) },
                        onPickPan = { panPicker.launch(arrayOf("image/*", "application/pdf")) },
                        onPickCheque = { chequePicker.launch(arrayOf("image/*", "application/pdf")) }
                    )

                    TermsAcceptanceCard(
                        accepted = termsAccepted,
                        locked = !docsEditable && termsReady,
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
                            if (
                                !docsEditable ||
                                (
                                    documents.isEmpty() &&
                                        agentProfile != null &&
                                        agentProfile.kycDocuments.isNotEmpty() &&
                                        agentProfile.onboardingStatus in listOf("approved", "under_review")
                                )
                            ) {
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
                                    onCompleted = onCompleted
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
                                    onCompleted = onCompleted
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
    enabled: Boolean,
    onPickAadhaar: () -> Unit,
    onPickPan: () -> Unit,
    onPickCheque: () -> Unit
) {
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
            Text(if (enabled) "Click to upload or replace documents" else "Document uploads are locked", fontWeight = FontWeight.Bold)
            Text(
                if (enabled) "SVG, PNG, JPG or PDF (max. 10MB)" else "Re-opened automatically if KYC is rejected.",
                color = AgentColorsMuted,
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniUploadStatus("Aadhaar", aadhaarReady, enabled, onPickAadhaar)
                MiniUploadStatus("PAN", panReady, enabled, onPickPan)
            }
            MiniUploadStatus("Cheque", chequeReady, enabled, onPickCheque)
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

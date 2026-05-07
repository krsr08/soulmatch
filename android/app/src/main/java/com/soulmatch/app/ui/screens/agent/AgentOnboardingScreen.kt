package com.soulmatch.app.ui.screens.agent

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.data.models.AgentOnboardingRequest
import com.soulmatch.app.ui.viewmodels.AgentViewModel
import java.util.Locale
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@Composable
fun AgentOnboardingScreen(
    onBack: () -> Unit,
    onCompleted: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenAccount: () -> Unit,
    vm: AgentViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var fullName by remember { mutableStateOf(state.agentProfile?.fullName.orEmpty()) }
    var phone by remember { mutableStateOf(state.agentProfile?.phone.orEmpty()) }
    var email by remember { mutableStateOf(state.agentProfile?.email.orEmpty()) }
    var city by remember { mutableStateOf(state.agentProfile?.city.orEmpty()) }
    var stateName by remember { mutableStateOf(state.agentProfile?.state.orEmpty()) }
    var businessName by remember { mutableStateOf(state.agentProfile?.businessName.orEmpty()) }
    var referralCode by remember { mutableStateOf(state.agentProfile?.referralCode.orEmpty()) }
    var aadhaarUri by remember { mutableStateOf<Uri?>(null) }
    var panUri by remember { mutableStateOf<Uri?>(null) }

    val aadhaarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        aadhaarUri = uri
    }
    val panPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        panUri = uri
    }

    AgentScaffold(
        title = "SoulMatch",
        selectedTab = AgentTab.Account,
        onOpenDashboard = onOpenDashboard,
        onOpenProfiles = onOpenProfiles,
        onOpenPlans = onOpenPlans,
        onOpenAccount = onOpenAccount
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("STEP 1 OF 2", color = AgentColorsAccent, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("Business Details", color = AgentColorsMuted)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(Color(0xFFE9E0DC), RoundedCornerShape(999.dp))
                    ) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(6.dp)
                                .background(AgentColorsAccent, RoundedCornerShape(999.dp))
                        )
                    }
                    AgentTextField("Full Name", fullName) { fullName = it }
                    AgentTextField("Business/Agency Name", businessName) { businessName = it }
                    AgentTextField("Professional Email", email) { email = it }
                    AgentTextField("Mobile Number", phone) { phone = it }
                    AgentTextField("City", city) { city = it }
                    AgentTextField("State", stateName) { stateName = it }
                    AgentTextField("Referral Code (Optional)", referralCode) { referralCode = it }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("KYC Verification", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text("Please upload your primary identification documents for platform security.", color = AgentColorsMuted)
                    }
                    AgentDocumentCard(
                        title = "Aadhaar Card",
                        subtitle = "Recommended for faster approval",
                        selectedName = aadhaarUri?.let { context.fileLabelFor(it) },
                        highlighted = true,
                        onPick = { aadhaarPicker.launch(arrayOf("image/*", "application/pdf")) }
                    )
                    AgentDocumentCard(
                        title = "PAN Card",
                        subtitle = "Alternative verification",
                        selectedName = panUri?.let { context.fileLabelFor(it) },
                        highlighted = false,
                        onPick = { panPicker.launch(arrayOf("image/*", "application/pdf")) }
                    )
                    UploadDropZone(
                        aadhaarReady = aadhaarUri != null,
                        panReady = panUri != null,
                        onPickAadhaar = { aadhaarPicker.launch(arrayOf("image/*", "application/pdf")) },
                        onPickPan = { panPicker.launch(arrayOf("image/*", "application/pdf")) }
                    )
                    state.error?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = AgentColorsAccent, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            val request = AgentOnboardingRequest(
                                fullName = fullName,
                                phone = phone,
                                email = email,
                                city = city,
                                state = stateName,
                                businessName = businessName,
                                referralCode = referralCode
                            )
                            val documents = listOfNotNull(
                                aadhaarUri?.let { context.toAgentDocumentPart(it, "documents", 0) },
                                panUri?.let { context.toAgentDocumentPart(it, "documents", 1) }
                            )
                            val documentMeta = buildList {
                                aadhaarUri?.let { add(mapOf("documentType" to "aadhaar", "documentSide" to "single")) }
                                panUri?.let { add(mapOf("documentType" to "pan", "documentSide" to "single")) }
                            }
                            vm.submitOnboardingWithDocuments(
                                request = request,
                                documents = documents,
                                documentMeta = documentMeta,
                                onCompleted = onCompleted
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AgentColorsAccent),
                        enabled = !state.saving
                    ) {
                        if (state.saving) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        } else {
                            Text("Register as Agent", fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        "By registering, you agree to our Terms of Service and Privacy Policy.",
                        color = AgentColorsMuted,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true
    )
}

@Composable
private fun AgentDocumentCard(
    title: String,
    subtitle: String,
    selectedName: String?,
    highlighted: Boolean,
    onPick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPick),
        color = if (highlighted) Color(0xFFFFF8F9) else Color(0xFFFFFBF9),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, if (highlighted) AgentColorsAccent else Color(0xFFE9D9D4))
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
            }
        }
    }
}

@Composable
private fun UploadDropZone(
    aadhaarReady: Boolean,
    panReady: Boolean,
    onPickAadhaar: () -> Unit,
    onPickPan: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFFFF8F7),
        border = BorderStroke(1.dp, Color(0xFFF0B8C5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Outlined.CloudUpload, contentDescription = null, tint = AgentColorsAccent, modifier = Modifier.size(30.dp))
            Text("Click to upload or drag and drop", fontWeight = FontWeight.Bold)
            Text("SVG, PNG, JPG or PDF (max. 10MB)", color = AgentColorsMuted, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniUploadStatus("Aadhaar", aadhaarReady, onPickAadhaar)
                MiniUploadStatus("PAN", panReady, onPickPan)
            }
        }
    }
}

@Composable
private fun MiniUploadStatus(label: String, uploaded: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
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
            Text(if (uploaded) "$label added" else "Upload $label", color = AgentColorsAccent, style = MaterialTheme.typography.bodySmall)
        }
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

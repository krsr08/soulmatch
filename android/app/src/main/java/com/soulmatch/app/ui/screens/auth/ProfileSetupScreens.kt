package com.soulmatch.app.ui.screens.auth

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.soulmatch.app.data.config.mediaUrl
import com.soulmatch.app.data.models.ProfileData
import com.soulmatch.app.data.models.ProfilePhoto
import com.soulmatch.app.data.models.VerificationRequestData
import com.soulmatch.app.data.models.fullName
import com.soulmatch.app.ui.design.SoulMatchTokens
import com.soulmatch.app.ui.viewmodels.MyProfileViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale

@Composable
fun ProfileIntroScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    ProfileSetupScaffold(
        title = "Create profile",
        stepLabel = "Screen 1 of 10",
        headline = "Build a profile families can trust",
        body = "We guide step by step. Real details now. Better matches later.",
        infoTitle = "Why profile setup matters",
        infoBody = "SoulMatch uses profile details, photos, trust checks, and partner preferences to rank better matches and help families review profiles faster.",
        onBack = onBack,
        primaryText = "Start profile",
        onPrimary = onContinue
    ) {
        SetupBullet("Basic details first")
        SetupBullet("Community, career, family, and lifestyle next")
        SetupBullet("Photos, verification, and final review before launch")
    }
}

@Composable
fun ProfilePhotoUploadScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    vm: MyProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val photos by vm.photos.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val isUploading by vm.isUploadingPhotos.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.toPhotoPart(uri, photos.size)?.let { vm.uploadPhotos(listOf(it)) }
    }

    ProfileSetupScaffold(
        title = "Photo upload",
        stepLabel = "Screen 8 of 10",
        headline = "Add profile photos",
        body = "Use clear recent photos. Pick one primary photo. Real uploads only.",
        infoTitle = "Photo guidance",
        infoBody = "At least one clear face photo helps verification and trust. Primary photo shows first in match cards.",
        onBack = onBack,
        primaryText = "Continue",
        primaryEnabled = photos.isNotEmpty() && !isUploading,
        onPrimary = onContinue
    ) {
        OutlinedButton(
            onClick = {
                vm.clearStatus()
                picker.launch("image/*")
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(SoulMatchTokens.PillRadius)
        ) {
            Icon(Icons.Filled.PhotoCamera, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text(if (isUploading) "Uploading..." else "Add photo")
        }
        if (!status.isNullOrBlank()) {
            InlineNotice(status.orEmpty(), error = false)
        }
        if (photos.isEmpty()) {
            EmptySetupCard("No photos yet. Add at least one photo to continue.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                photos.forEach { photo ->
                    PhotoCard(
                        photo = photo,
                        onMakePrimary = { vm.setPrimaryPhoto(photo.photoId) },
                        onDelete = { vm.deletePhoto(photo.photoId) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileVerificationScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    vm: MyProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val verifications by vm.verifications.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val isSubmitting by vm.isSubmittingVerification.collectAsStateWithLifecycle()
    var documentType by rememberSaveable { mutableStateOf("aadhaar") }
    var referenceNumber by rememberSaveable { mutableStateOf("") }
    var selectedUri by rememberSaveable { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedUri = uri?.toString()
    }

    ProfileSetupScaffold(
        title = "Verification",
        stepLabel = "Screen 9 of 10",
        headline = "Submit trust checks",
        body = "Phone, profile, and optional identity checks help review move faster.",
        infoTitle = "Verification help",
        infoBody = "Profile verification marks the account for review. Identity documents add stronger trust signals when available.",
        onBack = onBack,
        primaryText = "Continue",
        primaryEnabled = !isSubmitting,
        onPrimary = onContinue
    ) {
        OutlinedButton(
            onClick = {
                vm.clearStatus()
                vm.submitProfileVerification()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(SoulMatchTokens.PillRadius)
        ) {
            Icon(Icons.Filled.VerifiedUser, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text(if (isSubmitting) "Submitting..." else "Submit profile verification")
        }
        SetupDropdown(
            label = "Document type",
            value = documentType,
            options = listOf("aadhaar", "pan", "voter_id", "education_certificate", "income_payslip"),
            onSelect = { documentType = it }
        )
        OutlinedTextField(
            value = referenceNumber,
            onValueChange = { referenceNumber = it.replace(" ", "") },
            label = { Text("Reference number") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedButton(
            onClick = { picker.launch("*/*") },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(SoulMatchTokens.PillRadius)
        ) {
            Text(if (selectedUri.isNullOrBlank()) "Choose document" else "Document selected")
        }
        Button(
            onClick = {
                val uri = selectedUri?.let(Uri::parse) ?: return@Button
                context.toVerificationDocumentPart(uri)?.let { part ->
                    vm.clearStatus()
                    vm.submitTrustVerification(
                        type = "identity",
                        document = part,
                        documentType = documentType,
                        referenceNumber = referenceNumber
                    )
                }
            },
            enabled = !selectedUri.isNullOrBlank() && !isSubmitting,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(SoulMatchTokens.PillRadius),
            colors = ButtonDefaults.buttonColors(containerColor = SoulMatchTokens.Tangerine, contentColor = Color.White)
        ) {
            Text("Upload document for review")
        }
        if (!status.isNullOrBlank()) {
            InlineNotice(status.orEmpty(), error = status.orEmpty().contains("couldn't", ignoreCase = true))
        }
        if (verifications.isEmpty()) {
            EmptySetupCard("No verification requests yet. Submit profile or document verification above.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                verifications.forEach { item ->
                    VerificationCard(item)
                }
            }
        }
    }
}

@Composable
fun ProfilePreviewReviewScreen(
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    vm: MyProfileViewModel = hiltViewModel()
) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val photos by vm.photos.collectAsStateWithLifecycle()
    val verifications by vm.verifications.collectAsStateWithLifecycle()

    ProfileSetupScaffold(
        title = "Preview profile",
        stepLabel = "Screen 10 of 10",
        headline = "Review before submit",
        body = "Check profile data, photos, and trust status before final review.",
        infoTitle = "Preview review",
        infoBody = "This is the final check before the profile moves into review. Go back and edit any section if something looks wrong.",
        onBack = onBack,
        primaryText = "Submit for review",
        primaryEnabled = profile != null,
        onPrimary = onSubmit
    ) {
        PreviewHeader(profile)
        PreviewSection("Photos") {
            if (photos.isEmpty()) {
                Text("No uploaded photos", color = SoulMatchTokens.Muted)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    photos.take(3).forEach { photo ->
                        AsyncImage(
                            model = mediaUrl(photo.photoUrl),
                            contentDescription = null,
                            modifier = Modifier.size(92.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
        PreviewSection("Verification") {
            Text(
                if (verifications.isEmpty()) "No verification request yet" else "${verifications.size} verification request(s) submitted",
                color = SoulMatchTokens.Text
            )
        }
        PreviewSection("Profile summary") {
            PreviewLine("Name", profile?.fullName().orEmpty())
            PreviewLine("Religion", profile?.religion.orEmpty())
            PreviewLine("Education", profile?.educationLevel.orEmpty())
            PreviewLine("Occupation", profile?.occupation.orEmpty())
            PreviewLine("Family city", profile?.familyCity.orEmpty())
            PreviewLine("Lifestyle", listOf(profile?.diet, profile?.smoking, profile?.drinking).filterNotNull().filter { it.isNotBlank() }.joinToString(", "))
        }
    }
}

@Composable
fun ProfileUnderReviewScreen(
    onPrimary: () -> Unit,
    onHelp: () -> Unit
) {
    ProfileSetupScaffold(
        title = "Under review",
        stepLabel = "Submitted",
        headline = "Profile sent for review",
        body = "We received your profile. Team review now running.",
        infoTitle = "Review status",
        infoBody = "Review checks profile quality, trust signals, and uploaded documents before the profile becomes broadly visible.",
        onBack = {},
        showBack = false,
        primaryText = "Go to home",
        onPrimary = onPrimary
    ) {
        SetupBullet("Profile data saved")
        SetupBullet("Photos uploaded")
        SetupBullet("Verification request queued")
        OutlinedButton(onClick = onHelp, modifier = Modifier.fillMaxWidth()) {
            Text("Help and support")
        }
    }
}

@Composable
fun ProfileCorrectionRequiredScreen(
    onBackToEdit: () -> Unit,
    onReviewAgain: () -> Unit
) {
    ProfileSetupScaffold(
        title = "Needs correction",
        stepLabel = "Action needed",
        headline = "Profile needs an update",
        body = "Fix requested details. Then send for review again.",
        infoTitle = "Correction help",
        infoBody = "Review may ask for better photos, clearer documents, or profile detail corrections.",
        onBack = onBackToEdit,
        primaryText = "Back to edit",
        onPrimary = onBackToEdit
    ) {
        OutlinedButton(onClick = onReviewAgain, modifier = Modifier.fillMaxWidth()) {
            Text("Go to review status")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSetupScaffold(
    title: String,
    stepLabel: String,
    headline: String,
    body: String,
    infoTitle: String,
    infoBody: String,
    onBack: () -> Unit,
    primaryText: String,
    onPrimary: () -> Unit,
    primaryEnabled: Boolean = true,
    showBack: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    var showInfo by remember { mutableStateOf(false) }
    if (showInfo) {
        ModalBottomSheet(onDismissRequest = { showInfo = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(infoTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(infoBody, style = MaterialTheme.typography.bodyMedium, color = SoulMatchTokens.Muted)
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, fontWeight = FontWeight.Bold)
                        Text(stepLabel, style = MaterialTheme.typography.bodySmall, color = SoulMatchTokens.Muted)
                    }
                },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = SoulMatchTokens.Tangerine)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showInfo = true }) {
                        Icon(Icons.Filled.Info, contentDescription = "Info", tint = SoulMatchTokens.Tangerine)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SoulMatchTokens.Bg,
                    navigationIconContentColor = SoulMatchTokens.Tangerine
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SoulMatchTokens.Bg)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(headline, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = SoulMatchTokens.Text)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = SoulMatchTokens.Muted)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content
            )
            Button(
                onClick = onPrimary,
                enabled = primaryEnabled,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(SoulMatchTokens.PillRadius),
                colors = ButtonDefaults.buttonColors(containerColor = SoulMatchTokens.Tangerine, contentColor = Color.White)
            ) {
                Text(primaryText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SetupBullet(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SoulMatchTokens.Border)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SoulMatchTokens.Tangerine)
            Text(text, color = SoulMatchTokens.Text)
        }
    }
}

@Composable
private fun InlineNotice(message: String, error: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (error) MaterialTheme.colorScheme.errorContainer else SoulMatchTokens.TangerineSoft
    ) {
        Text(
            message,
            modifier = Modifier.padding(14.dp),
            color = if (error) MaterialTheme.colorScheme.onErrorContainer else SoulMatchTokens.Tangerine
        )
    }
}

@Composable
private fun EmptySetupCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SoulMatchTokens.Border)
    ) {
        Text(message, modifier = Modifier.padding(18.dp), color = SoulMatchTokens.Muted)
    }
}

@Composable
private fun PhotoCard(
    photo: ProfilePhoto,
    onMakePrimary: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SoulMatchTokens.Border)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = mediaUrl(photo.photoUrl),
                contentDescription = null,
                modifier = Modifier.size(88.dp),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (photo.isPrimary) "Primary photo" else "Profile photo", fontWeight = FontWeight.Bold, color = SoulMatchTokens.Text)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!photo.isPrimary) {
                        OutlinedButton(onClick = onMakePrimary) { Text("Make primary") }
                    }
                    OutlinedButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun VerificationCard(item: VerificationRequestData) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SoulMatchTokens.Border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(item.type.replace('_', ' ').replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold, color = SoulMatchTokens.Text)
            Text(item.status.replace('_', ' ').replaceFirstChar { it.uppercase() }, color = SoulMatchTokens.Tangerine)
            if (!item.reviewNote.isNullOrBlank()) {
                Text(item.reviewNote.orEmpty(), color = SoulMatchTokens.Muted)
            }
        }
    }
}

@Composable
private fun PreviewHeader(profile: ProfileData?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SoulMatchTokens.Border)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(profile?.fullName().orEmpty().ifBlank { "New member" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(
                listOf(profile?.gender, profile?.workingCity, profile?.familyCity).filterNotNull().filter { it.isNotBlank() }.joinToString(" | "),
                color = SoulMatchTokens.Muted
            )
        }
    }
}

@Composable
private fun PreviewSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SoulMatchTokens.Border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold, color = SoulMatchTokens.Text)
            content()
        }
    }
}

@Composable
private fun PreviewLine(label: String, value: String) {
    if (value.isBlank()) return
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = SoulMatchTokens.Muted)
        Text(value, color = SoulMatchTokens.Text, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SetupDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = value.replace('_', ' ').replaceFirstChar { it.uppercase() },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.replace('_', ' ').replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun Context.toPhotoPart(uri: Uri, index: Int): MultipartBody.Part? {
    val contentType = contentResolver.getType(uri) ?: "image/jpeg"
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType)?.lowercase(Locale.US) ?: "jpg"
    val fileName = "profile-photo-${System.currentTimeMillis()}-$index.$extension"
    val body = bytes.toRequestBody(contentType.toMediaTypeOrNull())
    return MultipartBody.Part.createFormData("photos", fileName, body)
}

private fun Context.toVerificationDocumentPart(uri: Uri): MultipartBody.Part? {
    val contentType = contentResolver.getType(uri) ?: "application/octet-stream"
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType)?.lowercase(Locale.US)
        ?: if (contentType.contains("pdf", ignoreCase = true)) "pdf" else "jpg"
    val fileName = "trust-document-${System.currentTimeMillis()}.$extension"
    val body = bytes.toRequestBody(contentType.toMediaTypeOrNull())
    return MultipartBody.Part.createFormData("document", fileName, body)
}

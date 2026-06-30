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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.soulmatch.app.data.config.mediaUrl
import com.soulmatch.app.data.models.ProfileData
import com.soulmatch.app.data.models.ProfilePhoto
import com.soulmatch.app.data.models.VerificationRequestData
import com.soulmatch.app.data.models.fullName
import com.soulmatch.app.ui.design.SoulMatchHeaderIconButton
import com.soulmatch.app.ui.design.SoulMatchTokens
import com.soulmatch.app.ui.titleCase
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
        stepNumber = null,
        progressPercent = null,
        headline = "",
        body = "",
        infoTitle = "Why profile setup matters",
        infoBody = "SoulMatch uses profile details, photos, trust checks, and partner preferences to rank better matches and help families review profiles faster.",
        onBack = onBack,
        primaryText = "Start Profile",
        onPrimary = onContinue
    ) {
        ProfileIntroHeroCard(progressPercent = 0)
        Text(
            text = "Build a profile families can trust",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 42.sp
            ),
            color = SoulMatchTokens.Text
        )
        Text(
            text = "We will guide you through personal details, values, career, family, lifestyle, partner preferences, photos, and verification.",
            style = MaterialTheme.typography.bodyLarge,
            color = SoulMatchTokens.Muted,
            lineHeight = 30.sp
        )
        CreateProfileInfoNotice()
        SetupBullet("Personal details and family context")
        SetupBullet("Partner preferences, photos, and verification")
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
    val uploadProgress by vm.photoUploadProgress.collectAsStateWithLifecycle()
    val uploadLabel by vm.photoUploadLabel.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.toPhotoPart(uri, photos.size)?.let { vm.uploadPhotos(listOf(it)) }
    }

    ProfileSetupScaffold(
        title = "Photo Upload",
        stepNumber = 7,
        progressPercent = 70,
        headline = "Add profile photos",
        body = "Use clear recent photos. Pick one primary photo. Real uploads only.",
        infoTitle = "Photo guidance",
        infoBody = "At least one clear face photo helps verification and trust. Primary photo shows first in match cards.",
        onBack = onBack,
        primaryText = "Continue",
        primaryEnabled = photos.isNotEmpty() && !isUploading,
        onPrimary = onContinue
    ) {
        PrimaryPhotoSlot(
            primaryPhoto = photos.firstOrNull { it.isPrimary } ?: photos.firstOrNull(),
            onAdd = {
                vm.clearStatus()
                picker.launch("image/*")
            },
            onRemove = {
                photos.firstOrNull { it.isPrimary }?.let { vm.deletePhoto(it.photoId) }
            }
        )
        if (isUploading && uploadLabel != null) {
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
                    Text(uploadLabel.orEmpty(), color = SoulMatchTokens.Tangerine, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = (uploadProgress.coerceIn(0, 100)) / 100f,
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = SoulMatchTokens.Tangerine,
                        trackColor = Color(0xFFF3E5DE)
                    )
                }
            }
        }
        PhotoGridRow(
            photos = photos,
            onAdd = {
                vm.clearStatus()
                picker.launch("image/*")
            },
            onMakePrimary = { vm.setPrimaryPhoto(it) },
            onDelete = { vm.deletePhoto(it) }
        )
        if (!status.isNullOrBlank()) {
            InlineNotice(status.orEmpty(), error = false)
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
    val profile by vm.profile.collectAsStateWithLifecycle()
    val verifications by vm.verifications.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val isSubmitting by vm.isSubmittingVerification.collectAsStateWithLifecycle()
    var documentType by rememberSaveable { mutableStateOf("aadhaar") }
    var referenceNumber by rememberSaveable { mutableStateOf("") }
    var selectedUri by rememberSaveable { mutableStateOf<String?>(null) }
    var showDocumentError by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedUri = uri?.toString()
        showDocumentError = false
    }

    ProfileSetupScaffold(
        title = "Verification",
        stepNumber = 8,
        progressPercent = 80,
        headline = "Submit trust checks",
        body = "Phone, profile, and optional identity checks help review move faster.",
        infoTitle = "Verification help",
        infoBody = "Profile verification marks the account for review. Identity documents add stronger trust signals when available.",
        onBack = onBack,
        primaryText = "Continue",
        primaryEnabled = !isSubmitting,
        onPrimary = {
            if (selectedUri.isNullOrBlank() && verifications.none { it.type.equals("identity", true) || it.type.equals("aadhaar", true) }) {
                showDocumentError = true
            } else {
                onContinue()
            }
        }
    ) {
        VerificationStatusRow(
            title = "Mobile verified",
            subtitle = if (profile?.isPhoneVerified == true) "Verified" else "Pending",
            verified = profile?.isPhoneVerified == true
        )
        VerificationStatusRow(
            title = "Email verified",
            subtitle = if (!profile?.email.isNullOrBlank()) "Available" else "Not added",
            verified = !profile?.email.isNullOrBlank()
        )
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
                    showDocumentError = false
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
        if (showDocumentError) {
            InlineNotice("ID verification document is required before review.", error = true)
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
    onEditSection: (Int) -> Unit,
    vm: MyProfileViewModel = hiltViewModel()
) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val photos by vm.photos.collectAsStateWithLifecycle()
    val verifications by vm.verifications.collectAsStateWithLifecycle()

    ProfileSetupScaffold(
        title = "Preview Profile",
        stepNumber = 9,
        progressPercent = 90,
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
        PreviewSection("Basic details", editStep = 1, onEdit = onEditSection) {
            PreviewLine("Name", profile?.fullName().orEmpty())
            PreviewLine("Gender", profile?.gender.orEmpty())
            PreviewLine("Current city", profile?.workingCity.orEmpty())
            PreviewLine("Native place", profile?.nativePlace.orEmpty())
        }
        PreviewSection("Religious details", editStep = 2, onEdit = onEditSection) {
            PreviewLine("Religion", profile?.religion.orEmpty())
            PreviewLine("Community", profile?.caste.orEmpty())
            PreviewLine("Sub-caste", profile?.subCaste.orEmpty())
            PreviewLine("Gothram", profile?.gotra.orEmpty())
        }
        PreviewSection("Education and career", editStep = 3, onEdit = onEditSection) {
            PreviewLine("Education", profile?.educationLevel.orEmpty())
            PreviewLine("Institution", profile?.institutionName.orEmpty())
            PreviewLine("Occupation", profile?.occupation.orEmpty())
            PreviewLine("Company", profile?.companyName.orEmpty())
        }
        PreviewSection("Family details", editStep = 4, onEdit = onEditSection) {
            PreviewLine("Family type", profile?.familyType.orEmpty())
            PreviewLine("Family status", profile?.familyStatus.orEmpty())
            PreviewLine("Father occupation", profile?.fatherOccupation.orEmpty())
            PreviewLine("Mother occupation", profile?.motherOccupation.orEmpty())
        }
        PreviewSection("Lifestyle", editStep = 5, onEdit = onEditSection) {
            PreviewLine("Diet", profile?.diet.orEmpty())
            PreviewLine("Smoking", profile?.smoking.orEmpty())
            PreviewLine("Drinking", profile?.drinking.orEmpty())
            PreviewLine("Hobbies", profile?.hobbies?.joinToString(", ").orEmpty())
        }
        PreviewSection("Partner preferences", editStep = 6, onEdit = onEditSection) {
            PreviewLine("Religion", profile?.partnerPreferences?.religion.orEmpty())
            PreviewLine("Education", profile?.partnerPreferences?.educationLevels?.joinToString(", ").orEmpty())
            PreviewLine("Occupation", profile?.partnerPreferences?.occupations?.joinToString(", ").orEmpty())
            PreviewLine("Location", profile?.locationPreferences?.joinToString(", ").orEmpty())
        }
        PreviewSection("Photos", editStep = 7, onEdit = onEditSection) {
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
        PreviewSection("Verification", editStep = 8, onEdit = onEditSection) {
            Text(
                if (verifications.isEmpty()) "No verification request yet" else "${verifications.size} verification request(s) submitted",
                color = SoulMatchTokens.Text
            )
        }
    }
}

@Composable
fun ProfileUnderReviewScreen(
    onPrimary: () -> Unit,
    onHelp: () -> Unit
) {
    ProfileSetupScaffold(
        title = "Under Review",
        stepNumber = 10,
        progressPercent = 100,
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
        title = "Needs Correction",
        stepNumber = 10,
        progressPercent = 100,
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
    stepNumber: Int? = null,
    progressPercent: Int? = null,
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
        ProfileInfoBottomSheet(
            title = infoTitle,
            body = infoBody,
            onDismiss = { showInfo = false }
        )
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(titleCase(title), fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    if (showBack) {
                        SoulMatchHeaderIconButton(icon = Icons.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)
                    }
                },
                actions = {
                    SoulMatchHeaderIconButton(icon = Icons.Filled.Info, contentDescription = "Info", onClick = { showInfo = true })
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
            if (stepNumber != null && progressPercent != null) {
                ProfileProgressHeader(stepNumber = stepNumber, progressPercent = progressPercent)
            }
            if (headline.isNotBlank() || body.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (headline.isNotBlank()) {
                        Text(
                            headline,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = SoulMatchTokens.Text
                        )
                    }
                    if (body.isNotBlank()) {
                        Text(body, style = MaterialTheme.typography.bodyMedium, color = SoulMatchTokens.Muted)
                    }
                }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(SoulMatchTokens.PillRadius),
                colors = ButtonDefaults.buttonColors(containerColor = SoulMatchTokens.Tangerine, contentColor = Color.White)
            ) {
                Text(primaryText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProfileProgressHeader(stepNumber: Int, progressPercent: Int) {
    val progress = (progressPercent.coerceIn(0, 100)) / 100f
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
private fun ProfileIntroHeroCard(progressPercent: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SoulMatchTokens.Border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = SoulMatchTokens.Gold,
                    modifier = Modifier.size(18.dp)
                )
            }
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(128.dp),
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(2.dp, SoulMatchTokens.Tangerine)
                ) {}
                Text(
                    text = "$progressPercent%",
                    color = SoulMatchTokens.Tangerine,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp
                )
            }
            Text(
                text = "Complete each section to unlock verified matches.",
                color = SoulMatchTokens.Muted,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp
            )
        }
    }
}

@Composable
private fun CreateProfileInfoNotice() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFFF8F0),
        border = BorderStroke(1.dp, SoulMatchTokens.Border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = SoulMatchTokens.Tangerine,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "Required fields and privacy-sensitive items will be checked before review.",
                color = SoulMatchTokens.Muted,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 28.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileInfoBottomSheet(
    title: String,
    body: String,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = SoulMatchTokens.Text
            )
            Text(
                body,
                style = MaterialTheme.typography.bodyLarge,
                color = SoulMatchTokens.Muted,
                lineHeight = 30.sp
            )
            SetupBullet("Verified profiles receive better responses.")
            SetupBullet("Privacy controls protect contact and photo access.")
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
                Text("Got it", fontWeight = FontWeight.Bold)
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
private fun PrimaryPhotoSlot(
    primaryPhoto: ProfilePhoto?,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SoulMatchTokens.Border)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Main profile photo", fontWeight = FontWeight.Bold, color = SoulMatchTokens.Text)
            if (primaryPhoto == null) {
                OutlinedButton(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(SoulMatchTokens.PillRadius)
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("Add main photo")
                }
            } else {
                AsyncImage(
                    model = mediaUrl(primaryPhoto.photoUrl),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentScale = ContentScale.Crop
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Primary photo selected", color = SoulMatchTokens.Tangerine, fontWeight = FontWeight.Bold)
                    Text(
                        "Remove photo",
                        color = SoulMatchTokens.Tangerine,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onRemove)
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoGridRow(
    photos: List<ProfilePhoto>,
    onAdd: () -> Unit,
    onMakePrimary: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), userScrollEnabled = false) {
        item {
            Text("Gallery", fontWeight = FontWeight.Bold, color = SoulMatchTokens.Text)
        }
        items(photos) { photo ->
            PhotoCard(
                photo = photo,
                onMakePrimary = { onMakePrimary(photo.photoId) },
                onDelete = { onDelete(photo.photoId) }
            )
        }
        item {
            OutlinedButton(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(SoulMatchTokens.PillRadius)
            ) {
                Text("Add photo")
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
private fun VerificationStatusRow(
    title: String,
    subtitle: String,
    verified: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SoulMatchTokens.Border)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.Bold, color = SoulMatchTokens.Text)
                Text(subtitle, color = if (verified) SoulMatchTokens.Success else SoulMatchTokens.Muted)
            }
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (verified) SoulMatchTokens.Success else SoulMatchTokens.Border
            )
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
private fun PreviewSection(
    title: String,
    editStep: Int,
    onEdit: (Int) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable(title) { mutableStateOf(true) }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    color = SoulMatchTokens.Text,
                    modifier = Modifier.clickable { expanded = !expanded }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (expanded) "Hide" else "Show",
                        color = SoulMatchTokens.Muted,
                        modifier = Modifier.clickable { expanded = !expanded }
                    )
                    Text(
                        "Edit",
                        color = SoulMatchTokens.Tangerine,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onEdit(editStep) }
                    )
                }
            }
            if (expanded) {
                content()
            }
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

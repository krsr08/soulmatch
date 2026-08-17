package com.soulmatch.app.ui.screens.profile

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.soulmatch.app.data.models.ProfileData
import com.soulmatch.app.data.models.ProfilePhoto
import com.soulmatch.app.data.models.fullName
import com.soulmatch.app.ui.components.media.MemberPhoto
import com.soulmatch.app.ui.components.premium.ChipTone
import com.soulmatch.app.ui.components.premium.PremiumCard
import com.soulmatch.app.ui.components.premium.SignalChip
import com.soulmatch.app.ui.components.premium.SignalChips
import com.soulmatch.app.ui.design.SoulMatchTokens
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.PrimaryDark
import com.soulmatch.app.ui.theme.SurfaceSoft
import com.soulmatch.app.ui.theme.TextSecondary
import com.soulmatch.app.ui.titleCase
import com.soulmatch.app.ui.viewmodels.ProfileChecklistItem
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.roundToInt

internal data class ReferenceField(val label: String, val value: String, val actionLabel: String? = null)

@Composable
internal fun ProfileReferenceHeader(
    profile: ProfileData,
    photos: List<ProfilePhoto>,
    localPhotoUris: List<Uri>,
    uploadingPhotos: Boolean,
    onUploadPhoto: () -> Unit
) {
    val primaryPhoto = photos.firstOrNull { it.isPrimary }?.photoUrl ?: profile.primaryPhotoUrl
    val identityLine = listOfNotBlank(
        profile.occupation,
        profile.workingCity,
        profile.motherTongue
    ).joinToString(" | ")
    val detailLine = listOfNotBlank(
        profile.religion,
        formatHeight(profile.heightCm),
        profile.maritalStatus
    ).joinToString(" | ")

    PremiumCard(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        containerColor = SoulMatchTokens.Card,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(contentAlignment = Alignment.BottomCenter) {
                if (localPhotoUris.isNotEmpty()) {
                    AsyncImage(
                        model = localPhotoUris.first(),
                        contentDescription = "Selected profile photo",
                        modifier = Modifier
                            .size(92.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    MemberPhoto(
                        photoUrl = primaryPhoto,
                        contentDescription = "Profile photo",
                        modifier = Modifier.size(92.dp),
                        shape = RoundedCornerShape(999.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    profile.fullName().ifBlank { "Complete your name" },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                if (identityLine.isNotBlank()) {
                    Text(
                        identityLine,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryDark,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    detailLine.ifBlank { "Add basic profile details" },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                OutlinedButton(onClick = onUploadPhoto, shape = RoundedCornerShape(999.dp)) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(if (uploadingPhotos) "Uploading..." else if (primaryPhoto.isNullOrBlank()) "Add photo" else "Manage photos")
                }
            }
        }
    }
}

@Composable
internal fun GoldBadgePromoCard(verified: Boolean, onClick: () -> Unit) {
    PremiumCard(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .clickable(onClick = onClick),
        containerColor = SoulMatchTokens.Card,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFFFF7DA)) {
                Icon(
                    Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint = Color(0xFFD6A319),
                    modifier = Modifier
                        .padding(10.dp)
                        .size(22.dp)
                )
            }
            Text(
                if (verified) "Verified Gold Badge active" else "Verify your profile to unlock the Gold Badge",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = PrimaryDark,
                fontWeight = FontWeight.SemiBold
            )
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}

@Composable
internal fun ProfileCompletionPromptCard(
    profile: ProfileData,
    checklist: List<ProfileChecklistItem>,
    onComplete: (() -> Unit)?
) {
    val score = profileCompletionScore(profile, checklist)
    PremiumCard(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .then(if (onComplete != null) Modifier.clickable(onClick = onComplete) else Modifier),
        containerColor = SoulMatchTokens.Card,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
                CircularProgressIndicator(
                    progress = score / 100f,
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = SurfaceSoft,
                    strokeWidth = 5.dp
                )
                Text("$score%", style = MaterialTheme.typography.labelSmall, color = PrimaryDark, fontWeight = FontWeight.ExtraBold)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    if (score >= 100) "Profile complete" else "Complete your profile",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryDark,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    if (score >= 100) "All required sections are filled." else "Fill the missing sections to improve match quality.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (onComplete != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Continue", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProfilePromptsCard(onAdd: () -> Unit) {
    PremiumCard(
        modifier = Modifier.padding(horizontal = 14.dp),
        containerColor = SoulMatchTokens.Card,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(14.dp), color = SoulMatchTokens.Ivory) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp).size(24.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Prompts", style = MaterialTheme.typography.titleMedium, color = PrimaryDark, fontWeight = FontWeight.ExtraBold)
                    SignalChip("New", tone = ChipTone.Warm)
                }
                Text("Add a short answer to make conversations easier.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            TextButton(onClick = onAdd) {
                Text("Add", fontWeight = FontWeight.ExtraBold)
                Icon(Icons.Filled.ChevronRight, contentDescription = null)
            }
        }
    }
}

@Composable
internal fun ReferenceInfoSection(
    title: String,
    icon: ImageVector,
    editStep: Int,
    onEdit: (Int) -> Unit,
    items: List<ReferenceField>,
    singleColumn: Boolean = false,
    footer: (@Composable () -> Unit)? = null
) {
    var expanded by rememberSaveable(title) { mutableStateOf(true) }
    val collapsedSummary = items
        .mapNotNull { field ->
            field.value
                .replace('\n', ',')
                .trim()
                .takeIf { it.isNotBlank() && !it.equals("Not Filled", ignoreCase = true) }
        }
        .distinct()
        .take(3)
        .joinToString(", ")

    PremiumCard(
        modifier = Modifier.padding(horizontal = 14.dp),
        containerColor = SoulMatchTokens.Card,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(if (expanded) 0f else -90f)
                )
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = PrimaryDark, fontWeight = FontWeight.ExtraBold)
                IconButton(onClick = { onEdit(editStep) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit $title", tint = TextSecondary)
                }
            }
            if (expanded) {
                if (singleColumn) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items.forEach { ReferenceFieldRow(it) }
                    }
                } else {
                    items.chunked(2).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                            rowItems.forEach { field ->
                                Box(modifier = Modifier.weight(1f)) {
                                    ReferenceFieldRow(field)
                                }
                            }
                            if (rowItems.size == 1) {
                                Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                footer?.invoke()
            } else if (collapsedSummary.isNotBlank()) {
                Text(
                    collapsedSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                Text(
                    "No details added yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun MoreControlRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SoulMatchTokens.Card,
        border = BorderStroke(1.dp, Divider.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SoulMatchTokens.Ivory,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = PrimaryDark, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            trailing()
        }
    }
}

@Composable
private fun ReferenceFieldRow(field: ReferenceField) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(field.label, style = MaterialTheme.typography.titleSmall, color = PrimaryDark, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                field.value.ifBlank { "Not Filled" },
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.bodyLarge,
                color = if (field.value.isBlank()) MaterialTheme.colorScheme.primary else TextSecondary
            )
            field.actionLabel?.let {
                Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
internal fun JanampatriPromptCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SoulMatchTokens.Border)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Unlock your Janampatri for free by adding time and place of birth!",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = PrimaryDark,
                fontWeight = FontWeight.ExtraBold
            )
            Surface(shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary), color = Color.Transparent) {
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@Composable
internal fun InterestPromptCard(onClick: () -> Unit) {
    PremiumCard(
        modifier = Modifier.padding(horizontal = 14.dp),
        containerColor = SoulMatchTokens.Card,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SignalChips(labels = listOf("Photography", "Cooking", "Travelling", "Reading"), tone = ChipTone.Info)
            Text("What's your vibe?", style = MaterialTheme.typography.labelLarge, color = PrimaryDark, fontWeight = FontWeight.Bold)
            Text(
                "Your interests say a lot about you - add yours and let the right person notice.",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryDark,
                fontWeight = FontWeight.ExtraBold
            )
            Button(onClick = onClick, shape = RoundedCornerShape(999.dp)) {
                Text("Pick your interests", fontWeight = FontWeight.ExtraBold)
                Icon(Icons.Filled.ChevronRight, contentDescription = null)
            }
        }
    }
}

@Composable
internal fun MoreProfileActionsCard(
    viewerCount: Int,
    showViewers: Boolean,
    verificationVisible: Boolean,
    assistEnabled: Boolean,
    isSavingAssist: Boolean,
    onToggleViewers: () -> Unit,
    onOpenTrust: () -> Unit,
    onOpenAssist: () -> Unit,
    onToggleAssist: (Boolean) -> Unit,
    onSubscribe: () -> Unit,
    onSettings: () -> Unit
) {
    PremiumCard(
        modifier = Modifier.padding(horizontal = 14.dp),
        containerColor = SoulMatchTokens.Card,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("More controls", style = MaterialTheme.typography.titleMedium, color = PrimaryDark, fontWeight = FontWeight.ExtraBold)
            Text(
                "Open trust checks, assist controls, settings, and viewer tools from one place.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            MoreControlRow(
                icon = Icons.Filled.Verified,
                title = if (verificationVisible) "Profile verification" else "Trust details",
                subtitle = if (verificationVisible) "Open verification and trust checks." else "Review trust status and verification details."
            ) {
                TextButton(onClick = onOpenTrust) { Text("Open") }
            }
            MoreControlRow(
                icon = Icons.Filled.RemoveRedEye,
                title = "Recent viewers",
                subtitle = "$viewerCount viewer${if (viewerCount == 1) "" else "s"}"
            ) {
                Switch(checked = showViewers, onCheckedChange = { onToggleViewers() })
            }
            MoreControlRow(
                icon = Icons.Filled.AutoAwesome,
                title = "SoulMatch Assist",
                subtitle = if (assistEnabled) "Assistance is enabled for your profile." else "Turn on assisted discovery and support."
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onOpenAssist) { Text("Details") }
                    Switch(
                        enabled = !isSavingAssist,
                        checked = assistEnabled,
                        onCheckedChange = onToggleAssist
                    )
                }
            }
            MoreControlRow(
                icon = Icons.Filled.Settings,
                title = "Settings",
                subtitle = "Privacy, notifications, blocked users, and support."
            ) {
                TextButton(onClick = onSettings) { Text("Open") }
            }
            OutlinedButton(onClick = onSubscribe, modifier = Modifier.fillMaxWidth()) {
                Text("Upgrade membership")
            }
        }
    }
}

internal fun profileCompletionScore(profile: ProfileData, checklist: List<ProfileChecklistItem>): Int {
    if (
        profile.reviewStatus.equals("submitted", ignoreCase = true) ||
        profile.reviewStatus.equals("under_review", ignoreCase = true) ||
        profile.reviewStatus.equals("approved", ignoreCase = true)
    ) {
        return 100
    }
    if (checklist.isEmpty()) return 0
    val sectionScore = ((checklist.count { it.isComplete }.toFloat() / checklist.size.toFloat()) * 100).roundToInt().coerceIn(0, 100)
    return if (sectionScore == 100) 100 else profile.completionScore.takeIf { it > 0 }?.coerceIn(0, 100) ?: sectionScore
}

internal fun basicProfileItems(profile: ProfileData): List<ReferenceField> = listOf(
    ReferenceField("Religion / Height", listOfNotBlank(profile.religion, formatHeight(profile.heightCm)).joinToString(" - ")),
    ReferenceField("Income", profile.annualIncome),
    ReferenceField("Caste", listOfNotBlank(profile.caste, profile.gotra).joinToString(" - ")),
    ReferenceField("Mother Tongue", profile.motherTongue),
    ReferenceField("Location", listOfNotBlank(profile.familyCity, profile.familyState).joinToString(", ")),
    ReferenceField("Marital Status", profile.maritalStatus),
    ReferenceField("Date of Birth", formatDate(profile.dob))
)

internal fun aboutProfileItems(profile: ProfileData): List<ReferenceField> = listOf(
    ReferenceField("About Me", profile.aboutMe),
    ReferenceField("Describe yourself in 5 words", ""),
    ReferenceField("Profile Created by", titleCase(profile.profileCreatedBy.ifBlank { "self" })),
    ReferenceField("Language known", profile.motherTongue),
    ReferenceField("Special Cases", "")
)

internal fun educationProfileItems(profile: ProfileData): List<ReferenceField> = listOf(
    ReferenceField("About My Education", ""),
    ReferenceField("Education status", if (profile.noEducation) "No Education" else profile.educationLevel),
    ReferenceField("Highest Degree", if (profile.noEducation) "Not applicable" else profile.educationLevel),
    ReferenceField("Under Graduation", if (profile.noEducation) "Not applicable" else profile.educationLevel),
    ReferenceField("School", "")
)

internal fun careerProfileItems(profile: ProfileData): List<ReferenceField> = listOf(
    ReferenceField("About My Career", ""),
    ReferenceField("Employed In", if (profile.isEmployed) "Employed" else ""),
    ReferenceField("Occupation", profile.occupation),
    ReferenceField("Annual Income", profile.annualIncome),
    ReferenceField("Working Location", listOfNotBlank(profile.workingCity, profile.workingState).joinToString(", ")),
    ReferenceField("Interested in settling abroad?", "")
)

internal fun familyProfileItems(profile: ProfileData): List<ReferenceField> = listOf(
    ReferenceField("About My Family", ""),
    ReferenceField("Family Background", profile.familyType),
    ReferenceField("Family Income", ""),
    ReferenceField("Father is", profile.fatherOccupation),
    ReferenceField("Mother is", profile.motherOccupation),
    ReferenceField("Brother/Sister", siblingSummary(profile)),
    ReferenceField("Living With Parents?", ""),
    ReferenceField("Family based out of", listOfNotBlank(profile.familyCity, profile.familyState).joinToString(", "))
)

internal fun contactProfileItems(profile: ProfileData): List<ReferenceField> = listOf(
    ReferenceField("Email ID", profile.email, if (profile.email.isNotBlank() && !profile.verificationStatus.equals("verified", ignoreCase = true)) "VERIFY" else null),
    ReferenceField("Alternate Email ID", ""),
    ReferenceField("Mobile no.", profile.phone.ifBlank { profile.maskedPhone }),
    ReferenceField("Alt. Mobile no", ""),
    ReferenceField("Landline no.", "")
)

internal fun horoscopeProfileItems(profile: ProfileData): List<ReferenceField> = listOf(
    ReferenceField("City, Country of Birth", profile.birthCity),
    ReferenceField("Date & Time of Birth", listOfNotBlank(formatDate(profile.dob), "Not Available").joinToString(", ")),
    ReferenceField("Rashi/Moon Sign?", profile.rashi),
    ReferenceField("Nakshatra?", profile.nakshatra),
    ReferenceField("Manglik Status", if (profile.isManglik) "Manglik" else "Non Manglik")
)

internal fun lifestyleProfileItems(profile: ProfileData): List<ReferenceField> = listOf(
    ReferenceField("Habits", listOfNotBlank(profile.diet, profile.smoking, profile.drinking).joinToString(" - ")),
    ReferenceField("Assets", ""),
    ReferenceField("Food I cook", ""),
    ReferenceField("Hobbies", ""),
    ReferenceField("Favourite Music", ""),
    ReferenceField("Favorite books", ""),
    ReferenceField("Dress style", ""),
    ReferenceField("Sports", ""),
    ReferenceField("Favorite Cuisine", profile.diet),
    ReferenceField("Favorite Movies", ""),
    ReferenceField("Favourite Read", ""),
    ReferenceField("Favorite TV shows", ""),
    ReferenceField("Vacation Destination", "")
)

private fun listOfNotBlank(vararg values: String?): List<String> {
    return values.mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
}

private fun formatHeight(heightCm: Int?): String {
    val cm = heightCm ?: return ""
    if (cm <= 0) return ""
    val totalInches = (cm / 2.54).roundToInt()
    return "${totalInches / 12}' ${totalInches % 12}\""
}

private fun siblingSummary(profile: ProfileData): String {
    val brothers = profile.numBrothers?.let { "$it brother${if (it == 1) "" else "s"}" }
    val sisters = profile.numSisters?.let { "$it sister${if (it == 1) "" else "s"}" }
    return listOfNotBlank(brothers, sisters).joinToString("\n")
}

private fun formatDate(value: String?): String {
    val raw = value?.trim().orEmpty()
    if (raw.isBlank()) return ""
    val outputFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    return try {
        OffsetDateTime.parse(raw).toLocalDate().format(outputFormatter)
    } catch (_: DateTimeParseException) {
        try {
            LocalDate.parse(raw).format(outputFormatter)
        } catch (_: DateTimeParseException) {
            raw
        }
    }
}


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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.PrimaryDark
import com.soulmatch.app.ui.theme.SurfaceSoft
import com.soulmatch.app.ui.theme.SurfaceWarm
import com.soulmatch.app.ui.theme.TextSecondary
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
    PremiumCard(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        containerColor = Color.White,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(contentAlignment = Alignment.BottomCenter) {
                if (localPhotoUris.isNotEmpty()) {
                    AsyncImage(
                        model = localPhotoUris.first(),
                        contentDescription = "Selected profile photo",
                        modifier = Modifier
                            .size(112.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    MemberPhoto(
                        photoUrl = primaryPhoto,
                        contentDescription = "Profile photo",
                        modifier = Modifier.size(112.dp),
                        shape = RoundedCornerShape(999.dp)
                    )
                }
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(onClick = onUploadPhoto),
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text(
                            if (uploadingPhotos) "Uploading..." else if (primaryPhoto.isNullOrBlank()) "Add photos" else "Manage photos",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
            Text(
                profile.fullName().ifBlank { "Complete your name" },
                style = MaterialTheme.typography.titleLarge,
                color = PrimaryDark,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                BulletLine("Profile with photo gives better response")
                BulletLine("Add quality photos, your photos are safe with us")
            }
        }
    }
}

@Composable
private fun BulletLine(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Text("-", color = Divider, style = MaterialTheme.typography.bodyMedium)
        Text(text, color = PrimaryDark, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun GoldBadgePromoCard(verified: Boolean, onClick: () -> Unit) {
    PremiumCard(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .clickable(onClick = onClick),
        containerColor = Color.White,
        contentPadding = PaddingValues(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                if (verified) "Your profile has a verified Gold Badge." else "Get a verified Gold Badge, stand out, and connect with more genuine profiles.",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = PrimaryDark,
                fontWeight = FontWeight.SemiBold
            )
            Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = Color(0xFFD6A319), modifier = Modifier.size(42.dp))
        }
    }
}

@Composable
internal fun ProfileCompletionPromptCard(
    profile: ProfileData,
    checklist: List<ProfileChecklistItem>,
    onComplete: () -> Unit
) {
    val score = profileCompletionScore(profile, checklist)
    PremiumCard(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .clickable(onClick = onComplete),
        containerColor = Color.White,
        contentPadding = PaddingValues(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                CircularProgressIndicator(
                    progress = { score / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = SurfaceSoft,
                    strokeWidth = 7.dp
                )
                Text("$score%", style = MaterialTheme.typography.labelLarge, color = PrimaryDark, fontWeight = FontWeight.ExtraBold)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Add a few more details to make your profile rich!", style = MaterialTheme.typography.titleMedium, color = PrimaryDark, fontWeight = FontWeight.ExtraBold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Complete your profile", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
internal fun ProfilePromptsCard(onAdd: () -> Unit) {
    PremiumCard(
        modifier = Modifier.padding(horizontal = 14.dp),
        containerColor = Color.White,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SignalChip("New", tone = ChipTone.Warm)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Prompts!", style = MaterialTheme.typography.titleLarge, color = PrimaryDark, fontWeight = FontWeight.ExtraBold)
                    Text("Share fun facts, opinions, and more.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
            }
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                Text("Add now", fontWeight = FontWeight.ExtraBold)
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
    PremiumCard(
        modifier = Modifier.padding(horizontal = 14.dp),
        containerColor = Color.White,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, contentDescription = null, tint = PrimaryDark, modifier = Modifier.size(28.dp))
                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = PrimaryDark, fontWeight = FontWeight.ExtraBold)
                IconButton(onClick = { onEdit(editStep) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit $title", tint = TextSecondary)
                }
            }
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
        color = Color(0xFFFFF5E7)
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
        containerColor = SurfaceWarm,
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
    onToggleAssist: () -> Unit,
    onSubscribe: () -> Unit,
    onSettings: () -> Unit
) {
    PremiumCard(
        modifier = Modifier.padding(horizontal = 14.dp),
        containerColor = SurfaceSoft,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("More controls", style = MaterialTheme.typography.titleMedium, color = PrimaryDark, fontWeight = FontWeight.ExtraBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onOpenTrust, modifier = Modifier.weight(1f)) {
                    Text(if (verificationVisible) "Verify" else "Trust")
                }
                OutlinedButton(onClick = onSubscribe, modifier = Modifier.weight(1f)) {
                    Text("Upgrade")
                }
                OutlinedButton(onClick = onSettings, modifier = Modifier.weight(1f)) {
                    Text("Settings")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Recent viewers: $viewerCount", modifier = Modifier.weight(1f), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Switch(checked = showViewers, onCheckedChange = { onToggleViewers() })
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("SoulMatch Assist", modifier = Modifier.weight(1f), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onOpenAssist) { Text("Details") }
                Switch(enabled = !isSavingAssist, checked = assistEnabled, onCheckedChange = { onToggleAssist() })
            }
        }
    }
}

internal fun profileCompletionScore(profile: ProfileData, checklist: List<ProfileChecklistItem>): Int {
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

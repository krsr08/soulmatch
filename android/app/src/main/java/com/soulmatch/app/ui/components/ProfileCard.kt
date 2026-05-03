package com.soulmatch.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soulmatch.app.data.models.ProfileSummary
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.Error
import com.soulmatch.app.ui.theme.PrimaryDark
import com.soulmatch.app.ui.theme.Success
import com.soulmatch.app.ui.theme.TextSecondary
import com.soulmatch.app.ui.theme.Warning

@Composable
fun ProfileCard(
    profile: ProfileSummary,
    onSendInterest: (String) -> Unit,
    onViewProfile: (String) -> Unit,
    onShortlist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onViewProfile(profile.profileId) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Divider.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val photoWidth = (maxWidth * 0.32f).coerceIn(104.dp, 124.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = profile.name.ifBlank { "SoulMatch member" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(6.dp))
                        VerificationBadge(isVerified = profile.isVerified)
                    }
                    Text(
                        text = profile.occupation.ifBlank { "Profession not added" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    ProfileOwnerTag(profile.profileCreatedBy)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LabeledInlineValue(label = "Age", value = "${profile.age.coerceAtLeast(0)} yrs")
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .width(1.dp)
                                .height(28.dp)
                                .background(Divider.copy(alpha = 0.58f))
                        )
                        LabeledInlineValue(label = "Height", value = profile.heightCm?.let { formatHeight(it) } ?: "Not added")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = profile.location.ifBlank { "Location not added" },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    ProfileMatchSignal(score = profile.compatibilityScore)
                }

                Box(
                    modifier = Modifier
                        .width(photoWidth)
                        .height(136.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    MemberPhoto(
                        photoUrl = profile.primaryPhoto,
                        contentDescription = "Photo of ${profile.name}",
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (profile.isPhotoPrivate) Modifier.blur(12.dp) else Modifier)
                    )
                    if (profile.isPhotoPrivate && profile.primaryPhoto.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier.align(Alignment.Center),
                            shape = RoundedCornerShape(999.dp),
                            color = Color.White.copy(alpha = 0.86f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.72f))
                        ) {
                            Text(
                                "Request photo",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryDark,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        PhotoCornerAction(
                            selected = profile.interestSent,
                            selectedContentDescription = "Interest sent",
                            unselectedContentDescription = "Send interest",
                            onClick = { onSendInterest(profile.profileId) }
                        ) {
                            Icon(
                                imageVector = if (profile.interestSent) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        PhotoCornerAction(
                            selected = profile.shortlisted,
                            selectedContentDescription = "Saved profile",
                            unselectedContentDescription = "Save profile",
                            onClick = { onShortlist(profile.profileId) }
                        ) {
                            Icon(
                                imageVector = if (profile.shortlisted) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileOwnerTag(profileCreatedBy: String) {
    val owner = if (profileCreatedBy.equals("mediator", ignoreCase = true)) "Mediator" else "Self"
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
    ) {
        Text(
            text = "Profile created by: $owner",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = PrimaryDark,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LabeledInlineValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VerificationBadge(isVerified: Boolean) {
    val background = if (isVerified) Success.copy(alpha = 0.12f) else Warning.copy(alpha = 0.14f)
    val content = if (isVerified) Success else Warning
    val label = if (isVerified) "Verified" else "Not Verified"
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = background,
        border = BorderStroke(1.dp, content.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(Icons.Filled.Verified, contentDescription = label, tint = content, modifier = Modifier.size(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = content,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PhotoCornerAction(
    selected: Boolean,
    selectedContentDescription: String,
    unselectedContentDescription: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(32.dp)
            .semantics { contentDescription = if (selected) selectedContentDescription else unselectedContentDescription }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
        contentColor = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.94f)
    ) {
        Box(contentAlignment = Alignment.Center) { icon() }
    }
}

@Composable
private fun ProfileMatchSignal(score: Int) {
    val content = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Profile Match",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(5.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(content.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(score.coerceIn(0, 100) / 100f)
                    .clip(RoundedCornerShape(999.dp))
                    .background(content)
            )
        }
        Text(
            text = "$score%",
            style = MaterialTheme.typography.labelMedium,
            color = content,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}

@Composable
fun CompatibilityBar(score: Int) {
    val color = when {
        score >= 90 -> Success
        score >= 80 -> Warning
        else -> Error
    }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Compatibility signal", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text("$score%", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.ExtraBold)
        }
        LinearProgressIndicator(
            progress = score.coerceIn(0, 100) / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}

private fun formatHeight(heightCm: Int): String {
    val totalInches = (heightCm / 2.54f).toInt()
    val feet = totalInches / 12
    val inches = totalInches % 12
    return "$feet' $inches\""
}

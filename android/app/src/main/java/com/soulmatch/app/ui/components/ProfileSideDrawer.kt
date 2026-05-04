package com.soulmatch.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soulmatch.app.ui.theme.SurfaceSoft
import com.soulmatch.app.ui.theme.TextSecondary

object ProfileDrawerRoutes {
    const val EditProfile = "edit_profile"
    const val PartnerPreference = "partner_preference"
    const val SoulMatchAssist = "soulmatch_assist"
    const val Spotlight = "spotlight"
    const val AstrologyServices = "astrology_services"
    const val AccountSettings = "account_settings"
    const val SafetyCenter = "safety_center"
    const val SubscriptionHistory = "subscription_history"
    const val HelpSupport = "help_support"
    const val SuccessStories2026 = "success_stories_2026"
    const val SuccessStories2025 = "success_stories_2025"
    const val SuccessStories2024 = "success_stories_2024"
}

private data class DrawerAction(
    val label: String,
    val route: String,
    val icon: ImageVector
)

private val primaryActions = listOf(
    DrawerAction("Edit Profile", ProfileDrawerRoutes.EditProfile, Icons.Outlined.Edit),
    DrawerAction("Partner Preference", ProfileDrawerRoutes.PartnerPreference, Icons.Outlined.Tune),
    DrawerAction("SoulMatch Assist", ProfileDrawerRoutes.SoulMatchAssist, Icons.Outlined.SupportAgent),
    DrawerAction("Spotlight", ProfileDrawerRoutes.Spotlight, Icons.Outlined.WorkspacePremium),
    DrawerAction("Astrology Services", ProfileDrawerRoutes.AstrologyServices, Icons.Outlined.AutoAwesome),
    DrawerAction("Account & Settings", ProfileDrawerRoutes.AccountSettings, Icons.Outlined.Settings),
    DrawerAction("Safety Center", ProfileDrawerRoutes.SafetyCenter, Icons.Outlined.VerifiedUser),
    DrawerAction("Subscription History", ProfileDrawerRoutes.SubscriptionHistory, Icons.Outlined.ReceiptLong),
    DrawerAction("Help & Support", ProfileDrawerRoutes.HelpSupport, Icons.Outlined.HelpOutline)
)

@Composable
fun ProfileSideDrawer(
    drawerState: DrawerState,
    profileName: String,
    profilePhotoUrl: String?,
    profileId: String = "",
    isVerified: Boolean,
    membershipLabel: String = "Free member",
    onDestinationSelected: (String) -> Unit,
    content: @Composable () -> Unit
) {
    var storiesExpanded by rememberSaveable { mutableStateOf(false) }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.86f),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    DrawerHeader(
                        profileName = profileName,
                        profilePhotoUrl = profilePhotoUrl,
                        profileId = profileId,
                        isVerified = isVerified,
                        membershipLabel = membershipLabel
                    )
                    primaryActions.forEach { action ->
                        DrawerItem(action = action, onClick = { onDestinationSelected(action.route) })
                    }
                    NavigationDrawerItem(
                        label = { Text("Success Stories") },
                        selected = false,
                        icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                        badge = {
                            Icon(
                                imageVector = if (storiesExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null
                            )
                        },
                        onClick = { storiesExpanded = !storiesExpanded },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = SurfaceSoft)
                    )
                    if (storiesExpanded) {
                        YearItem("2026", ProfileDrawerRoutes.SuccessStories2026, onDestinationSelected)
                        YearItem("2025", ProfileDrawerRoutes.SuccessStories2025, onDestinationSelected)
                        YearItem("2024", ProfileDrawerRoutes.SuccessStories2024, onDestinationSelected)
                    }
                }
            }
        },
        content = content
    )
}

@Composable
private fun DrawerHeader(
    profileName: String,
    profilePhotoUrl: String?,
    profileId: String,
    isVerified: Boolean,
    membershipLabel: String
) {
    val statusColor = if (isVerified) MaterialTheme.colorScheme.primary else Color(0xFFA7A29F)
    val memberPillColor = if (isVerified) Color(0xFFFFE6EF) else Color(0xFFF5F3F1)
    val memberPillBorder = if (isVerified) Color(0xFFF0B5CB) else Color(0xFFE0DCDA)
    val memberPillText = if (isVerified) MaterialTheme.colorScheme.primary else Color(0xFF6F6864)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(modifier = Modifier.padding(start = 2.dp)) {
            Surface(
                modifier = Modifier.size(92.dp),
                shape = RoundedCornerShape(999.dp),
                color = Color(0xFFFFF1F6),
                border = BorderStroke(5.dp, Color(0xFFF4C3D5))
            ) {
                MemberPhoto(
                    photoUrl = profilePhotoUrl,
                    contentDescription = "Profile photo",
                    modifier = Modifier
                        .padding(5.dp)
                        .fillMaxSize(),
                    shape = RoundedCornerShape(999.dp)
                )
            }
            if (isVerified) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(34.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = "Verified profile",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
        Text(
            profileName.ifBlank { "SoulMatch member" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Surface(
                modifier = Modifier.size(10.dp),
                shape = RoundedCornerShape(999.dp),
                color = statusColor
            ) {}
            Text(
                if (isVerified) "Verified profile" else "Unverified profile",
                style = MaterialTheme.typography.bodyMedium,
                color = statusColor,
                fontWeight = FontWeight.Bold
            )
        }
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = memberPillColor,
            border = BorderStroke(1.dp, memberPillBorder)
        ) {
            Text(
                membershipLabel.ifBlank { "Free member" }.uppercase(),
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = memberPillText,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            "ID: ${formatSoulMatchId(profileId)}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DrawerItem(action: DrawerAction, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(action.label) },
        selected = false,
        icon = { Icon(action.icon, contentDescription = null) },
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = MaterialTheme.colorScheme.surface,
            unselectedIconColor = Color(0xFF4A252E),
            unselectedTextColor = Color(0xFF4A252E)
        )
    )
}

@Composable
private fun YearItem(year: String, route: String, onDestinationSelected: (String) -> Unit) {
    NavigationDrawerItem(
        label = { Text(year) },
        selected = false,
        icon = { Spacer(Modifier.size(24.dp)) },
        onClick = { onDestinationSelected(route) },
        modifier = Modifier.padding(start = 18.dp),
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = MaterialTheme.colorScheme.surface)
    )
}

private fun formatSoulMatchId(profileId: String): String {
    val trimmed = profileId.trim()
    if (trimmed.isBlank()) return "SM----"
    if (trimmed.startsWith("SM-", ignoreCase = true)) return trimmed.uppercase()
    val digits = trimmed.filter { it.isDigit() }.takeLast(4)
    if (digits.isNotBlank()) return "SM-${digits.padStart(4, '0')}"
    return "SM-${java.lang.Math.floorMod(trimmed.hashCode(), 10000).toString().padStart(4, '0')}"
}

package com.soulmatch.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.SurfaceSoft
import com.soulmatch.app.ui.theme.TextSecondary

object ProfileDrawerRoutes {
    const val EditProfile = "edit_profile"
    const val PartnerPreference = "partner_preference"
    const val Spotlight = "spotlight"
    const val AstrologyServices = "astrology_services"
    const val AccountSettings = "account_settings"
    const val SafetyCenter = "safety_center"
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
    DrawerAction("Edit Profile", ProfileDrawerRoutes.EditProfile, Icons.Filled.Edit),
    DrawerAction("Partner Preference", ProfileDrawerRoutes.PartnerPreference, Icons.Filled.Favorite),
    DrawerAction("Spotlight", ProfileDrawerRoutes.Spotlight, Icons.Filled.Star),
    DrawerAction("Astrology Services", ProfileDrawerRoutes.AstrologyServices, Icons.Filled.Star),
    DrawerAction("Account & Settings", ProfileDrawerRoutes.AccountSettings, Icons.Filled.Settings),
    DrawerAction("Safety Center", ProfileDrawerRoutes.SafetyCenter, Icons.Filled.Lock),
    DrawerAction("Help & Support", ProfileDrawerRoutes.HelpSupport, Icons.Filled.Help)
)

@Composable
fun ProfileSideDrawer(
    drawerState: DrawerState,
    profileName: String,
    profilePhotoUrl: String?,
    isVerified: Boolean,
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
                        isVerified = isVerified
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
    isVerified: Boolean
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceSoft,
        border = BorderStroke(1.dp, Divider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MemberPhoto(
                photoUrl = profilePhotoUrl,
                contentDescription = "Profile photo",
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(999.dp)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    profileName.ifBlank { "SoulMatch member" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = if (isVerified) Icons.Filled.Verified else Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        if (isVerified) "Verified profile" else "Verification pending",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerItem(action: DrawerAction, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(action.label) },
        selected = false,
        icon = { Icon(action.icon, contentDescription = null) },
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = MaterialTheme.colorScheme.surface)
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

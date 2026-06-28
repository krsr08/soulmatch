package com.soulmatch.app.ui.screens.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.ui.components.premium.PremiumCard
import com.soulmatch.app.ui.components.premium.PremiumScreen
import com.soulmatch.app.ui.components.premium.SectionTitle
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.Error
import com.soulmatch.app.ui.theme.ErrorSoft
import com.soulmatch.app.ui.theme.Info
import com.soulmatch.app.ui.theme.InfoSoft
import com.soulmatch.app.ui.theme.PrimaryDark
import com.soulmatch.app.ui.theme.Success
import com.soulmatch.app.ui.theme.SuccessSoft
import com.soulmatch.app.ui.theme.SurfaceSoft
import com.soulmatch.app.ui.theme.SurfaceWarm
import com.soulmatch.app.ui.theme.TextSecondary
import com.soulmatch.app.ui.viewmodels.SettingsUiState
import com.soulmatch.app.ui.viewmodels.SettingsViewModel
import com.soulmatch.app.ui.viewmodels.PrivacyMemberUi
import com.soulmatch.app.ui.viewmodels.ReportedConcernUi
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onViewProfile: ((String) -> Unit)? = null,
    onOpenChat: ((String, String) -> Unit)? = null,
    onSubscribe: (() -> Unit)? = null,
    onEditSection: ((Int) -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    onOpenPrivacy: (() -> Unit)? = null,
    onOpenNotifications: (() -> Unit)? = null,
    onOpenPayment: (() -> Unit)? = null,
    onOpenDeleteAccount: (() -> Unit)? = null,
    onOpenLogout: (() -> Unit)? = null,
    profileId: String = "",
    chatId: String = "",
    participantName: String = "",
    vm: SettingsViewModel = hiltViewModel()
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val logout: () -> Unit = onLogout ?: {}

    LaunchedEffect(status) {
        if (status != null) {
            delay(2200)
            vm.clearStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        PremiumScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!status.isNullOrBlank()) {
                    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp), containerColor = SuccessSoft) {
                        Text(status ?: "", style = MaterialTheme.typography.bodyMedium, color = Success, fontWeight = FontWeight.SemiBold)
                    }
                }
                SettingsNavigationCard(
                    onOpenPrivacy = onOpenPrivacy,
                    onOpenNotifications = onOpenNotifications,
                    onOpenPayment = onOpenPayment,
                    onOpenDeleteAccount = onOpenDeleteAccount,
                    onOpenLogout = onOpenLogout
                )
                PrivacyControlsCard(settings = settings, vm = vm)
                ContactAndVisibilityCard(settings = settings, vm = vm)
                DataRightsCard(
                    onExport = { vm.exportMyData() },
                    onDelete = { onOpenDeleteAccount?.invoke() ?: vm.deleteAccount(logout) }
                )
                SupportCard(onLogout = { onOpenLogout?.invoke() ?: vm.logout(logout) })
                Box(Modifier.padding(bottom = 8.dp))
            }
        }
    }
}

@Composable
private fun SettingsNavigationCard(
    onOpenPrivacy: (() -> Unit)?,
    onOpenNotifications: (() -> Unit)?,
    onOpenPayment: (() -> Unit)?,
    onOpenDeleteAccount: (() -> Unit)?,
    onOpenLogout: (() -> Unit)?
) {
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp), containerColor = MaterialTheme.colorScheme.surface) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingsNavRow(Icons.Filled.PrivacyTip, "Privacy settings", "Control phone, email, photos, and profile visibility", onOpenPrivacy)
            SettingsNavRow(Icons.Filled.Notifications, "Notification settings", "Manage match, interest, message, and payment alerts", onOpenNotifications)
            SettingsNavRow(Icons.Filled.Payment, "Payment", "Review selected plan and secure checkout", onOpenPayment)
            SettingsNavRow(Icons.Filled.Delete, "Delete account", "Permanently remove your SoulMatch profile", onOpenDeleteAccount, danger = true)
            SettingsNavRow(Icons.Filled.Logout, "Logout", "Sign out from this phone", onOpenLogout)
        }
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: (() -> Unit)?,
    danger: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(14.dp),
        color = if (danger) ErrorSoft else SurfaceSoft,
        border = BorderStroke(1.dp, Divider)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (danger) Error else PrimaryDark, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PrivacyControlsCard(settings: SettingsUiState, vm: SettingsViewModel) {
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp), containerColor = MaterialTheme.colorScheme.surface) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionTitle("Basic controls", "Simple choices for daily use")
            SettingToggle(
                icon = Icons.Filled.Notifications,
                title = "Push notifications",
                description = "New interests, chat replies, viewers, and match recommendations",
                checked = settings.pushEnabled,
                onCheckedChange = { vm.setPushNotifications(it) }
            )
            SettingToggle(
                icon = Icons.Filled.Lock,
                title = "Require approval for photos",
                description = if (settings.photoPrivacyEnabled) "Members see Request photo until you approve access" else "Everyone can see photos",
                checked = settings.photoPrivacyEnabled,
                onCheckedChange = { vm.setPrivacy(photoPrivate = it, visible = settings.profileVisible) }
            )
        }
    }
}

@Composable
private fun ContactAndVisibilityCard(settings: SettingsUiState, vm: SettingsViewModel) {
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp), containerColor = SurfaceWarm) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionTitle("Who can see your profile?", "Hide your profile when you want to pause matching")
            SettingToggle(
                icon = if (settings.profileActive) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                title = if (settings.profileActive) "Profile status: Active" else "Profile status: Inactive",
                description = if (settings.profileActive) "Your profile can appear in search and match recommendations" else "Your profile is paused and hidden from search and matches",
                checked = settings.profileActive,
                onCheckedChange = { vm.setProfileStatus(it) }
            )
            SettingToggle(
                icon = if (settings.profileVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                title = if (settings.profileVisible) "Everyone can see my profile" else "Hide my profile",
                description = if (settings.profileVisible) "Your profile appears in search and matches" else "Your profile is hidden from new people",
                checked = settings.profileVisible,
                onCheckedChange = { vm.setPrivacy(photoPrivate = settings.photoPrivacyEnabled, visible = it) }
            )
            SettingToggle(
                icon = Icons.Filled.Lock,
                title = if (settings.contactMasked) "Contact details are private" else "Eligible members can unlock contacts",
                description = if (settings.contactMasked) "Members will see a prompt to use chat instead of your phone or email" else "Silver and above members can spend a contact unlock to view your phone and email",
                checked = settings.contactMasked,
                onCheckedChange = { vm.setContactPrivacy(it) }
            )
            SettingToggle(
                icon = Icons.Filled.Lock,
                title = "Who can send interest?",
                description = "Allow only people close to your partner preferences",
                checked = settings.contactFilterEnabled,
                onCheckedChange = { vm.setContactFilters(it) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StateTile(
                    label = "Photo privacy",
                    value = if (settings.photoPrivacyEnabled) "Approval" else "Everyone",
                    modifier = Modifier.weight(1f),
                    success = settings.photoPrivacyEnabled
                )
                StateTile(
                    label = "Search visibility",
                    value = if (settings.profileVisible) "Visible" else "Hidden",
                    modifier = Modifier.weight(1f),
                    success = settings.profileVisible
                )
            }
        }
    }
}

@Composable
private fun ManagementListsCard(
    hiddenMembers: List<PrivacyMemberUi>,
    blockedMembers: List<PrivacyMemberUi>,
    reportedConcerns: List<ReportedConcernUi>,
    onShowAgain: (String) -> Unit,
    onUnblock: (String) -> Unit,
    onEditConcern: (ReportedConcernUi) -> Unit,
    onDeleteConcern: (String) -> Unit
) {
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp), containerColor = MaterialTheme.colorScheme.surface) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionTitle("Hidden members", "These members will not appear in your matches.")
            if (hiddenMembers.isEmpty()) {
                EmptyPrivacyRow("No hidden members")
            } else {
                hiddenMembers.forEach { member ->
                    PrivacyMemberRow(
                        icon = Icons.Filled.VisibilityOff,
                        member = member,
                        actionLabel = "Show Again",
                        tone = InfoSoft,
                        content = Info,
                        onAction = { onShowAgain(member.profileId) }
                    )
                }
            }
            SectionTitle("Blocked members", "Blocked members cannot contact you.")
            if (blockedMembers.isEmpty()) {
                EmptyPrivacyRow("No blocked members")
            } else {
                blockedMembers.forEach { member ->
                    PrivacyMemberRow(
                        icon = Icons.Filled.Block,
                        member = member,
                        actionLabel = "Unblock",
                        tone = ErrorSoft,
                        content = Error,
                        onAction = { onUnblock(member.profileId) }
                    )
                }
            }
            SectionTitle("Reported concerns", "Concerns you saved can be changed or deleted.")
            if (reportedConcerns.isEmpty()) {
                EmptyPrivacyRow("No reported concerns")
            } else {
                reportedConcerns.forEach { concern ->
                    ReportedConcernRow(
                        concern = concern,
                        onEdit = { onEditConcern(concern) },
                        onDelete = { onDeleteConcern(concern.profileId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DataRightsCard(onExport: () -> Unit, onDelete: () -> Unit) {
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp), containerColor = MaterialTheme.colorScheme.surface) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Data rights", "Export your account data or request deletion")
            Text(
                "SoulMatch keeps a consent log for privacy, KYC, photo sharing, and assistance choices. Account deletion anonymizes personal details and disables login.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Export my data")
            }
            OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = Error)
                Text("Delete my account", color = Error)
            }
        }
    }
}

@Composable
private fun SupportCard(onLogout: () -> Unit) {
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp), containerColor = SurfaceSoft) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Account", "Sign out from this phone")
            Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Text("Log out")
            }
        }
    }
}

@Composable
private fun SettingToggle(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (checked) SuccessSoft else SurfaceSoft,
            modifier = Modifier.size(48.dp),
            border = BorderStroke(1.dp, Divider)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = if (checked) Success else TextSecondary)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StateTile(label: String, value: String, modifier: Modifier = Modifier, success: Boolean) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (success) SuccessSoft else SurfaceSoft,
        border = BorderStroke(1.dp, Divider)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(value, style = MaterialTheme.typography.titleSmall, color = if (success) Success else PrimaryDark, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PrivacyMemberRow(
    icon: ImageVector,
    member: PrivacyMemberUi,
    actionLabel: String,
    tone: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    onAction: () -> Unit
) {
    Surface(shape = RoundedCornerShape(8.dp), color = tone, border = BorderStroke(1.dp, Divider)) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = content)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(member.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(member.detail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            OutlinedButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun EmptyPrivacyRow(message: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = SurfaceSoft, border = BorderStroke(1.dp, Divider)) {
        Text(
            message,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun ReportedConcernRow(
    concern: ReportedConcernUi,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(shape = RoundedCornerShape(8.dp), color = SurfaceWarm, border = BorderStroke(1.dp, Divider)) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Report, contentDescription = null, tint = PrimaryDark)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(concern.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(concern.detail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            Text(concern.concern, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Edit")
                }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Delete")
                }
            }
        }
    }
}

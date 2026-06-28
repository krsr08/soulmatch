package com.soulmatch.app.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.ui.design.SoulMatchIconButton
import com.soulmatch.app.ui.design.SoulMatchPrimaryButton
import com.soulmatch.app.ui.design.SoulMatchSecondaryButton
import com.soulmatch.app.ui.design.SoulMatchTokens
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.Error
import com.soulmatch.app.ui.theme.ErrorSoft
import com.soulmatch.app.ui.theme.Success
import com.soulmatch.app.ui.theme.SuccessSoft
import com.soulmatch.app.ui.theme.SurfaceSoft
import com.soulmatch.app.ui.theme.TextSecondary
import com.soulmatch.app.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.delay

@Composable
fun PrivacySettingsScreen(
    onBack: () -> Unit,
    onOpenNotifications: () -> Unit = {},
    vm: SettingsViewModel = hiltViewModel()
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()

    LaunchedEffect(status) {
        if (status != null) {
            delay(1800)
            vm.clearStatus()
        }
    }

    SettingsDetailScaffold(title = "Privacy", onBack = onBack, onAction = onOpenNotifications) {
        Text(
            "Privacy settings",
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
            color = SoulMatchTokens.Text,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Choose what families and matches can see.",
            style = MaterialTheme.typography.bodySmall,
            color = SoulMatchTokens.Muted
        )
        status?.let { StatusPill(it) }
        PrivacyToggleRow(
            icon = Icons.Filled.Phone,
            title = "Show phone number",
            detail = "Visible only after contact unlock",
            checked = !settings.contactMasked,
            onCheckedChange = { vm.setContactPrivacy(!it) }
        )
        PrivacyToggleRow(
            icon = Icons.Filled.Email,
            title = "Show email",
            detail = "Keep email private by default",
            checked = !settings.contactMasked,
            onCheckedChange = { vm.setContactPrivacy(!it) }
        )
        SelectRow(Icons.Filled.Image, "Photo visibility", if (settings.photoPrivacyEnabled) "Request approval" else "Everyone") {
            vm.setPrivacy(photoPrivate = !settings.photoPrivacyEnabled, visible = settings.profileVisible)
        }
        SelectRow(Icons.Filled.Visibility, "Profile visibility", if (settings.profileVisible) "Visible to matches" else "Hidden from discovery") {
            vm.setPrivacy(photoPrivate = settings.photoPrivacyEnabled, visible = !settings.profileVisible)
        }
        SelectRow(Icons.Filled.Security, "Interest access", if (settings.contactFilterEnabled) "Preference matched" else "All verified members") {
            vm.setContactFilters(!settings.contactFilterEnabled)
        }
    }
}

@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()

    LaunchedEffect(status) {
        if (status != null) {
            delay(1800)
            vm.clearStatus()
        }
    }

    SettingsDetailScaffold(title = "Notifications", onBack = onBack) {
        Text(
            "Notification settings",
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
            color = SoulMatchTokens.Text,
            fontWeight = FontWeight.Bold
        )
        Text("Control alerts from SoulMatch.", style = MaterialTheme.typography.bodySmall, color = SoulMatchTokens.Muted)
        status?.let { StatusPill(it) }
        NotificationToggle("Match notifications", "New compatible profile suggestions", settings.pushEnabled) {
            vm.setPushNotifications(it)
        }
        NotificationToggle("Interest notifications", "Received, accepted, declined interests", settings.pushEnabled) {
            vm.setPushNotifications(it)
        }
        NotificationToggle("Message notifications", "New chat messages from accepted matches", settings.pushEnabled) {
            vm.setPushNotifications(it)
        }
        NotificationToggle("Payment reminders", "Plan expiry and subscription status", settings.pushEnabled) {
            vm.setPushNotifications(it)
        }
        NotificationToggle("Safety alerts", "Important account and safety updates", true) {}
    }
}

@Composable
fun PaymentScreen(
    onBack: () -> Unit,
    onPayNow: () -> Unit
) {
    SettingsDetailScaffold(title = "Payment", onBack = onBack) {
        Text(
            "Complete payment",
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
            color = SoulMatchTokens.Text,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Review your selected plan and choose a payment method.",
            style = MaterialTheme.typography.bodySmall,
            color = SoulMatchTokens.Muted
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(1.dp, SoulMatchTokens.Border)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Gold Plan", style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif), color = SoulMatchTokens.Tangerine, fontWeight = FontWeight.Bold)
                Text("1 month membership", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("INR 399", style = MaterialTheme.typography.headlineSmall, color = SoulMatchTokens.Text, fontWeight = FontWeight.Bold)
                Text("Includes contact sharing, Engage+, 50 contact details, Super Interests, and one spotlight.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        PaymentMethodRow("UPI", "Pay with any UPI app", selected = true)
        PaymentMethodRow("Cards", "Credit and debit cards", selected = false)
        PaymentMethodRow("Net banking", "All major Indian banks", selected = false)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = SuccessSoft,
            border = BorderStroke(1.dp, Color(0xFFBFE8D0))
        ) {
            Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Security, contentDescription = null, tint = Success, modifier = Modifier.size(18.dp))
                Text("Secure encrypted payment. No card details stored.", style = MaterialTheme.typography.bodySmall, color = Success)
            }
        }
        SoulMatchPrimaryButton("Pay now", onClick = onPayNow)
    }
}

@Composable
fun DeleteAccountScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    val status by vm.status.collectAsStateWithLifecycle()
    var selectedReason by remember { mutableStateOf("Found a match") }
    val reasons = listOf("Found a match", "Privacy concern", "Too many notifications", "Not getting relevant matches")

    SettingsDetailScaffold(title = "Delete Account", onBack = onBack) {
        Text(
            "Delete account",
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
            color = SoulMatchTokens.Text,
            fontWeight = FontWeight.Bold
        )
        Text(
            "This permanently removes your profile, matches, interests, and messages.",
            style = MaterialTheme.typography.bodySmall,
            color = SoulMatchTokens.Muted
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = ErrorSoft,
            border = BorderStroke(1.dp, Color(0xFFF1B7B0))
        ) {
            Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Report, contentDescription = null, tint = Error, modifier = Modifier.size(26.dp))
                Text(
                    "This action cannot be undone. Active subscription benefits will stop after account deletion.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8A1F17)
                )
            }
        }
        status?.let { StatusPill(it, success = false) }
        reasons.forEach { reason ->
            ReasonRow(reason = reason, selected = selectedReason == reason, onClick = { selectedReason = reason })
        }
        SoulMatchPrimaryButton("Delete account", onClick = { vm.deleteAccount(onDeleted) })
        SoulMatchSecondaryButton("Cancel", onClick = onBack)
    }
}

@Composable
fun LogoutConfirmationScreen(
    onCancel: () -> Unit,
    onLoggedOut: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x337A1833))
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = BorderStroke(1.dp, SoulMatchTokens.Border)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    modifier = Modifier.size(82.dp),
                    shape = CircleShape,
                    color = SoulMatchTokens.Ivory,
                    border = BorderStroke(1.dp, SoulMatchTokens.Gold)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Logout, contentDescription = null, tint = SoulMatchTokens.Tangerine, modifier = Modifier.size(34.dp))
                    }
                }
                Text(
                    "Logout from SoulMatch?",
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
                    color = SoulMatchTokens.Text,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    "You can sign back in anytime with your registered mobile number or email.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoulMatchTokens.Muted,
                    textAlign = TextAlign.Center
                )
                SoulMatchPrimaryButton("Confirm logout", onClick = { vm.logout(onLoggedOut) })
                SoulMatchSecondaryButton("Cancel", onClick = onCancel)
            }
        }
    }
}

@Composable
private fun SettingsDetailScaffold(
    title: String,
    onBack: () -> Unit,
    onAction: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(PaddingValues(horizontal = SoulMatchTokens.ScreenPadding))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SoulMatchIconButton(Icons.Filled.ArrowBack, "Back", onBack)
            Text(
                title,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall,
                color = SoulMatchTokens.Text,
                fontWeight = FontWeight.Bold
            )
            SoulMatchIconButton(Icons.Filled.Notifications, "Notifications", onAction ?: {})
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun PrivacyToggleRow(
    icon: ImageVector,
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ToggleRow(icon = icon, title = title, detail = detail, checked = checked, onCheckedChange = onCheckedChange)
}

@Composable
private fun NotificationToggle(
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ToggleRow(
        icon = Icons.Filled.Notifications,
        title = title,
        detail = detail,
        checked = checked,
        onCheckedChange = onCheckedChange
    )
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SoulMatchTokens.Border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = SoulMatchTokens.Tangerine, modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodySmall, color = SoulMatchTokens.Text, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.labelSmall, color = SoulMatchTokens.Muted)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun SelectRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SoulMatchTokens.Border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = SoulMatchTokens.Gold, modifier = Modifier.size(18.dp))
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = SoulMatchTokens.Text, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.labelSmall, color = SoulMatchTokens.Muted)
        }
    }
}

@Composable
private fun PaymentMethodRow(title: String, detail: String, selected: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (selected) SoulMatchTokens.Tangerine else SoulMatchTokens.Border)
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Payment, contentDescription = null, tint = if (selected) SoulMatchTokens.Tangerine else TextSecondary, modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Icon(Icons.Filled.Circle, contentDescription = null, tint = if (selected) SoulMatchTokens.Tangerine else Divider, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun ReasonRow(reason: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SoulMatchTokens.Border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Text(reason, style = MaterialTheme.typography.bodySmall, color = SoulMatchTokens.Text)
        }
    }
}

@Composable
private fun StatusPill(message: String, success: Boolean = true) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (success) SuccessSoft else ErrorSoft,
        border = BorderStroke(1.dp, if (success) Color(0xFFBFE8D0) else Color(0xFFF1B7B0))
    ) {
        Text(
            message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (success) Success else Error,
            fontWeight = FontWeight.SemiBold
        )
    }
}

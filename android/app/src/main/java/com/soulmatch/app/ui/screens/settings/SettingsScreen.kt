package com.soulmatch.app.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.ui.design.SoulMatchIconButton
import com.soulmatch.app.ui.design.SoulMatchTokens
import com.soulmatch.app.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.delay

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
    val status by vm.status.collectAsStateWithLifecycle()

    LaunchedEffect(status) {
        if (status != null) {
            delay(1800)
            vm.clearStatus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsTopBar(onBack = onBack, onOpenNotifications = onOpenNotifications)
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
            color = SoulMatchTokens.Text,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Manage account, privacy, notifications, and support.",
            style = MaterialTheme.typography.bodySmall,
            color = SoulMatchTokens.Muted
        )
        status?.let { SettingsStatus(text = it) }
        SettingsRow(
            icon = Icons.Filled.Person,
            title = "Account settings",
            detail = "Mobile, email, password",
            onClick = { onEditSection?.invoke(0) ?: onViewProfile?.invoke(profileId) }
        )
        SettingsRow(
            icon = Icons.Filled.PrivacyTip,
            title = "Privacy settings",
            detail = "Contact and profile visibility",
            onClick = onOpenPrivacy
        )
        SettingsRow(
            icon = Icons.Filled.Notifications,
            title = "Notification settings",
            detail = "Matches, interests, messages",
            onClick = onOpenNotifications
        )
        SettingsRow(
            icon = Icons.Filled.Block,
            title = "Blocked users",
            detail = "Manage blocked profiles",
            onClick = onOpenPrivacy
        )
        SettingsRow(
            icon = Icons.Filled.Report,
            title = "Help and support",
            detail = "FAQ and tickets",
            onClick = { onOpenChat?.invoke(chatId, participantName) }
        )
        SettingsRow(
            icon = Icons.Filled.Report,
            title = "Terms and conditions",
            detail = "SoulMatch usage terms",
            onClick = null
        )
        SettingsRow(
            icon = Icons.Filled.Lock,
            title = "Privacy policy",
            detail = "How your data is protected",
            onClick = null
        )
        SettingsRow(
            icon = Icons.Filled.Delete,
            title = "Delete account",
            detail = "Permanently remove profile",
            onClick = onOpenDeleteAccount,
            danger = true
        )
        SettingsRow(
            icon = Icons.Filled.Logout,
            title = "Logout",
            detail = "Sign out from this device",
            onClick = { onOpenLogout?.invoke() ?: vm.logout(onLogout ?: {}) },
            danger = true
        )
        Box(Modifier.height(12.dp))
    }
}

@Composable
private fun SettingsTopBar(
    onBack: () -> Unit,
    onOpenNotifications: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SoulMatchIconButton(
            icon = Icons.Filled.ArrowBack,
            contentDescription = "Back",
            onClick = onBack
        )
        Text(
            text = "Settings",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = SoulMatchTokens.Text,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        SoulMatchIconButton(
            icon = Icons.Filled.Notifications,
            contentDescription = "Notifications",
            onClick = { onOpenNotifications?.invoke() }
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    detail: String,
    onClick: (() -> Unit)?,
    danger: Boolean = false
) {
    val accent = if (danger) Color(0xFFD93025) else SoulMatchTokens.Gold
    val border = if (danger) Color(0xFFF1B7B0) else SoulMatchTokens.Border
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (danger) accent else SoulMatchTokens.Text,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = SoulMatchTokens.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = SoulMatchTokens.Muted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SettingsStatus(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SoulMatchTokens.TangerineSoft,
        border = BorderStroke(1.dp, SoulMatchTokens.GoldSoft)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = SoulMatchTokens.Tangerine,
            fontWeight = FontWeight.Bold
        )
    }
}

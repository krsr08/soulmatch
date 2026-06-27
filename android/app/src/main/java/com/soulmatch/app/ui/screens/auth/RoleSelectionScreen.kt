package com.soulmatch.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.ui.design.SoulMatchTokens
import com.soulmatch.app.ui.viewmodels.AuthUiState
import com.soulmatch.app.ui.viewmodels.AuthViewModel

@Composable
fun RoleSelectionScreen(
    onResolved: (String) -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state is AuthUiState.Verified) {
            onResolved((state as AuthUiState.Verified).route)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SoulMatchTokens.Bg)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shape = RoundedCornerShape(SoulMatchTokens.CardRadius),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, SoulMatchTokens.Border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 6.dp)
                        .background(SoulMatchTokens.Border, RoundedCornerShape(999.dp))
                )
                Text(
                    text = "Choose How You Want To Use SoulMatch",
                    color = SoulMatchTokens.Text,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    lineHeight = 30.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Select a path to continue. You can look for a partner yourself or manage profiles for others.",
                    color = SoulMatchTokens.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                RoleCard(
                    title = "User",
                    subtitle = "Looking for a life partner",
                    cta = "Continue as User",
                    icon = Icons.Outlined.FavoriteBorder,
                    enabled = state !is AuthUiState.Loading,
                    onClick = { vm.continueAsMember() }
                )
                RoleCard(
                    title = "Agent",
                    subtitle = "Create and manage profiles for others",
                    cta = "Continue as Agent",
                    icon = Icons.Outlined.WorkOutline,
                    enabled = state !is AuthUiState.Loading,
                    onClick = { vm.selectUserType("agent") }
                )
                if (state is AuthUiState.Loading) {
                    CircularProgressIndicator(
                        color = SoulMatchTokens.Tangerine,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (state is AuthUiState.Error) {
                    Text(
                        text = (state as AuthUiState.Error).message,
                        color = SoulMatchTokens.Error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    subtitle: String,
    cta: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(146.dp)
            .border(1.dp, SoulMatchTokens.Border, RoundedCornerShape(SoulMatchTokens.CardRadius))
            .clickable(enabled = enabled, onClick = onClick),
        color = SoulMatchTokens.Card,
        shape = RoundedCornerShape(SoulMatchTokens.CardRadius)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(SoulMatchTokens.TangerineSoft, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SoulMatchTokens.Tangerine,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    fontFamily = FontFamily.Serif,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SoulMatchTokens.Text
                )
                Text(
                    text = subtitle,
                    color = SoulMatchTokens.Muted,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = cta,
                    color = SoulMatchTokens.Tangerine,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(SoulMatchTokens.Tangerine, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

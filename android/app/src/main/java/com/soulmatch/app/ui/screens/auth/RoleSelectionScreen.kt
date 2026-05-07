package com.soulmatch.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
            .background(Color(0xFFFFF7F8))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFFFF5F7),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 7.dp)
                        .background(Color(0xFFE5DEDF), RoundedCornerShape(999.dp))
                )
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFFFF5D9), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u2606",
                        color = Color(0xFF9B7A11),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "How will you use\nSoulMatch?",
                    color = Color(0xFF23181B),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    lineHeight = 32.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Select your role to tailor your experience on our matrimony platform.",
                    color = Color(0xFF73535A),
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                RoleCard(
                    title = "User",
                    subtitle = "Looking for a life partner",
                    icon = Icons.Outlined.FavoriteBorder,
                    enabled = state !is AuthUiState.Loading,
                    onClick = { vm.continueAsMember() }
                )
                RoleCard(
                    title = "Agent",
                    subtitle = "Managing profiles for others",
                    icon = Icons.Outlined.WorkOutline,
                    enabled = state !is AuthUiState.Loading,
                    onClick = { vm.selectUserType("agent") }
                )
                if (state is AuthUiState.Loading) {
                    CircularProgressIndicator(
                        color = Color(0xFF8E1235),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (state is AuthUiState.Error) {
                    Text(
                        text = (state as AuthUiState.Error).message,
                        color = Color(0xFFD72964),
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
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color(0xFFE6C8D0), RoundedCornerShape(24.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 22.dp, horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color(0xFFFFF7F8), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF7A5B61),
                    modifier = Modifier.size(30.dp)
                )
            }
            Text(
                text = title,
                fontFamily = FontFamily.Serif,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1A19)
            )
            Text(
                text = subtitle,
                color = Color(0xFF5C4444),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

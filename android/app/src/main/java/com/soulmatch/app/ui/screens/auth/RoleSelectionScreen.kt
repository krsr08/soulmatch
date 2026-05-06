package com.soulmatch.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.ui.viewmodels.AuthUiState
import com.soulmatch.app.ui.viewmodels.AuthViewModel

private enum class RoleChoice { User, Agent }

@Composable
fun RoleSelectionScreen(
    onResolved: (String) -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var selectedRole by remember { mutableStateOf(RoleChoice.User) }

    LaunchedEffect(state) {
        if (state is AuthUiState.Verified) {
            onResolved((state as AuthUiState.Verified).route)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5EEEB))
            .padding(horizontal = 18.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shape = RoundedCornerShape(34.dp),
            tonalElevation = 8.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 58.dp, height = 8.dp)
                        .background(Color(0xFFE5E0DD), RoundedCornerShape(999.dp))
                )
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(0xFFFFF8E7), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✩", color = Color(0xFF907000), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "How will you use\nSoulMatch?",
                    color = Color(0xFF1C1A1A),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    lineHeight = 38.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Select your role to tailor your experience on our premium matrimony platform.",
                    color = Color(0xFF6C4F52),
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 34.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                RoleCard(
                    selected = selectedRole == RoleChoice.User,
                    title = "User",
                    subtitle = "Looking for a life partner",
                    icon = Icons.Outlined.FavoriteBorder,
                    onClick = { selectedRole = RoleChoice.User }
                )
                RoleCard(
                    selected = selectedRole == RoleChoice.Agent,
                    title = "Agent",
                    subtitle = "Managing profiles for others",
                    icon = Icons.Outlined.WorkOutline,
                    onClick = { selectedRole = RoleChoice.Agent }
                )
                if (state is AuthUiState.Error) {
                    Text(
                        text = (state as AuthUiState.Error).message,
                        color = Color(0xFFD72964),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
                Button(
                    onClick = {
                        when (selectedRole) {
                            RoleChoice.User -> vm.continueAsMember()
                            RoleChoice.Agent -> vm.selectUserType("agent")
                        }
                    },
                    enabled = state !is AuthUiState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7B001C),
                        contentColor = Color.White
                    )
                ) {
                    if (state is AuthUiState.Loading) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Continue", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleCard(
    selected: Boolean,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val borderColor = if (selected) Color(0xFF6B001C) else Color(0xFFE2DEDB)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(30.dp))
            .clickable(onClick = onClick),
        color = if (selected) Color(0xFFFFF8F7) else Color.White,
        shape = RoundedCornerShape(30.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 30.dp, horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(Color(0xFFF7F4F2), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF6C4F52), modifier = Modifier.size(34.dp))
            }
            Text(
                text = title,
                fontFamily = FontFamily.Serif,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1A19)
            )
            Text(
                text = subtitle,
                color = Color(0xFF5C4444),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

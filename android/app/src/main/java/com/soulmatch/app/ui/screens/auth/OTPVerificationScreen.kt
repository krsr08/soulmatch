package com.soulmatch.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.ui.design.SoulMatchTokens
import com.soulmatch.app.ui.screens.design.DesignHotspot
import com.soulmatch.app.ui.screens.design.ExactDesignScreen
import com.soulmatch.app.ui.screens.design.backHotspot
import com.soulmatch.app.ui.viewmodels.AuthUiState
import com.soulmatch.app.ui.viewmodels.AuthViewModel

@Composable
fun OTPVerificationScreen(
    phone: String,
    userType: String? = null,
    onVerified: (String) -> Unit,
    onBack: () -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var otp by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        when (state) {
            is AuthUiState.Verified -> onVerified((state as AuthUiState.Verified).route)
            is AuthUiState.Error -> otp = ""
            else -> Unit
        }
    }

    ExactDesignScreen(
        assetName = "05_otp_verification_screen.png",
        hotspots = listOf(
            backHotspot(onBack),
            DesignHotspot(28f, 754f, 334f, 58f) {
                if (otp.length == 6) {
                    vm.verifyOTP(phone, otp, userType)
                } else {
                    vm.reportError("Enter the 6 digit OTP.")
                }
            },
            DesignHotspot(118f, 420f, 154f, 42f) { vm.sendOTP(phone, userType) }
        ),
        overlay = {
            OtpInputOverlay(
                otp = otp,
                onOtpChange = {
                    otp = it.filter(Char::isDigit).take(6)
                    if (state is AuthUiState.Error) vm.clearError()
                }
            )
        }
    )
}

@Composable
private fun BoxWithConstraintsScope.OtpInputOverlay(
    otp: String,
    onOtpChange: (String) -> Unit
) {
    BasicTextField(
        value = otp,
        onValueChange = onOtpChange,
        modifier = Modifier
            .offset(x = (maxWidth.value * 30f / 390f).dp, y = (maxHeight.value * 314f / 844f).dp)
            .size(width = (maxWidth.value * 330f / 390f).dp, height = (maxHeight.value * 66f / 844f).dp)
            .background(Color.White.copy(alpha = 0.02f)),
        textStyle = TextStyle(
            color = SoulMatchTokens.Text,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            letterSpacing = 28.sp
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true
    )
}

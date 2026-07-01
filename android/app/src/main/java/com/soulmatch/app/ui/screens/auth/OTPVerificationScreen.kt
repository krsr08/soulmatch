package com.soulmatch.app.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.R
import com.soulmatch.app.ui.design.SoulMatchHeaderIconButton
import com.soulmatch.app.ui.design.SoulMatchPrimaryButton
import com.soulmatch.app.ui.design.SoulMatchTokens
import com.soulmatch.app.ui.viewmodels.AuthUiState
import com.soulmatch.app.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.delay

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun OTPVerificationScreen(
    phone: String,
    userType: String? = null,
    onVerified: (String) -> Unit,
    onBack: () -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    val boxes = remember { mutableStateListOf("", "", "", "", "", "") }
    val focusers = remember { List(6) { FocusRequester() } }
    val state by vm.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var countdown by remember { mutableIntStateOf(30) }
    var canResend by remember { mutableStateOf(false) }
    var resendCycle by remember { mutableIntStateOf(0) }
    val errorMessage = (state as? AuthUiState.Error)?.message.orEmpty()
    val otpValue = boxes.joinToString("")

    LaunchedEffect(phone, resendCycle) {
        countdown = 30
        canResend = false
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        canResend = true
    }

    LaunchedEffect(state) {
        when (state) {
            is AuthUiState.Verified -> onVerified((state as AuthUiState.Verified).route)
            is AuthUiState.Error -> {
                val targetIndex = boxes.indexOfFirst { it.isEmpty() }.let { if (it == -1) 5 else it }
                runCatching { focusers[targetIndex].requestFocus() }
            }
            else -> Unit
        }
    }

    LaunchedEffect(Unit) {
        delay(200)
        focusers[0].requestFocus()
    }

    fun verifyOtp() {
        if (otpValue.length != 6 || state is AuthUiState.Loading) return
        closeOtpKeyboard(focusManager, keyboardController)
        runCatching {
            vm.verifyOTP(phone, otpValue, userType)
        }.onFailure {
            vm.reportError("Wrong / Expired OTP. Please check and enter again.")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SoulMatchTokens.Bg)
            .statusBarsPadding()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        DecorativeStar(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 124.dp, end = 18.dp)
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SoulMatchHeaderIconButton(
                    icon = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack
                )
            }
            Spacer(Modifier.height(56.dp))
            SoftIcon {
                Image(
                    painter = painterResource(R.drawable.login_route_icon),
                    contentDescription = null,
                    modifier = Modifier.size(38.dp)
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                text = "Verify your number",
                color = SoulMatchTokens.Text,
                fontFamily = FontFamily.Serif,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Enter the 6-digit OTP sent to $phone",
                modifier = Modifier.padding(top = 18.dp),
                color = SoulMatchTokens.Muted,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(34.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                boxes.forEachIndexed { index, value ->
                    val borderColor = when {
                        errorMessage.isNotBlank() -> SoulMatchTokens.Error
                        value.isNotBlank() -> SoulMatchTokens.Tangerine
                        else -> SoulMatchTokens.Border
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = { nextValue ->
                            if (nextValue.length <= 1 && nextValue.all(Char::isDigit)) {
                                boxes[index] = nextValue
                                if (errorMessage.isNotBlank()) vm.clearError()
                                if (nextValue.isNotEmpty() && index < 5) {
                                    focusers[index + 1].requestFocus()
                                }
                                if (boxes.joinToString("").length == 6) {
                                    closeOtpKeyboard(focusManager, keyboardController)
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(76.dp)
                            .border(2.dp, borderColor, RoundedCornerShape(SoulMatchTokens.CardRadius))
                            .focusRequester(focusers[index]),
                        textStyle = TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = SoulMatchTokens.Text
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        decorationBox = { inner ->
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                inner()
                            }
                        }
                    )
                }
            }
            if (errorMessage.isNotBlank()) {
                Text(
                    text = otpErrorMessage(errorMessage),
                    color = SoulMatchTokens.Error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 18.dp),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(28.dp))
            if (canResend) {
                TextButton(
                    onClick = {
                        vm.clearError()
                        vm.sendOTP(phone, userType)
                        resendCycle++
                    }
                ) {
                    Text("Resend OTP", color = SoulMatchTokens.Tangerine, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = "Resend OTP in 00:${countdown.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoulMatchTokens.Muted,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.weight(1f))
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state is AuthUiState.Loading) {
                CircularProgressIndicator(
                    color = SoulMatchTokens.Tangerine,
                    modifier = Modifier
                        .padding(bottom = 14.dp)
                        .size(22.dp),
                    strokeWidth = 2.dp
                )
            }
            SoulMatchPrimaryButton(
                text = "Verify OTP",
                enabled = otpValue.length == 6 && state !is AuthUiState.Loading,
                onClick = { verifyOtp() },
                modifier = Modifier.height(64.dp)
            )
        }
    }
}

private fun otpErrorMessage(raw: String): String {
    val lower = raw.lowercase()
    return if (
        lower.contains("invalid otp") ||
        lower.contains("expired otp") ||
        lower.contains("wrong / expired otp") ||
        lower.contains("invalid code") ||
        lower.contains("code expired")
    ) {
        "Wrong / Expired OTP. Please check and enter again."
    } else {
        raw
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun closeOtpKeyboard(
    focusManager: FocusManager,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?
) {
    focusManager.clearFocus(force = true)
    keyboardController?.hide()
}

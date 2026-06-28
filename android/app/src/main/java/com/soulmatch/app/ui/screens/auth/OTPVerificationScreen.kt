package com.soulmatch.app.ui.screens.auth

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.ui.design.SoulMatchTokens
import com.soulmatch.app.ui.viewmodels.AuthUiState
import com.soulmatch.app.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.delay

@Composable
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
    var countdown by remember { mutableIntStateOf(30) }
    var canResend by remember { mutableStateOf(false) }
    var resendCycle by remember { mutableIntStateOf(0) }
    val errorMessage = (state as? AuthUiState.Error)?.message.orEmpty()

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
                boxes.forEachIndexed { index, _ -> boxes[index] = "" }
                delay(80)
                runCatching { focusers[0].requestFocus() }
            }
            else -> Unit
        }
    }

    LaunchedEffect(Unit) {
        delay(200)
        focusers[0].requestFocus()
    }

    Scaffold(
        topBar = {
            AuthPageHeader(
                title = "OTP Verification",
                onBack = onBack,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Code sent to $phone", style = MaterialTheme.typography.bodyMedium, color = SoulMatchTokens.Muted)
            Spacer(Modifier.size(34.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                boxes.forEachIndexed { index, value ->
                    val borderColor = when {
                        state is AuthUiState.Error -> SoulMatchTokens.Error
                        value.isNotEmpty() -> SoulMatchTokens.Tangerine
                        else -> SoulMatchTokens.Border
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = { nextValue ->
                            if (nextValue.length <= 1 && nextValue.all(Char::isDigit)) {
                                boxes[index] = nextValue
                                if (nextValue.isNotEmpty() && index < 5) focusers[index + 1].requestFocus()
                                if (boxes.joinToString("").length == 6) {
                                    runCatching {
                                        vm.verifyOTP(phone, boxes.joinToString(""), userType)
                                    }.onFailure {
                                        vm.reportError("Invalid OTP")
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .border(2.dp, borderColor, RoundedCornerShape(SoulMatchTokens.CardRadius))
                            .focusRequester(focusers[index]),
                        textStyle = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = SoulMatchTokens.Text
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        decorationBox = { inner ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                inner()
                            }
                        }
                    )
                }
            }
            if (state is AuthUiState.Error) {
                Text(
                    if (errorMessage.equals("Invalid OTP", ignoreCase = true)) {
                        "Invalid OTP. Please enter a valid OTP"
                    } else {
                        errorMessage
                    },
                    color = SoulMatchTokens.Error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Spacer(Modifier.size(32.dp))
            if (canResend) {
                TextButton(onClick = {
                    vm.sendOTP(phone, userType)
                    resendCycle++
                }) {
                    Text("Resend OTP", color = SoulMatchTokens.Tangerine)
                }
            } else {
                Text("Resend in 00:${countdown.toString().padStart(2, '0')}", style = MaterialTheme.typography.bodySmall, color = SoulMatchTokens.Muted)
            }
            Spacer(Modifier.size(16.dp))
            if (state is AuthUiState.Loading) {
                CircularProgressIndicator(color = SoulMatchTokens.Tangerine)
            }
        }
    }
}

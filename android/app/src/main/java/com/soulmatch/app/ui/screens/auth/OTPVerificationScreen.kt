package com.soulmatch.app.ui.screens.auth
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.soulmatch.app.ui.theme.Primary
import com.soulmatch.app.ui.viewmodels.AuthViewModel
import com.soulmatch.app.ui.viewmodels.AuthUiState
import kotlinx.coroutines.delay
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OTPVerificationScreen(phone: String, userType: String? = null, onVerified: (String) -> Unit, onBack: () -> Unit, vm: AuthViewModel = hiltViewModel()) {
    val boxes = remember { mutableStateListOf("","","","","","") }
    val focusers = remember { List(6) { FocusRequester() } }
    val state by vm.uiState.collectAsStateWithLifecycle()
    var countdown by remember { mutableIntStateOf(30) }
    var canResend by remember { mutableStateOf(false) }
    var resendCycle by remember { mutableIntStateOf(0) }
    LaunchedEffect(resendCycle) { while (countdown > 0) { delay(1000); countdown-- }; canResend = true }
    LaunchedEffect(state) { when (state) { is AuthUiState.Verified -> onVerified((state as AuthUiState.Verified).route); is AuthUiState.Error -> { boxes.forEachIndexed { i, _ -> boxes[i] = "" }; focusers[0].requestFocus() }; else -> {} } }
    LaunchedEffect(Unit) { delay(200); focusers[0].requestFocus() }
    Scaffold(topBar = { TopAppBar(title = {}, navigationIcon = { IconButton(onClick=onBack) { Icon(Icons.Filled.ArrowBack,"Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal=24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Verify Your Number", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Code sent to $phone", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f))
            Spacer(Modifier.height(40.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                boxes.forEachIndexed { i, v ->
                    val borderColor = when { state is AuthUiState.Error -> MaterialTheme.colorScheme.error; v.isNotEmpty() -> Primary; else -> MaterialTheme.colorScheme.outline }
                    BasicTextField(value = v, onValueChange = { nv -> if (nv.length <= 1 && nv.all(Char::isDigit)) { boxes[i] = nv; if (nv.isNotEmpty() && i < 5) focusers[i+1].requestFocus(); if (boxes.joinToString("").length == 6) vm.verifyOTP(phone, boxes.joinToString(""), userType) } }, modifier = Modifier.size(48.dp).border(2.dp, borderColor, RoundedCornerShape(8.dp)).focusRequester(focusers[i]), textStyle = TextStyle(fontSize=20.sp, fontWeight=FontWeight.Bold, textAlign=TextAlign.Center, color=MaterialTheme.colorScheme.onSurface), keyboardOptions = KeyboardOptions(keyboardType=KeyboardType.NumberPassword), singleLine = true, decorationBox = { inner -> Box(Modifier.fillMaxSize(), contentAlignment=Alignment.Center) { inner() } })
                }
            }
            if (state is AuthUiState.Error) Text((state as AuthUiState.Error).message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top=8.dp))
            Spacer(Modifier.height(32.dp))
            if (canResend) TextButton(onClick = {
                vm.sendOTP(phone, userType)
                countdown=30
                canResend=false
                resendCycle++
            }) { Text("Resend OTP", color = Primary) }
            else Text("Resend in 00:${countdown.toString().padStart(2,'0')}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f))
            Spacer(Modifier.height(16.dp))
            if (state is AuthUiState.Loading) CircularProgressIndicator(color = Primary)
        }
    }
}

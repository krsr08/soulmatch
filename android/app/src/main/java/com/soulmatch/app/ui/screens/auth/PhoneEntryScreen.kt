package com.soulmatch.app.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.data.models.PhoneEntryContentData
import com.soulmatch.app.ui.components.PremiumCard
import com.soulmatch.app.ui.components.PremiumScreen
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.Error
import com.soulmatch.app.ui.theme.PrimaryDark
import com.soulmatch.app.ui.theme.Success
import com.soulmatch.app.ui.theme.SuccessSoft
import com.soulmatch.app.ui.theme.SurfaceSoft
import com.soulmatch.app.ui.theme.SurfaceWarm
import com.soulmatch.app.ui.theme.TextSecondary
import com.soulmatch.app.ui.viewmodels.AuthUiState
import com.soulmatch.app.ui.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneEntryScreen(
    content: PhoneEntryContentData = PhoneEntryContentData(),
    userType: String = "member",
    onOTPSent: (String) -> Unit,
    onVerified: (String) -> Unit = {},
    onBack: () -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    var phone by remember { mutableStateOf("") }
    val countryCode = "+91"
    val state by vm.uiState.collectAsStateWithLifecycle()
    val isLoading = state is AuthUiState.Loading
    val localError = phone.isNotEmpty() && phone.length < 10
    val canSubmit = phone.length == 10 && !isLoading

    LaunchedEffect(state) {
        if (state is AuthUiState.OTPSent) onOTPSent(countryCode + phone)
        if (state is AuthUiState.Verified) onVerified((state as AuthUiState.Verified).route)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(content.topBarTitle.ifBlank { "Mobile verification" }, fontWeight = FontWeight.Bold) },
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
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PremiumCard(containerColor = SurfaceWarm) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            content.title.ifBlank { "Enter your mobile number" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            content.subtitle.ifBlank { "We use this number for OTP login, account recovery, and important match alerts." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val trustLines = content.trustLines.filter { it.isNotBlank() }.ifEmpty {
                                listOf("No password needed", "Private by default")
                            }
                            TrustLine(trustLines.getOrElse(0) { "No password needed" })
                            TrustLine(trustLines.getOrElse(1) { "Private by default" })
                        }
                    }
                }

                PremiumCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = countryCode,
                                onValueChange = {},
                                label = { Text("Code") },
                                modifier = Modifier.width(84.dp),
                                readOnly = true,
                                singleLine = true
                            )
                            Spacer(Modifier.width(10.dp))
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { raw ->
                                    phone = raw.filter(Char::isDigit).take(10)
                                    vm.clearError()
                                },
                                label = { Text(content.fieldLabel.ifBlank { "10 digit mobile number" }) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                isError = localError || state is AuthUiState.Error
                            )
                        }

                        Text(
                            text = when {
                                state is AuthUiState.Error -> (state as AuthUiState.Error).message
                                localError -> "Enter all 10 digits. Only numbers are allowed."
                                phone.length == 10 -> "OTP will be sent to $countryCode $phone."
                                else -> content.helperText.ifBlank { "Use your active Indian mobile number." }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                state is AuthUiState.Error || localError -> Error
                                phone.length == 10 -> Success
                                else -> TextSecondary
                            }
                        )

                        Button(
                            onClick = {
                                vm.sendOTP(countryCode + phone, userType)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            enabled = canSubmit
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text(content.submitCta.ifBlank { "Send OTP" }, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = SurfaceSoft,
                    border = BorderStroke(1.dp, Divider)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = PrimaryDark, modifier = Modifier.size(20.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(content.privacyTitle.ifBlank { "Your number stays protected" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(content.privacyBody.ifBlank { "Members see contact details only when your privacy and plan rules allow it." }, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrustLine(label: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = SuccessSoft,
        border = BorderStroke(1.dp, Divider)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = PrimaryDark, fontWeight = FontWeight.Bold)
        }
    }
}

package com.soulmatch.app.ui.screens.auth

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.soulmatch.app.BuildConfig
import com.soulmatch.app.R
import com.soulmatch.app.data.models.AuthContentData
import com.soulmatch.app.data.models.BrandingConfig
import com.soulmatch.app.ui.design.SoulMatchTokens
import com.soulmatch.app.ui.viewmodels.AuthUiState
import com.soulmatch.app.ui.viewmodels.AuthViewModel

@Composable
fun WelcomeScreen(
    branding: BrandingConfig = BrandingConfig(),
    content: AuthContentData = AuthContentData(),
    googleWebClientId: String = "",
    onOtpSent: (String) -> Unit,
    onBackToLanguage: () -> Unit = {},
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onAuthenticated: (String) -> Unit = {},
    vm: AuthViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val webClientId = rememberGoogleWebClientId(googleWebClientId)
    var phone by remember { mutableStateOf("") }
    var localPhoneError by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val googleClient = remember(webClientId, context) {
        val optionsBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
        if (webClientId.isNotBlank()) {
            optionsBuilder.requestIdToken(webClientId)
        }
        GoogleSignIn.getClient(
            context,
            optionsBuilder.build()
        )
    }

    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val token = account.idToken
            if (token.isNullOrBlank()) {
                vm.reportError("Google sign-in did not return a valid ID token. Please check the app's Google OAuth setup.")
            } else {
                vm.googleLogin(token, "member")
            }
        } catch (error: ApiException) {
            val statusName = GoogleSignInStatusCodes.getStatusCodeString(error.statusCode)
            Log.e("WelcomeScreen", "Google sign-in failed: statusCode=${error.statusCode}, status=$statusName", error)
            vm.reportError(googleSignInErrorMessage(error.statusCode, statusName))
        }
    }

    LaunchedEffect(state) {
        when (state) {
            is AuthUiState.OTPSent -> normalizeIndianPhone(phone)?.let(onOtpSent)
            is AuthUiState.Verified -> onAuthenticated((state as AuthUiState.Verified).route)
            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SoulMatchTokens.Bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuthPageHeader(
                title = "Login or register",
                onBack = onBackToLanguage
            )
            Spacer(Modifier.height(22.dp))
            Text(
                text = "Enter your mobile number or continue with Google. We'll verify your number with OTP and then take you to the right screen.",
                modifier = Modifier.fillMaxWidth(),
                color = SoulMatchTokens.Muted,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 28.sp
            )
            Spacer(Modifier.height(28.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Mobile number", color = SoulMatchTokens.Muted, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                MobileNumberField(
                    value = phone,
                    onValueChange = {
                        phone = it.filter(Char::isDigit).take(10)
                        localPhoneError = null
                        vm.clearError()
                    }
                )
                if (localPhoneError != null) {
                    Text(
                        text = localPhoneError.orEmpty(),
                        color = SoulMatchTokens.Error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                color = SoulMatchTokens.Ivory,
                shape = RoundedCornerShape(SoulMatchTokens.CardRadius),
                border = BorderStroke(1.dp, SoulMatchTokens.Border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(Icons.Filled.Security, contentDescription = null, tint = SoulMatchTokens.Tangerine, modifier = Modifier.size(28.dp))
                    Text(
                        text = "After OTP, existing users continue to their app and new users start profile setup.",
                        color = SoulMatchTokens.Muted,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    val normalized = normalizeIndianPhone(phone.trim())
                    if (normalized == null) {
                        localPhoneError = "Please enter a valid mobile number"
                    } else {
                        localPhoneError = null
                        vm.clearError()
                        vm.sendOTP(normalized, "member")
                    }
                },
                enabled = state !is AuthUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(SoulMatchTokens.PillRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoulMatchTokens.Tangerine,
                    contentColor = Color.White
                )
            ) {
                Text("Register / Login", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            when (state) {
                is AuthUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .size(22.dp),
                    color = SoulMatchTokens.Tangerine,
                    strokeWidth = 2.dp
                )
                is AuthUiState.Error -> Text(
                    text = (state as AuthUiState.Error).message,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    color = SoulMatchTokens.Error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start
                )
                else -> Unit
            }
            ContinueWithDivider(modifier = Modifier.padding(vertical = 16.dp))
            OutlinedButton(
                onClick = {
                    vm.clearError()
                    if (webClientId.isBlank()) {
                        vm.reportError("Google sign-in is not configured yet. Add a valid Google web client ID in public config or local app config.")
                    } else {
                        googleClient.signOut().addOnCompleteListener {
                            googleLauncher.launch(googleClient.signInIntent)
                        }
                    }
                },
                enabled = state !is AuthUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(SoulMatchTokens.PillRadius),
                border = BorderStroke(1.dp, SoulMatchTokens.Border),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF2D2D2D)
                )
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_google_g),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(content.googleCta.ifBlank { "Continue with Google" }, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            LegalLinks(
                prefix = content.termsPrefix.ifBlank { "By continuing, you agree to our" },
                onOpenTerms = onOpenTerms,
                onOpenPrivacy = onOpenPrivacy
            )
        }
    }
}

@Composable
private fun MobileNumberField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SoulMatchTokens.CardRadius),
        border = BorderStroke(1.dp, SoulMatchTokens.Border),
        color = SoulMatchTokens.Ivory
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            textStyle = TextStyle(
                color = SoulMatchTokens.Text,
                fontSize = 18.sp
            ),
            decorationBox = { inner ->
                if (value.isBlank()) {
                    Text(
                        text = "Enter your mobile number",
                        color = SoulMatchTokens.Muted,
                        fontSize = 18.sp
                    )
                }
                inner()
            }
        )
    }
}

@Composable
private fun ContinueWithDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Divider(modifier = Modifier.weight(1f), color = SoulMatchTokens.Border)
        Text(
            text = "Or",
            color = SoulMatchTokens.Muted,
            fontSize = 14.sp
        )
        Divider(modifier = Modifier.weight(1f), color = SoulMatchTokens.Border)
    }
}

@Composable
private fun LegalLinks(
    prefix: String,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = SoulMatchTokens.Muted)) {
            append(prefix)
            append(" ")
        }
        pushStringAnnotation(tag = "legal", annotation = "terms")
        withStyle(SpanStyle(color = SoulMatchTokens.Tangerine, textDecoration = TextDecoration.Underline)) {
            append("Terms of Service")
        }
        pop()
        withStyle(SpanStyle(color = SoulMatchTokens.Muted)) {
            append(" and ")
        }
        pushStringAnnotation(tag = "legal", annotation = "privacy")
        withStyle(SpanStyle(color = SoulMatchTokens.Tangerine, textDecoration = TextDecoration.Underline)) {
            append("Privacy Policy")
        }
        pop()
    }
    androidx.compose.foundation.text.ClickableText(
        text = text,
        style = TextStyle(
            fontSize = 12.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center
        ),
        modifier = Modifier.fillMaxWidth(),
        onClick = { offset ->
            when (text.getStringAnnotations("legal", offset, offset).firstOrNull()?.item) {
                "terms" -> onOpenTerms()
                "privacy" -> onOpenPrivacy()
            }
        }
    )
}

private fun normalizeIndianPhone(raw: String): String? {
    val digits = raw.filter(Char::isDigit)
    val startsValid = digits.firstOrNull() in listOf('6', '7', '8', '9')
    val notRepeated = digits.toSet().size > 1
    return if (digits.length == 10 && startsValid && notRepeated) "+91$digits" else null
}

private fun googleSignInErrorMessage(statusCode: Int, statusName: String): String {
    return when (statusCode) {
        GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Google sign-in was cancelled."
        GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "Google sign-in is already in progress. Please wait."
        GoogleSignInStatusCodes.SIGN_IN_FAILED,
        CommonStatusCodes.DEVELOPER_ERROR -> "Google sign-in setup needs attention. Please verify the app SHA fingerprints and web client ID. Code: $statusCode"
        else -> "Google sign-in failed. Please try again."
    }
}

@Composable
private fun rememberGoogleWebClientId(configuredClientId: String): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    return configuredClientId.ifBlank {
        BuildConfig.GOOGLE_WEB_CLIENT_ID.ifBlank {
            val defaultWebClientId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (defaultWebClientId != 0) context.getString(defaultWebClientId) else ""
        }
    }
}

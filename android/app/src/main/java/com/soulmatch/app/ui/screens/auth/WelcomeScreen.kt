package com.soulmatch.app.ui.screens.auth

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
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
import com.soulmatch.app.data.models.AuthContentData
import com.soulmatch.app.data.models.BrandingConfig
import com.soulmatch.app.ui.design.SoulMatchTokens
import com.soulmatch.app.ui.screens.design.DesignHotspot
import com.soulmatch.app.ui.screens.design.ExactDesignScreen
import com.soulmatch.app.ui.screens.design.backHotspot
import com.soulmatch.app.ui.viewmodels.AuthUiState
import com.soulmatch.app.ui.viewmodels.AuthViewModel

@Composable
fun WelcomeScreen(
    branding: BrandingConfig = BrandingConfig(),
    content: AuthContentData = AuthContentData(),
    googleWebClientId: String = "",
    onOtpSent: (String) -> Unit,
    onBackToLanguage: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onAuthenticated: (String) -> Unit = {},
    vm: AuthViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var phone by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val webClientId = rememberGoogleWebClientId(googleWebClientId)
    val googleClient = remember(webClientId, context) {
        val optionsBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
        if (webClientId.isNotBlank()) {
            optionsBuilder.requestIdToken(webClientId)
        }
        GoogleSignIn.getClient(context, optionsBuilder.build())
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

    ExactDesignScreen(
        assetName = "03_login_screen.png",
        hotspots = listOf(
            backHotspot(onBackToLanguage),
            DesignHotspot(28f, 662f, 334f, 58f) {
                val normalized = normalizeIndianPhone(phone)
                if (normalized == null) {
                    vm.reportError("Enter a valid 10 digit mobile number.")
                } else {
                    vm.clearError()
                    vm.sendOTP(normalized, "member")
                }
            },
            DesignHotspot(28f, 764f, 334f, 58f) {
                googleLauncher.launch(googleClient.signInIntent)
            }
        ),
        overlay = {
            LoginPhoneField(phone = phone, onPhoneChange = {
                phone = it.filter(Char::isDigit).take(10)
                vm.clearError()
            })
        }
    )
}

@Composable
private fun BoxWithConstraintsScope.LoginPhoneField(
    phone: String,
    onPhoneChange: (String) -> Unit
) {
    BasicTextField(
        value = phone,
        onValueChange = onPhoneChange,
        modifier = Modifier
            .offset(x = (maxWidth.value * 64f / 390f).dp, y = (maxHeight.value * 280f / 844f).dp)
            .size(width = (maxWidth.value * 270f / 390f).dp, height = (maxHeight.value * 36f / 844f).dp)
            .background(Color.White),
        textStyle = TextStyle(
            color = SoulMatchTokens.Text,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        ),
        singleLine = true
    )
}

private fun normalizeIndianPhone(raw: String): String? {
    val digits = raw.filter(Char::isDigit)
    return if (digits.length == 10) "+91$digits" else null
}

private fun googleSignInErrorMessage(statusCode: Int, statusName: String): String {
    return when (statusCode) {
        GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Google sign-in was cancelled."
        GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "Google sign-in is already in progress. Please wait."
        GoogleSignInStatusCodes.SIGN_IN_FAILED,
        CommonStatusCodes.DEVELOPER_ERROR -> "Google sign-in setup needs attention. Please verify the app SHA fingerprints and web client ID. Code: $statusCode"
        else -> "Google sign-in failed. Code: $statusCode $statusName"
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

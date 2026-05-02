package com.soulmatch.app.ui.screens.auth

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.soulmatch.app.BuildConfig
import com.soulmatch.app.R
import com.soulmatch.app.data.models.AuthContentData
import com.soulmatch.app.data.models.BrandingConfig
import com.soulmatch.app.ui.viewmodels.AuthUiState
import com.soulmatch.app.ui.viewmodels.AuthViewModel

@Composable
@Suppress("UNUSED_PARAMETER")
fun WelcomeScreen(
    branding: BrandingConfig = BrandingConfig(),
    content: AuthContentData = AuthContentData(),
    googleWebClientId: String = "",
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onContinue: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onAuthenticated: (String) -> Unit = {},
    vm: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by vm.uiState.collectAsStateWithLifecycle()
    val webClientId = rememberGoogleWebClientId(googleWebClientId)
    val googleClient = GoogleSignIn.getClient(
        context,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .let { builder ->
                if (webClientId.isNotBlank()) builder.requestIdToken(webClientId) else builder
            }
            .build()
    )
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            vm.googleLogin(account.idToken)
        } catch (error: ApiException) {
            val statusName = GoogleSignInStatusCodes.getStatusCodeString(error.statusCode)
            Log.e("WelcomeScreen", "Google sign-in failed: statusCode=${error.statusCode}, status=$statusName", error)
            vm.reportError(googleSignInErrorMessage(error.statusCode, statusName))
        }
    }

    LaunchedEffect(state) {
        if (state is AuthUiState.Verified) {
            onAuthenticated((state as AuthUiState.Verified).route)
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(LoginCream)
    ) {
        val compact = maxHeight < 850.dp
        val heroHeight = if (compact) 392.dp else 476.dp
        val cardTop = heroHeight - if (compact) 88.dp else 104.dp
        val waveHeight = if (compact) 52.dp else 78.dp

        HeroBackdrop(
            imageUrl = branding.previewImageUrl,
            height = heroHeight
        )
        HeroIntro(
            appTitle = branding.appTitle.ifBlank { "SoulMatch" },
            tagline = branding.tagline.ifBlank { "Serious matchmaking for modern families" },
            trustLabels = content.trustChips,
            heroHeight = heroHeight,
            compact = compact
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = cardTop, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 20.dp else 34.dp)
        ) {
            AuthCard(
                state = state,
                content = content,
                compact = compact,
                onRegister = onRegister,
                onLogin = onLogin,
                onGoogle = {
                    vm.clearError()
                    if (webClientId.isBlank()) {
                        vm.reportError("Google sign-in needs a Firebase web OAuth client ID. Add it in Control Panel > Dynamic Config > Public app keys, or keep GOOGLE_WEB_CLIENT_ID in local.properties.")
                    } else {
                        googleClient.signOut().addOnCompleteListener {
                            googleLauncher.launch(googleClient.signInIntent)
                        }
                    }
                }
            )
            FooterLegal(
                modifier = Modifier.padding(horizontal = 8.dp),
                compact = compact,
                prefix = content.termsPrefix,
                onOpenTerms = onOpenTerms,
                onOpenPrivacy = onOpenPrivacy
            )
        }
        WaveBottom(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(waveHeight)
        )
    }
}

private fun googleSignInErrorMessage(statusCode: Int, statusName: String): String {
    return when (statusCode) {
        GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Google sign-in was cancelled."
        GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "Google sign-in is already in progress. Please wait."
        GoogleSignInStatusCodes.SIGN_IN_FAILED,
        CommonStatusCodes.DEVELOPER_ERROR -> "Google sign-in setup needs fixing. Check Android OAuth SHA and Web client ID. Code: $statusCode"
        else -> "Google sign-in failed. Code: $statusCode $statusName"
    }
}

@Composable
private fun HeroBackdrop(imageUrl: String, height: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp))
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(1.5.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(R.drawable.login_region_north_banarasi),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(1.5.dp),
                contentScale = ContentScale.Crop
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(LoginHeroOverlay)
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            LoginCream.copy(alpha = 0.44f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun HeroIntro(
    appTitle: String,
    tagline: String,
    trustLabels: List<String>,
    heroHeight: Dp,
    compact: Boolean
) {
    val resolvedTrust = trustLabels.filter { it.isNotBlank() }.ifEmpty {
        listOf("Verified profiles", "Private photos")
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight)
            .padding(horizontal = 26.dp)
            .padding(top = if (compact) 58.dp else 86.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            appTitle,
            color = Color.White,
            fontSize = if (compact) 26.sp else 30.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(if (compact) 12.dp else 18.dp))
        Text(
            tagline,
            color = Color.White,
            fontSize = if (compact) 24.sp else 28.sp,
            lineHeight = if (compact) 31.sp else 36.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(Modifier.height(if (compact) 54.dp else 72.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 18.dp)
        ) {
            HeroTrustPill(
                label = resolvedTrust.getOrElse(0) { "Verified profiles" },
                icon = Icons.Filled.VerifiedUser,
                compact = compact,
                modifier = Modifier.weight(1f)
            )
            HeroTrustPill(
                label = resolvedTrust.getOrElse(1) { "Private photos" },
                icon = Icons.Filled.Lock,
                compact = compact,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HeroTrustPill(
    label: String,
    icon: ImageVector,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(if (compact) 76.dp else 92.dp),
        shape = RoundedCornerShape(48.dp),
        color = Color.White.copy(alpha = 0.22f),
        border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.48f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 14.dp else 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(if (compact) 22.dp else 26.dp))
            Spacer(Modifier.size(if (compact) 10.dp else 14.dp))
            Text(
                label.uppercase(),
                color = Color.White,
                fontSize = if (compact) 18.sp else 22.sp,
                lineHeight = if (compact) 27.sp else 33.sp,
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AuthCard(
    state: AuthUiState,
    content: AuthContentData,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onRegister: () -> Unit,
    onLogin: () -> Unit,
    onGoogle: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = LoginPanel,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.75f)),
        shadowElevation = 22.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = if (compact) 22.dp else 30.dp, vertical = if (compact) 20.dp else 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 20.dp)
        ) {
            Button(
                onClick = onRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 58.dp else 70.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LoginPink, contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Icon(Icons.Filled.PhoneAndroid, contentDescription = null, modifier = Modifier.size(if (compact) 26.dp else 32.dp))
                Spacer(Modifier.size(if (compact) 14.dp else 18.dp))
                Text(
                    content.registerCta.ifBlank { "Register with mobile" },
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 20.sp else 24.sp
                )
            }

            OrDivider(compact)

            SocialButton(
                label = content.googleCta.ifBlank { "Continue with Google" },
                enabled = state !is AuthUiState.Loading,
                compact = compact,
                modifier = Modifier.fillMaxWidth(),
                onClick = onGoogle
            )

            if (state is AuthUiState.Loading) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = LoginPink)
            } else if (state is AuthUiState.Error) {
                Text(
                    state.message,
                    color = LoginPink,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Already have an account?",
                    color = LoginDeepText,
                    fontSize = if (compact) 18.sp else 22.sp
                )
                TextButton(onClick = onLogin) {
                    Text(
                        "Log In",
                        color = LoginDarkPink,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (compact) 18.sp else 22.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun OrDivider(compact: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Divider(Modifier.weight(1f), color = LoginPanelLine)
        Text(
            "OR CONTINUE WITH",
            color = LoginMuted,
            fontSize = if (compact) 18.sp else 22.sp,
            letterSpacing = 3.2.sp,
            fontWeight = FontWeight.Medium
        )
        Divider(Modifier.weight(1f), color = LoginPanelLine)
    }
}

@Composable
private fun SocialButton(
    label: String,
    enabled: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(if (compact) 58.dp else 70.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LoginSocialBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = LoginSocialText,
            disabledContainerColor = Color.White,
            disabledContentColor = LoginSocialText
        )
    ) {
        Image(
            painter = painterResource(R.drawable.ic_google_g),
            contentDescription = null,
            modifier = Modifier.size(if (compact) 20.dp else 24.dp)
        )
        Spacer(Modifier.size(if (compact) 10.dp else 14.dp))
        Text(
            label,
            fontSize = if (compact) 18.sp else 22.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FooterLegal(
    modifier: Modifier = Modifier,
    compact: Boolean,
    prefix: String,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 18.dp else 28.dp)
    ) {
        LegalLinks(
            compact = compact,
            prefix = prefix.ifBlank { "By continuing, you agree to our" },
            onOpenTerms = onOpenTerms,
            onOpenPrivacy = onOpenPrivacy
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Security, contentDescription = null, tint = LoginProtection, modifier = Modifier.size(if (compact) 22.dp else 26.dp))
            Spacer(Modifier.size(if (compact) 20.dp else 28.dp))
            Text(
                "End-to-End Encrypted Data\nProtection",
                color = LoginProtection,
                fontSize = if (compact) 18.sp else 22.sp,
                lineHeight = if (compact) 27.sp else 32.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LegalLinks(
    compact: Boolean,
    prefix: String,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = LoginLegal)) {
            append(prefix)
            append("\n")
        }
        pushStringAnnotation(tag = "legal", annotation = "terms")
        withStyle(SpanStyle(color = LoginLegalStrong, textDecoration = TextDecoration.Underline)) {
            append("Terms of Service")
        }
        pop()
        withStyle(SpanStyle(color = LoginLegal)) {
            append(" and ")
        }
        pushStringAnnotation(tag = "legal", annotation = "privacy")
        withStyle(SpanStyle(color = LoginLegalStrong, textDecoration = TextDecoration.Underline)) {
            append("Privacy Policy")
        }
        pop()
    }
    ClickableText(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = TextStyle(
            textAlign = TextAlign.Center,
            fontSize = if (compact) 18.sp else 22.sp,
            lineHeight = if (compact) 28.sp else 34.sp,
            fontWeight = FontWeight.Medium
        ),
        onClick = { offset ->
            when (text.getStringAnnotations(tag = "legal", start = offset, end = offset).firstOrNull()?.item) {
                "terms" -> onOpenTerms()
                "privacy" -> onOpenPrivacy()
            }
        }
    )
}

@Composable
private fun WaveBottom(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(0f, size.height * 0.38f)
            cubicTo(size.width * 0.22f, -size.height * 0.18f, size.width * 0.34f, size.height * 1.06f, size.width * 0.56f, size.height * 0.44f)
            cubicTo(size.width * 0.78f, -size.height * 0.18f, size.width * 0.88f, size.height * 0.22f, size.width, size.height * 0.02f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path = path, color = LoginWave)
    }
}

@Composable
private fun rememberGoogleWebClientId(configuredClientId: String): String {
    val context = LocalContext.current
    return BuildConfig.GOOGLE_WEB_CLIENT_ID.ifBlank {
        configuredClientId.ifBlank {
            val defaultWebClientId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (defaultWebClientId != 0) context.getString(defaultWebClientId) else ""
        }
    }
}

private val LoginCream = Color(0xFFFFF8EF)
private val LoginPink = Color(0xFFD72964)
private val LoginDarkPink = Color(0xFFB50742)
private val LoginHeroOverlay = Color(0xCCBF0B4E)
private val LoginPanel = Color(0xFFFFEEF3)
private val LoginPanelLine = Color(0xFFEFD6DA)
private val LoginSocialBorder = Color(0xFFE7B7C0)
private val LoginSocialText = Color(0xFF5F5D5B)
private val LoginDeepText = Color(0xFF49333B)
private val LoginMuted = Color(0xFF8A7075)
private val LoginLegal = Color(0xFF8A7075)
private val LoginLegalStrong = Color(0xFF4C2832)
private val LoginProtection = Color(0xFF73716E)
private val LoginWave = Color(0xFFF5C9D1)
private val GoogleBlue = Color(0xFF4285F4)

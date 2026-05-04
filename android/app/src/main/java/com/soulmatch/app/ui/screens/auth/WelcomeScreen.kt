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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Work
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
        val compact = maxHeight < 940.dp
        val heroHeight = if (compact) 300.dp else 428.dp
        val cardTop = heroHeight - if (compact) 20.dp else 34.dp
        val waveHeight = if (compact) 48.dp else 76.dp

        HeroBackdrop(
            imageUrl = branding.previewImageUrl,
            height = heroHeight
        )
        HeroWaveSeparator(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = heroHeight - waveHeight)
                .fillMaxWidth()
                .height(waveHeight)
        )
        HeroIntro(
            appTitle = branding.appTitle.ifBlank { "SoulMatch" },
            heroTitle = if (content.heroTitle == "Serious matchmaking for modern families") "Trusted matchmaking for" else content.heroTitle,
            heroSubtitle = if (content.heroSubtitle == "Serious matchmaking for modern families") "families & agents" else content.heroSubtitle,
            heroHeight = heroHeight,
            compact = compact
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = cardTop, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 24.dp)
        ) {
            AuthCard(
                state = state,
                content = content,
                compact = compact,
                onRegister = onRegister,
                onLogin = onLogin,
                onAgentRegister = onContinue,
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
    heroTitle: String,
    heroSubtitle: String,
    heroHeight: Dp,
    compact: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight)
            .padding(horizontal = 26.dp)
            .padding(top = if (compact) 56.dp else 94.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            appTitle,
            color = Color.White,
            fontSize = if (compact) 36.sp else 52.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(if (compact) 12.dp else 22.dp))
        HeroAccentDivider(compact = compact)
        Spacer(Modifier.height(if (compact) 14.dp else 28.dp))
        Text(
            heroTitle,
            color = LoginGold,
            fontSize = if (compact) 20.sp else 28.sp,
            lineHeight = if (compact) 25.sp else 34.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Text(
            heroSubtitle,
            color = Color.White,
            fontSize = if (compact) 24.sp else 36.sp,
            lineHeight = if (compact) 30.sp else 44.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun HeroAccentDivider(
    compact: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(0.66f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 16.dp)
    ) {
        Divider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.72f), thickness = 1.dp)
        Icon(
            Icons.Filled.Favorite,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(if (compact) 14.dp else 18.dp)
        )
        Divider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.72f), thickness = 1.dp)
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
    onAgentRegister: () -> Unit,
    onGoogle: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 22.dp)
    ) {
        Button(
            onClick = onRegister,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 54.dp else 72.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LoginPink, contentColor = Color.White),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp)
        ) {
            Icon(Icons.Filled.PhoneAndroid, contentDescription = null, modifier = Modifier.size(if (compact) 22.dp else 30.dp))
            Spacer(Modifier.size(if (compact) 10.dp else 16.dp))
            Text(
                "Continue with Mobile",
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (compact) 18.sp else 24.sp
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
                fontSize = if (compact) 16.sp else 22.sp
            )
            TextButton(onClick = onLogin) {
                Text(
                    "Log in",
                    color = LoginDarkPink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (compact) 16.sp else 22.sp
                )
            }
        }

        AgentRegistrationCard(compact = compact, onRegister = onAgentRegister)
        SafetyHighlights(compact = compact)
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
            "OR",
            color = LoginMuted,
            fontSize = if (compact) 16.sp else 22.sp,
            letterSpacing = 0.6.sp,
            fontWeight = FontWeight.SemiBold
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
        modifier = modifier.height(if (compact) 54.dp else 70.dp),
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
            modifier = Modifier.size(if (compact) 18.dp else 24.dp)
        )
        Spacer(Modifier.size(if (compact) 8.dp else 14.dp))
        Text(
            label,
            fontSize = if (compact) 17.sp else 22.sp,
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
        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Divider(modifier = Modifier.weight(1f), color = LoginPanelLine)
            Icon(Icons.Filled.Security, contentDescription = null, tint = LoginMuted, modifier = Modifier.size(if (compact) 18.dp else 20.dp))
            Divider(modifier = Modifier.weight(1f), color = LoginPanelLine)
        }
        LegalLinks(
            compact = compact,
            prefix = prefix.ifBlank { "By continuing, you agree to our" },
            onOpenTerms = onOpenTerms,
            onOpenPrivacy = onOpenPrivacy
        )
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.White.copy(alpha = 0.66f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.65f)),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (compact) 14.dp else 22.dp, vertical = if (compact) 8.dp else 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = LoginProtection, modifier = Modifier.size(if (compact) 16.dp else 20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "End-to-End Encrypted  •  No Spam  •  No Fake Profiles",
                    color = LoginProtection,
                    fontSize = if (compact) 12.sp else 18.sp,
                    lineHeight = if (compact) 16.sp else 24.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AgentRegistrationCard(
    compact: Boolean,
    onRegister: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.52f),
        border = BorderStroke(1.dp, LoginSocialBorder.copy(alpha = 0.95f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 14.dp else 22.dp, vertical = if (compact) 14.dp else 22.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = LoginPink.copy(alpha = 0.1f),
                modifier = Modifier.size(if (compact) 48.dp else 64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Work, contentDescription = null, tint = LoginPink, modifier = Modifier.size(if (compact) 24.dp else 32.dp))
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Are you a Matrimony Agent?",
                    color = LoginDeepText,
                    fontSize = if (compact) 18.sp else 26.sp,
                    lineHeight = if (compact) 23.sp else 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Manage multiple profiles and help families find the perfect match.",
                    color = LoginSocialText,
                    fontSize = if (compact) 13.sp else 18.sp,
                    lineHeight = if (compact) 18.sp else 25.sp
                )
            }
            OutlinedButton(
                onClick = onRegister,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.2.dp, LoginPink.copy(alpha = 0.65f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = LoginPink
                )
            ) {
                Text(
                    "Register\nas Agent",
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (compact) 14.sp else 18.sp,
                    lineHeight = if (compact) 17.sp else 22.sp
                )
            }
        }
    }
}

@Composable
private fun SafetyHighlights(compact: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        SafetyHighlightItem(
            icon = Icons.Filled.VerifiedUser,
            title = "Verified Profiles",
            body = "Every profile is verified for your safety.",
            compact = compact,
            modifier = Modifier.weight(1f)
        )
        HighlightDivider(compact = compact)
        SafetyHighlightItem(
            icon = Icons.Filled.Lock,
            title = "Private Photos",
            body = "Your photos stay private and access is controlled.",
            compact = compact,
            modifier = Modifier.weight(1f)
        )
        HighlightDivider(compact = compact)
        SafetyHighlightItem(
            icon = Icons.Filled.Security,
            title = "Safe & Secure",
            body = "We use advanced security to protect you always.",
            compact = compact,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HighlightDivider(compact: Boolean) {
    Spacer(
        modifier = Modifier
            .padding(top = if (compact) 12.dp else 14.dp)
            .width(1.dp)
            .height(if (compact) 88.dp else 124.dp)
            .background(LoginPanelLine)
    )
}

@Composable
private fun SafetyHighlightItem(
    icon: ImageVector,
    title: String,
    body: String,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = LoginPink.copy(alpha = 0.1f),
            modifier = Modifier.size(if (compact) 42.dp else 62.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = LoginPink, modifier = Modifier.size(if (compact) 18.dp else 28.dp))
            }
        }
        Text(
            title,
            color = LoginDeepText,
            fontSize = if (compact) 14.sp else 20.sp,
            lineHeight = if (compact) 18.sp else 26.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Text(
            body,
            color = LoginSocialText,
            fontSize = if (compact) 11.sp else 16.sp,
            lineHeight = if (compact) 15.sp else 22.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HeroWaveSeparator(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(0f, size.height * 0.48f)
            cubicTo(size.width * 0.16f, size.height * 0.08f, size.width * 0.34f, size.height * 0.86f, size.width * 0.54f, size.height * 0.52f)
            cubicTo(size.width * 0.76f, size.height * 0.12f, size.width * 0.9f, size.height * 0.72f, size.width, size.height * 0.2f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path = path, color = LoginCream)
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
            append(" ")
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
            fontSize = if (compact) 14.sp else 22.sp,
            lineHeight = if (compact) 20.sp else 34.sp,
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
private val LoginGold = Color(0xFFFFD68A)
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

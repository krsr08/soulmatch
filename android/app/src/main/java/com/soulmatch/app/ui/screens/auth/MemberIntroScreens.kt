package com.soulmatch.app.ui.screens.auth

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.ui.design.SoulMatchPrimaryButton
import com.soulmatch.app.ui.design.SoulMatchTokens
import com.soulmatch.app.ui.viewmodels.IntroViewModel

@Composable
fun LanguageSelectionScreen(
    onContinue: () -> Unit,
    vm: IntroViewModel = hiltViewModel()
) {
    val savedLanguage by vm.appLanguage.collectAsStateWithLifecycle()
    var selected by remember(savedLanguage) { mutableStateOf(savedLanguage ?: "English") }
    val languages = listOf(
        "English",
        "Hindi",
        "Telugu",
        "Tamil",
        "Malayalam",
        "Kannada",
        "Gujarati",
        "Bengali",
        "Oriya",
        "Marati"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoulMatchTokens.Bg)
            .statusBarsPadding()
            .padding(horizontal = 22.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(86.dp))
        SoftIcon { Text("A", color = SoulMatchTokens.Tangerine, fontSize = 34.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(28.dp))
        Text(
            text = "Choose your language",
            color = SoulMatchTokens.Text,
            fontFamily = FontFamily.Serif,
            fontSize = 34.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Select the language you prefer for SoulMatch. You can change this later in settings.",
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
            color = SoulMatchTokens.Muted,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 26.sp,
            textAlign = TextAlign.Center
        )
        languages.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { language ->
                    LanguagePill(
                        label = language,
                        selected = selected == language,
                        modifier = Modifier.weight(1f),
                        onClick = { selected = language }
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.weight(1f))
        SoulMatchPrimaryButton(
            text = "Continue",
            onClick = { vm.saveLanguage(selected, onContinue) },
            modifier = Modifier.height(64.dp)
        )
    }
}

@Composable
fun OnboardingBenefitScreen(
    onContinue: () -> Unit,
    vm: IntroViewModel = hiltViewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoulMatchTokens.Bg)
            .statusBarsPadding()
            .padding(horizontal = 22.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(70.dp))
        SoftIcon { Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = SoulMatchTokens.Tangerine, modifier = Modifier.size(34.dp)) }
        Spacer(Modifier.height(28.dp))
        Text(
            text = "A safer way to meet",
            color = SoulMatchTokens.Text,
            fontFamily = FontFamily.Serif,
            fontSize = 34.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "SoulMatch helps you move from account setup to profile creation with confidence.",
            modifier = Modifier.padding(top = 16.dp, bottom = 28.dp),
            color = SoulMatchTokens.Muted,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 26.sp,
            textAlign = TextAlign.Center
        )
        BenefitCard(Icons.Filled.VerifiedUser, "Profile verification", "ID and contact checks help families trust every introduction.")
        BenefitCard(Icons.Outlined.Shield, "Privacy controls", "Choose who can view photos, horoscope, contact details, and family information.")
        BenefitCard(Icons.Outlined.AutoAwesome, "Match recommendations", "Get meaningful suggestions based on community, lifestyle, preferences, and intent.")
        Row(
            modifier = Modifier.padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(width = if (index == 3) 48.dp else 16.dp, height = 14.dp)
                        .background(
                            if (index == 3) SoulMatchTokens.Tangerine else SoulMatchTokens.Border,
                            RoundedCornerShape(999.dp)
                        )
                )
            }
        }
        Spacer(Modifier.weight(1f))
        SoulMatchPrimaryButton(
            text = "Continue to profile creation",
            onClick = { vm.completeOnboarding(onContinue) },
            modifier = Modifier.height(64.dp)
        )
    }
}

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onOpenReset: () -> Unit
) {
    var contact by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoulMatchTokens.Bg)
            .statusBarsPadding()
            .padding(horizontal = 22.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BackText("Login", onBack)
        Spacer(Modifier.height(46.dp))
        SoftIcon { Icon(Icons.Filled.Key, contentDescription = null, tint = SoulMatchTokens.Tangerine, modifier = Modifier.size(34.dp)) }
        Spacer(Modifier.height(32.dp))
        AuthTitle("Forgot password?")
        Text(
            text = "Enter your registered mobile number or email and we will send a secure reset OTP.",
            modifier = Modifier.padding(top = 18.dp, bottom = 28.dp),
            color = SoulMatchTokens.Muted,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 26.sp,
            textAlign = TextAlign.Center
        )
        LabeledInput(
            label = "Mobile number or email",
            value = contact,
            onValueChange = { contact = it },
            placeholder = "Enter mobile number or email",
            leadingIcon = Icons.Outlined.Mail
        )
        Spacer(Modifier.weight(1f))
        SoulMatchPrimaryButton(
            text = "Send OTP / reset link",
            enabled = contact.isNotBlank(),
            onClick = onOpenReset,
            modifier = Modifier.height(64.dp)
        )
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 12.dp)) {
            Text("Back to login", color = SoulMatchTokens.Tangerine, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ResetPasswordScreen(
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val valid = password.length >= 6 && password == confirm
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoulMatchTokens.Bg)
            .statusBarsPadding()
            .padding(horizontal = 22.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BackText("Forgot password", onBack)
        Spacer(Modifier.height(42.dp))
        SoftIcon { Icon(Icons.Filled.Lock, contentDescription = null, tint = SoulMatchTokens.Tangerine, modifier = Modifier.size(34.dp)) }
        Spacer(Modifier.height(32.dp))
        AuthTitle("Set new password")
        Text(
            text = "Choose a strong password to keep your profile private.",
            modifier = Modifier.padding(top = 18.dp, bottom = 28.dp),
            color = SoulMatchTokens.Muted,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 26.sp,
            textAlign = TextAlign.Center
        )
        LabeledInput(
            label = "New password",
            value = password,
            onValueChange = { password = it },
            placeholder = "Enter password",
            leadingIcon = Icons.Filled.Lock,
            isPassword = true
        )
        Spacer(Modifier.height(18.dp))
        LabeledInput(
            label = "Confirm password",
            value = confirm,
            onValueChange = { confirm = it },
            placeholder = "Confirm password",
            leadingIcon = Icons.Filled.Lock,
            isPassword = true
        )
        StrengthBars(password)
        if (valid) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                color = Color(0xFFEAF8F1),
                border = BorderStroke(1.dp, Color(0xFFA6E3C1)),
                shape = RoundedCornerShape(SoulMatchTokens.CardRadius)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SoulMatchTokens.Success)
                    Text("Password updated successfully.", color = SoulMatchTokens.Success, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        SoulMatchPrimaryButton(
            text = "Submit",
            enabled = valid,
            onClick = onDone,
            modifier = Modifier.height(64.dp)
        )
    }
}

@Composable
private fun LanguagePill(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .height(58.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) SoulMatchTokens.Tangerine else Color.White,
        border = BorderStroke(1.dp, if (selected) SoulMatchTokens.Tangerine else SoulMatchTokens.Border)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (selected) Color.White else SoulMatchTokens.Text,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun BenefitCard(icon: ImageVector, title: String, subtitle: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(SoulMatchTokens.CardRadius),
        color = Color.White,
        border = BorderStroke(1.dp, SoulMatchTokens.Border),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(shape = CircleShape, color = SoulMatchTokens.Ivory, modifier = Modifier.size(58.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = SoulMatchTokens.Tangerine, modifier = Modifier.size(28.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, color = SoulMatchTokens.Text, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                Text(subtitle, color = SoulMatchTokens.Muted, style = MaterialTheme.typography.bodyMedium, lineHeight = 21.sp)
            }
        }
    }
}

@Composable
private fun SoftIcon(content: @Composable () -> Unit) {
    Surface(shape = CircleShape, color = SoulMatchTokens.Ivory, border = BorderStroke(1.dp, SoulMatchTokens.Border), modifier = Modifier.size(86.dp)) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun BackText(label: String, onClick: () -> Unit) {
    Text(
        text = "< $label",
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = SoulMatchTokens.Tangerine,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun AuthTitle(text: String) {
    Text(
        text = text,
        color = SoulMatchTokens.Text,
        fontFamily = FontFamily.Serif,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun LabeledInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    isPassword: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, color = SoulMatchTokens.Muted, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp),
            placeholder = { Text(placeholder, color = SoulMatchTokens.Muted) },
            leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = SoulMatchTokens.Tangerine) },
            singleLine = true,
            shape = RoundedCornerShape(SoulMatchTokens.CardRadius),
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None
        )
    }
}

@Composable
private fun StrengthBars(password: String) {
    val level = when {
        password.length >= 10 && password.any { !it.isLetterOrDigit() } -> 4
        password.length >= 8 -> 3
        password.length >= 6 -> 2
        password.isNotEmpty() -> 1
        else -> 0
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .background(if (index < level) SoulMatchTokens.Gold else SoulMatchTokens.Border, RoundedCornerShape(999.dp))
            )
        }
    }
    val label = when (level) {
        0 -> "Enter a password to check strength"
        1 -> "Weak strength - use at least 6 characters"
        2 -> "Medium strength - add a symbol for better security"
        3 -> "Good strength"
        else -> "Strong password"
    }
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        color = SoulMatchTokens.Muted,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )
}

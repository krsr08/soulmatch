package com.soulmatch.app.ui.screens.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.ui.screens.design.DesignHotspot
import com.soulmatch.app.ui.screens.design.ExactDesignScreen
import com.soulmatch.app.ui.screens.design.backHotspot
import com.soulmatch.app.ui.viewmodels.IntroViewModel

@Composable
fun LanguageSelectionScreen(
    onContinue: () -> Unit,
    vm: IntroViewModel = hiltViewModel()
) {
    val savedLanguage by vm.appLanguage.collectAsStateWithLifecycle()
    var selected by remember(savedLanguage) { mutableStateOf(savedLanguage ?: "English") }
    ExactDesignScreen(
        assetName = "02_language_selection_screen.png",
        hotspots = listOf(
            DesignHotspot(28f, 312f, 158f, 46f) { selected = "English" },
            DesignHotspot(204f, 312f, 158f, 46f) { selected = "Hindi" },
            DesignHotspot(28f, 366f, 158f, 46f) { selected = "Telugu" },
            DesignHotspot(204f, 366f, 158f, 46f) { selected = "Tamil" },
            DesignHotspot(28f, 420f, 158f, 46f) { selected = "Malayalam" },
            DesignHotspot(204f, 420f, 158f, 46f) { selected = "Kannada" },
            DesignHotspot(28f, 474f, 158f, 46f) { selected = "Gujarati" },
            DesignHotspot(204f, 474f, 158f, 46f) { selected = "Bengali" },
            DesignHotspot(28f, 528f, 158f, 46f) { selected = "Oriya" },
            DesignHotspot(204f, 528f, 158f, 46f) { selected = "Marati" },
            DesignHotspot(28f, 754f, 334f, 58f) { vm.saveLanguage(selected, onContinue) }
        )
    )
}

@Composable
fun OnboardingBenefitScreen(
    onContinue: () -> Unit,
    vm: IntroViewModel = hiltViewModel()
) {
    ExactDesignScreen(
        assetName = "08_onboarding_benefit_screen.png",
        hotspots = listOf(
            DesignHotspot(28f, 754f, 334f, 58f) { vm.completeOnboarding(onContinue) }
        )
    )
}

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onOpenReset: () -> Unit
) {
    ExactDesignScreen(
        assetName = "06_forgot_password_screen.png",
        hotspots = listOf(
            backHotspot(onBack),
            DesignHotspot(28f, 754f, 334f, 58f) { onOpenReset() },
            DesignHotspot(112f, 814f, 166f, 28f) { onBack() }
        )
    )
}

@Composable
fun ResetPasswordScreen(
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    ExactDesignScreen(
        assetName = "07_reset_password_screen.png",
        hotspots = listOf(
            backHotspot(onBack),
            DesignHotspot(28f, 754f, 334f, 58f) { onDone() }
        )
    )
}

package com.soulmatch.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.google.firebase.messaging.FirebaseMessaging
import com.soulmatch.app.data.api.ControlPlaneApiService
import com.soulmatch.app.data.api.NotificationApiService
import com.soulmatch.app.data.api.ProfileApiService
import com.soulmatch.app.data.auth.resolveAgentRoute
import com.soulmatch.app.data.auth.resolvePostLoginRoute
import com.soulmatch.app.data.auth.resolveWizardStep
import com.soulmatch.app.data.local.UserPreferences
import com.soulmatch.app.data.models.FcmTokenRequest
import com.soulmatch.app.data.models.NotificationPromptContentData
import com.soulmatch.app.data.models.RuntimeConfigData
import com.soulmatch.app.data.payments.PaymentCoordinator
import com.soulmatch.app.ui.navigation.AppNavigation
import com.soulmatch.app.ui.screens.system.LaunchBrandScreen
import com.soulmatch.app.ui.screens.system.MaintenanceScreen
import com.soulmatch.app.ui.theme.SoulMatchTheme
import com.soulmatch.app.util.CrashReporter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject

@AndroidEntryPoint
class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    @Inject lateinit var userPreferences: UserPreferences
    @Inject lateinit var paymentCoordinator: PaymentCoordinator
    @Inject lateinit var profileApi: ProfileApiService
    @Inject lateinit var controlPlaneApi: ControlPlaneApiService
    @Inject lateinit var notificationApi: NotificationApiService

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) registerCurrentFcmToken()
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        return try {
            super.dispatchGenericMotionEvent(ev)
        } catch (error: IllegalStateException) {
            if (error.message?.contains("ACTION_HOVER_EXIT event was not cleared") == true) {
                CrashReporter.breadcrumb("ignored_hover_exit_state_error")
                true
            } else {
                throw error
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Checkout.preload(applicationContext)
        setContent {
            val token by userPreferences.authToken.collectAsState(initial = null)
            val pushEnabled by userPreferences.pushNotifications.collectAsState(initial = true)
            val notificationPromptDismissed by userPreferences.notificationPromptDismissed.collectAsState(initial = false)
            var runtimeConfig by remember { mutableStateOf(RuntimeConfigData()) }
            var startDestination by remember(token) { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                suspend fun refreshRuntimeConfig() {
                    runCatching { controlPlaneApi.getRuntimeConfig() }
                        .getOrNull()
                        ?.body()
                        ?.takeIf { it.success }
                        ?.data
                        ?.let { runtimeConfig = it }
                }

                refreshRuntimeConfig()
                while (true) {
                    delay(45_000)
                    refreshRuntimeConfig()
                }
            }

            LaunchedEffect(token) {
                startDestination = null
                if (token.isNullOrEmpty()) {
                    CrashReporter.clearUser()
                    userPreferences.clearPendingAuthRoute()
                    startDestination = "welcome"
                    return@LaunchedEffect
                }
                CrashReporter.identify(
                    userId = userPreferences.userId.first(),
                    userType = userPreferences.userType.first(),
                    profileId = userPreferences.profileId.first(),
                    advisorId = userPreferences.advisorId.first()
                )
                ensurePushTokenRegistered()

                val storedUserType = userPreferences.userType.first().orEmpty().lowercase()
                val pendingAuthRoute = userPreferences.pendingAuthRoute.first()
                if (!pendingAuthRoute.isNullOrBlank()) {
                    userPreferences.clearPendingAuthRoute()
                    if (storedUserType == "agent" && pendingAuthRoute.startsWith("agent_")) {
                        val agentResponse = runCatching { profileApi.getAgentProfile() }.getOrNull()
                        val agentBody = agentResponse?.body()
                        startDestination = if (agentResponse?.isSuccessful == true && agentBody?.success == true) {
                            userPreferences.saveAdvisorId(agentBody.data?.advisorId.orEmpty())
                            resolveAgentRoute(agentBody.data)
                        } else {
                            pendingAuthRoute
                        }
                    } else {
                        startDestination = pendingAuthRoute
                    }
                    return@LaunchedEffect
                }

                if (storedUserType == "agent") {
                    val agentResponse = runCatching { profileApi.getAgentProfile() }.getOrNull()
                    val agentBody = agentResponse?.body()
                    when {
                        agentResponse?.isSuccessful == true && agentBody?.success == true -> {
                            userPreferences.saveAdvisorId(agentBody.data?.advisorId.orEmpty())
                            CrashReporter.identify(
                                userId = userPreferences.userId.first(),
                                userType = "agent",
                                advisorId = agentBody.data?.advisorId
                            )
                            startDestination = resolveAgentRoute(agentBody.data)
                        }
                        agentResponse?.code() == 401 -> {
                            userPreferences.clearAll()
                            startDestination = "welcome"
                        }
                        else -> {
                            startDestination = "agent_onboarding"
                        }
                    }
                    return@LaunchedEffect
                }

                val response = runCatching { profileApi.getMyProfile() }.getOrNull()
                val body = response?.body()
                when {
                    response?.isSuccessful == true && body?.success == true -> {
                        val profile = body.data
                        if (profile?.profileId.isNullOrBlank()) {
                            userPreferences.clearProfileProgress()
                        } else {
                            userPreferences.saveProfileId(profile?.profileId.orEmpty())
                        }
                        CrashReporter.identify(
                            userId = userPreferences.userId.first(),
                            userType = "member",
                            profileId = profile?.profileId
                        )
                        userPreferences.saveWizardStep(resolveWizardStep(profile) ?: 7)
                        startDestination = resolvePostLoginRoute(profile)
                    }
                    response?.code() == 401 -> {
                        userPreferences.clearAll()
                        startDestination = "welcome"
                    }
                    else -> {
                        startDestination = "dashboard"
                    }
                }
            }

            SoulMatchTheme(themeConfig = runtimeConfig.theme) {
                if (runtimeConfig.maintenance.enabled) {
                    MaintenanceScreen(config = runtimeConfig)
                } else {
                    val route = startDestination
                    if (route == null) {
                        LaunchBrandScreen()
                    } else {
                        AppNavigation(
                            startDestination = route,
                            isAuthenticated = !token.isNullOrEmpty(),
                            navigationLocked = route == "auth_role_selection",
                            branding = runtimeConfig.branding,
                            content = runtimeConfig.content,
                            legal = runtimeConfig.legal,
                            clientIntegrations = runtimeConfig.clientIntegrations
                        )
                        if (
                            !token.isNullOrEmpty() &&
                            runtimeConfig.content.notificationPrompt.enabled &&
                            !pushEnabled &&
                            !notificationPromptDismissed
                        ) {
                            NotificationOptInPrompt(
                                content = runtimeConfig.content.notificationPrompt,
                                onAllow = {
                                    lifecycleScope.launch {
                                        userPreferences.savePushNotifications(true)
                                        userPreferences.saveNotificationPromptDismissed(true)
                                        ensurePushTokenRegistered()
                                    }
                                },
                                onDismiss = {
                                    lifecycleScope.launch {
                                        userPreferences.saveNotificationPromptDismissed(true)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        lifecycleScope.launch {
            CrashReporter.breadcrumb("razorpay_success")
            paymentCoordinator.completeSuccess(
                paymentId = razorpayPaymentId.orEmpty(),
                signature = paymentData?.signature.orEmpty()
            )
        }
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        lifecycleScope.launch {
            CrashReporter.breadcrumb("razorpay_error:$code")
            paymentCoordinator.completeFailure(
                message = resolvePaymentFailureMessage(code, response),
                code = code,
                rawResponse = response
            )
        }
    }

    private fun resolvePaymentFailureMessage(code: Int, response: String?): String {
        val error = runCatching {
            response
                ?.takeIf { it.isNotBlank() }
                ?.let { JSONObject(it).optJSONObject("error") }
        }.getOrNull()
        val description = error?.optString("description").orEmpty()
        val reason = error?.optString("reason").orEmpty()
        val normalized = listOf(description, reason, response.orEmpty())
            .joinToString(" ")
            .lowercase()

        return when {
            code == 0 || normalized.contains("cancel") ->
                "Payment was cancelled. No amount was charged by SoulMatch."
            normalized.contains("network") || normalized.contains("timeout") ->
                "Payment could not be completed because of a network issue. Please check your connection and try again."
            description.isNotBlank() ->
                "Payment failed in Razorpay: $description"
            else ->
                "Payment failed. No membership was activated, and you can try again safely."
        }
    }

    private fun ensurePushTokenRegistered() {
        lifecycleScope.launch {
            if (!userPreferences.pushNotifications.first()) return@launch
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return@launch
            }
            registerCurrentFcmToken()
        }
    }

    private fun registerCurrentFcmToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
            if (fcmToken.isBlank()) return@addOnSuccessListener
            lifecycleScope.launch {
                val authToken = userPreferences.authToken.first()
                val enabled = userPreferences.pushNotifications.first()
                if (authToken.isNullOrBlank() || !enabled) return@launch
                runCatching { notificationApi.registerFcmToken(FcmTokenRequest(fcmToken)) }
                    .onFailure { CrashReporter.recordNonFatal(it, "fcm_token_registration") }
            }
        }
    }
}

@Composable
private fun NotificationOptInPrompt(
    content: NotificationPromptContentData,
    onAllow: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(42.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        content.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    if (content.subtitle.isNotBlank()) {
                        Text(
                            content.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    content.bullets.take(4).forEach { bullet ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                bullet,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Button(
                    onClick = onAllow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(content.allowCta, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDismiss) {
                    Text(content.laterCta, color = Color(0xFF6B7280), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

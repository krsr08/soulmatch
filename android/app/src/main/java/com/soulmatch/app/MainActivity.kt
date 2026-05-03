package com.soulmatch.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.google.firebase.messaging.FirebaseMessaging
import com.soulmatch.app.data.api.ControlPlaneApiService
import com.soulmatch.app.data.api.NotificationApiService
import com.soulmatch.app.data.api.ProfileApiService
import com.soulmatch.app.data.auth.resolvePostLoginRoute
import com.soulmatch.app.data.auth.resolveWizardStep
import com.soulmatch.app.data.local.UserPreferences
import com.soulmatch.app.data.models.FcmTokenRequest
import com.soulmatch.app.data.models.RuntimeConfigData
import com.soulmatch.app.data.payments.PaymentCoordinator
import com.soulmatch.app.ui.navigation.AppNavigation
import com.soulmatch.app.ui.screens.system.LaunchBrandScreen
import com.soulmatch.app.ui.screens.system.MaintenanceScreen
import com.soulmatch.app.ui.theme.SoulMatchTheme
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Checkout.preload(applicationContext)
        setContent {
            val token by userPreferences.authToken.collectAsState(initial = null)
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
                    startDestination = "welcome"
                    return@LaunchedEffect
                }
                ensurePushTokenRegistered()

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
                            branding = runtimeConfig.branding,
                            content = runtimeConfig.content,
                            legal = runtimeConfig.legal,
                            clientIntegrations = runtimeConfig.clientIntegrations
                        )
                    }
                }
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        lifecycleScope.launch {
            paymentCoordinator.completeSuccess(
                paymentId = razorpayPaymentId.orEmpty(),
                signature = paymentData?.signature.orEmpty()
            )
        }
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        lifecycleScope.launch {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        registerCurrentFcmToken()
    }

    private fun registerCurrentFcmToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
            if (fcmToken.isBlank()) return@addOnSuccessListener
            lifecycleScope.launch {
                val authToken = userPreferences.authToken.first()
                val enabled = userPreferences.pushNotifications.first()
                if (authToken.isNullOrBlank() || !enabled) return@launch
                runCatching { notificationApi.registerFcmToken(FcmTokenRequest(fcmToken)) }
            }
        }
    }
}

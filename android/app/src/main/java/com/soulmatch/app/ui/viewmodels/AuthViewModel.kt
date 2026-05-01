package com.soulmatch.app.ui.viewmodels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmatch.app.data.api.AuthApiService
import com.soulmatch.app.data.api.ProfileApiService
import com.soulmatch.app.data.auth.resolvePostLoginRoute
import com.soulmatch.app.data.auth.resolveWizardStep
import com.soulmatch.app.data.local.UserPreferences
import com.soulmatch.app.data.models.AuthData
import com.soulmatch.app.data.models.GoogleLoginRequest
import com.soulmatch.app.data.models.SendOTPRequest
import com.soulmatch.app.data.models.VerifyOTPRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object OTPSent : AuthUiState()
    data class Verified(val isNewUser: Boolean, val route: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authApi: AuthApiService,
    private val profileApi: ProfileApiService,
    private val prefs: UserPreferences
) : ViewModel() {
    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _state.asStateFlow()

    fun clearError() {
        if (_state.value is AuthUiState.Error) {
            _state.value = AuthUiState.Idle
        }
    }

    fun reportError(message: String) {
        _state.value = AuthUiState.Error(message)
    }

    fun sendOTP(phone: String) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            try {
                val r = authApi.sendOTP(SendOTPRequest(phone))
                _state.value = if (r.isSuccessful && r.body()?.success == true) AuthUiState.OTPSent else AuthUiState.Error(r.body()?.error?.message ?: "Failed to send OTP")
            } catch (e: Exception) { _state.value = AuthUiState.Error(e.message ?: "Network error") }
        }
    }
    fun verifyOTP(phone: String, otp: String) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            try {
                val r = authApi.verifyOTP(VerifyOTPRequest(phone, otp))
                if (r.isSuccessful && r.body()?.success == true) {
                    val d = r.body()!!.data!!
                    val route = persistSessionAndResolveRoute(d)
                    _state.value = AuthUiState.Verified(d.isNewUser, route)
                } else { _state.value = AuthUiState.Error(r.body()?.error?.message ?: "Invalid OTP") }
            } catch (e: Exception) { _state.value = AuthUiState.Error(e.message ?: "Network error") }
        }
    }

    fun googleLogin(googleIdToken: String?) {
        if (googleIdToken.isNullOrBlank()) {
            _state.value = AuthUiState.Error("Google sign-in did not return an ID token. Check OAuth client configuration and try again.")
            return
        }
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            try {
                val r = authApi.googleLogin(GoogleLoginRequest(googleIdToken))
                if (r.isSuccessful && r.body()?.success == true) {
                    val d = r.body()!!.data!!
                    val route = persistSessionAndResolveRoute(d)
                    _state.value = AuthUiState.Verified(d.isNewUser, route)
                } else {
                    _state.value = AuthUiState.Error(r.body()?.error?.message ?: "Google sign-in failed. Please try again.")
                }
            } catch (e: Exception) {
                _state.value = AuthUiState.Error("Google sign-in could not reach SoulMatch. Check your connection and retry.")
            }
        }
    }

    private suspend fun persistSessionAndResolveRoute(data: AuthData): String {
        prefs.saveAuthToken(data.accessToken)
        prefs.saveRefreshToken(data.refreshToken)
        prefs.saveUserId(data.userId)
        return if (data.isNewUser) {
            prefs.clearProfileProgress()
            prefs.saveWizardStep(1)
            "profile_wizard/1"
        } else {
            val profile = runCatching {
                profileApi.getMyProfile().body()?.takeIf { it.success }?.data
            }.getOrNull()
            val resolvedStep = resolveWizardStep(profile)
            if (profile?.profileId.isNullOrBlank()) {
                prefs.clearProfileProgress()
            } else {
                prefs.saveProfileId(profile?.profileId.orEmpty())
            }
            prefs.saveWizardStep(resolvedStep ?: 7)
            resolvePostLoginRoute(profile)
        }
    }
}

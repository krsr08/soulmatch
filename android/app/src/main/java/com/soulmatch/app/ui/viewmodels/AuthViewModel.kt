package com.soulmatch.app.ui.viewmodels
import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmatch.app.data.api.AuthApiService
import com.soulmatch.app.data.api.ProfileApiService
import com.soulmatch.app.data.auth.resolvePostLoginRoute
import com.soulmatch.app.data.auth.resolveWizardStep
import com.soulmatch.app.data.local.UserPreferences
import com.soulmatch.app.data.models.AuthData
import com.soulmatch.app.data.models.FirebasePhoneLoginRequest
import com.soulmatch.app.data.models.GoogleLoginRequest
import com.soulmatch.app.data.models.SendOTPRequest
import com.soulmatch.app.data.models.VerifyOTPRequest
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
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
    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }
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

    fun sendFirebaseOTP(activity: Activity, phone: String) {
        _state.value = AuthUiState.Loading
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    viewModelScope.launch {
                        completeFirebasePhoneVerification(phone, credential)
                    }
                }

                override fun onVerificationFailed(error: FirebaseException) {
                    _state.value = AuthUiState.Error(error.message ?: "Firebase could not send OTP.")
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    viewModelScope.launch {
                        prefs.savePendingOtpSession(phone, verificationId)
                        _state.value = AuthUiState.OTPSent
                    }
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOTP(phone: String, otp: String) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            try {
                val response = authApi.verifyOTP(
                    VerifyOTPRequest(
                        phone = phone,
                        otp = otp
                    )
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    val authData = response.body()!!.data!!
                    prefs.clearPendingOtpSession()
                    val route = persistSessionAndResolveRoute(authData)
                    _state.value = AuthUiState.Verified(authData.isNewUser, route)
                } else {
                    _state.value = AuthUiState.Error(response.body()?.error?.message ?: "Could not verify OTP.")
                }
            } catch (e: Exception) {
                _state.value = AuthUiState.Error(e.message ?: "Could not verify OTP.")
            }
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

    private suspend fun completeFirebasePhoneVerification(phone: String, credential: PhoneAuthCredential) {
        try {
            val signInResult = firebaseAuth.signInWithCredential(credential).await()
            val user = signInResult.user
            if (user == null) {
                _state.value = AuthUiState.Error("Firebase did not return a signed-in user.")
                return
            }
            val firebaseToken = user.getIdToken(false).await().token
            if (firebaseToken.isNullOrBlank()) {
                _state.value = AuthUiState.Error("Firebase did not return a valid phone token.")
                return
            }
            val response = authApi.firebasePhoneLogin(
                FirebasePhoneLoginRequest(
                    firebaseToken = firebaseToken,
                    phone = phone
                )
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val authData = response.body()!!.data!!
                prefs.clearPendingOtpSession()
                val route = persistSessionAndResolveRoute(authData)
                _state.value = AuthUiState.Verified(authData.isNewUser, route)
            } else {
                _state.value = AuthUiState.Error(response.body()?.error?.message ?: "SoulMatch could not complete phone sign-in.")
            }
        } catch (e: Exception) {
            _state.value = AuthUiState.Error(e.message ?: "Phone verification failed.")
        }
    }

    private suspend fun persistSessionAndResolveRoute(data: AuthData): String {
        prefs.saveAuthToken(data.accessToken)
        prefs.saveRefreshToken(data.refreshToken)
        prefs.saveUserId(data.userId)
        prefs.saveUserType(data.userType.ifBlank { "member" })
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

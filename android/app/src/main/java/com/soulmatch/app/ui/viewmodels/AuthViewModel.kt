package com.soulmatch.app.ui.viewmodels
import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmatch.app.data.api.AuthApiService
import com.soulmatch.app.data.api.ProfileApiService
import com.soulmatch.app.data.auth.resolveAgentRoute
import com.soulmatch.app.data.auth.resolvePostLoginRoute
import com.soulmatch.app.data.auth.resolveWizardStep
import com.soulmatch.app.data.local.UserPreferences
import com.soulmatch.app.data.models.AuthData
import com.soulmatch.app.data.models.FirebasePhoneLoginRequest
import com.soulmatch.app.data.models.GoogleLoginRequest
import com.soulmatch.app.data.models.SelectUserTypeRequest
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
import org.json.JSONObject
import retrofit2.Response
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

    fun sendOTP(phone: String, userType: String? = null) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            try {
                val r = authApi.sendOTP(SendOTPRequest(phone = phone, userType = userType))
                _state.value = if (r.isSuccessful && r.body()?.success == true) {
                    AuthUiState.OTPSent
                } else {
                    AuthUiState.Error(extractErrorMessage(r, "Failed to send OTP"))
                }
            } catch (e: Exception) { _state.value = AuthUiState.Error(e.message ?: "Network error") }
        }
    }

    fun sendFirebaseOTP(activity: Activity, phone: String, userType: String? = null) {
        _state.value = AuthUiState.Loading
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    viewModelScope.launch {
                        completeFirebasePhoneVerification(phone, credential, userType)
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

    fun verifyOTP(phone: String, otp: String, userType: String? = null) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            try {
                val response = authApi.verifyOTP(
                    VerifyOTPRequest(
                        phone = phone,
                        otp = otp,
                        userType = userType
                    )
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    val authData = response.body()!!.data!!
                    prefs.clearPendingOtpSession()
                    val route = persistSessionAndResolveRoute(authData, userType)
                    _state.value = AuthUiState.Verified(authData.isNewUser, route)
                } else {
                    _state.value = AuthUiState.Error(extractErrorMessage(response, "Could not verify OTP."))
                }
            } catch (e: Exception) {
                _state.value = AuthUiState.Error(e.message ?: "Could not verify OTP.")
            }
        }
    }

    fun verifyFirebaseOTP(phone: String, otp: String, userType: String? = null) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            val verificationId = prefs.pendingOtpVerificationId.firstOrNull()
            if (verificationId.isNullOrBlank()) {
                _state.value = AuthUiState.Error("OTP session expired. Please request a new code.")
                return@launch
            }
            try {
                val credential = PhoneAuthProvider.getCredential(verificationId, otp)
                completeFirebasePhoneVerification(phone, credential, userType)
            } catch (e: Exception) {
                _state.value = AuthUiState.Error(e.message ?: "Phone verification failed.")
            }
        }
    }

    fun googleLogin(googleIdToken: String?, userType: String? = null) {
        if (googleIdToken.isNullOrBlank()) {
            _state.value = AuthUiState.Error("Google sign-in did not return an ID token. Check OAuth client configuration and try again.")
            return
        }
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            try {
                val r = authApi.googleLogin(GoogleLoginRequest(googleToken = googleIdToken, userType = userType))
                if (r.isSuccessful && r.body()?.success == true) {
                    val d = r.body()!!.data!!
                    val route = persistSessionAndResolveRoute(d, userType)
                    _state.value = AuthUiState.Verified(d.isNewUser, route)
                } else {
                    _state.value = AuthUiState.Error(extractErrorMessage(r, "Google sign-in failed. Please try again."))
                }
            } catch (e: Exception) {
                _state.value = AuthUiState.Error(e.message ?: "Google sign-in could not reach SoulMatch. Check your connection and retry.")
            }
        }
    }

    fun continueAsMember() {
        selectUserType("member")
    }

    fun selectUserType(userType: String) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            try {
                val response = authApi.selectUserType(SelectUserTypeRequest(userType = userType))
                if (response.isSuccessful && response.body()?.success == true) {
                    val authData = response.body()!!.data!!
                    val route = persistSessionAndResolveRoute(authData, userType)
                    _state.value = AuthUiState.Verified(authData.isNewUser, route)
                } else {
                    _state.value = AuthUiState.Error(extractErrorMessage(response, "We could not update the account type."))
                }
            } catch (e: Exception) {
                _state.value = AuthUiState.Error(e.message ?: "We could not update the account type.")
            }
        }
    }

    private suspend fun completeFirebasePhoneVerification(phone: String, credential: PhoneAuthCredential, userType: String? = null) {
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
                    phone = phone,
                    userType = userType
                )
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val authData = response.body()!!.data!!
                prefs.clearPendingOtpSession()
                val route = persistSessionAndResolveRoute(authData, userType)
                _state.value = AuthUiState.Verified(authData.isNewUser, route)
            } else {
                _state.value = AuthUiState.Error(extractErrorMessage(response, "SoulMatch could not complete phone sign-in."))
            }
        } catch (e: Exception) {
            _state.value = AuthUiState.Error(e.message ?: "Phone verification failed.")
        }
    }

    private suspend fun persistSessionAndResolveRoute(data: AuthData, requestedUserType: String? = null): String {
        val needsRoleSelection = data.requiresRoleSelection || (data.isNewUser && requestedUserType.isNullOrBlank())
        val normalizedUserType = data.userType.ifBlank { requestedUserType ?: "member" }
        if (needsRoleSelection) {
            prefs.clearUserType()
        } else {
            prefs.saveUserType(normalizedUserType)
        }
        prefs.saveAuthToken(data.accessToken)
        prefs.saveRefreshToken(data.refreshToken)
        prefs.saveUserId(data.userId)
        val route = if (needsRoleSelection) {
            "auth_role_selection"
        } else if (normalizedUserType == "agent") {
            val agentProfile = runCatching {
                profileApi.getAgentProfile().body()?.takeIf { it.success }?.data
            }.getOrNull()
            if (data.isNewUser || agentProfile?.advisorId.isNullOrBlank()) {
                "agent_onboarding"
            } else {
                resolveAgentRoute(agentProfile)
            }
        } else if (!data.isNewUser) {
            val profile = runCatching {
                profileApi.getMyProfile().body()?.takeIf { it.success }?.data
            }.getOrNull()
            if (profile?.profileId.isNullOrBlank()) {
                prefs.clearProfileProgress()
            } else {
                prefs.saveProfileId(profile?.profileId.orEmpty())
            }
            prefs.saveWizardStep(resolveWizardStep(profile) ?: 7)
            "dashboard"
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
            if (requestedUserType == "member" && data.isNewUser && profile?.profileId.isNullOrBlank()) {
                "profile_wizard/1"
            } else {
                resolvePostLoginRoute(profile)
            }
        }
        if (needsRoleSelection) {
            prefs.savePendingAuthRoute("auth_role_selection")
        } else {
            prefs.savePendingAuthRoute(route)
        }
        if (route == "profile_wizard/1") {
            prefs.clearProfileProgress()
            prefs.saveWizardStep(1)
        }
        return route
    }

    private fun <T> extractErrorMessage(response: Response<T>, fallback: String): String {
        val body = response.body()
        if (body is com.soulmatch.app.data.models.GenericResponse<*>) {
            body.error?.message?.takeIf { it.isNotBlank() }?.let { return it }
            body.message?.takeIf { it.isNotBlank() }?.let { return it }
        }
        val raw = runCatching { response.errorBody()?.string().orEmpty() }.getOrDefault("")
        if (raw.isNotBlank()) {
            runCatching {
                val json = JSONObject(raw)
                json.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: json.optString("message").takeIf { it.isNotBlank() }
            }.getOrNull()?.let { return it }
        }
        return fallback
    }
}

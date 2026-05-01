package com.soulmatch.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmatch.app.data.api.ProfileApiService
import com.soulmatch.app.data.config.AppEnvironment
import com.soulmatch.app.data.local.UserPreferences
import com.soulmatch.app.data.mock.MarketFixtures
import com.soulmatch.app.data.models.ProfileData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileApi: ProfileApiService,
    private val prefs: UserPreferences
) : ViewModel() {
    private val stepData = mutableMapOf<Int, Map<String, Any>>()
    private val _profile = MutableStateFlow<ProfileData?>(null)
    private val _isSaving = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _loadMessage = MutableStateFlow<String?>(null)
    private var usingMockProfile = false

    val profile: StateFlow<ProfileData?> = _profile.asStateFlow()
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    val loadMessage: StateFlow<String?> = _loadMessage.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _loadMessage.value = null
            try {
                val response = profileApi.getMyProfile()
                val body = response.body()
                if (response.isSuccessful && body?.success == true) {
                    _profile.value = body.data
                    usingMockProfile = false
                } else {
                    if (AppEnvironment.allowDemoFallback) {
                        _profile.value = MarketFixtures.myProfile
                        usingMockProfile = true
                        _loadMessage.value = body?.error?.message ?: "Showing demo profile details because saved details could not be loaded."
                    } else {
                        _profile.value = null
                        usingMockProfile = false
                        _loadMessage.value = body?.error?.message ?: "Saved profile details could not be loaded."
                    }
                }
            } catch (error: Exception) {
                if (AppEnvironment.allowDemoFallback) {
                    _profile.value = MarketFixtures.myProfile
                    usingMockProfile = true
                    _loadMessage.value = when (error) {
                        is IOException -> "Couldn't reach the server. Showing demo profile details for UI testing."
                        else -> "Couldn't load your saved profile details. Showing demo profile details for UI testing."
                    }
                } else {
                    _profile.value = null
                    usingMockProfile = false
                    _loadMessage.value = when (error) {
                        is IOException -> "Couldn't reach the server. Check your connection and try again."
                        else -> "Couldn't load your saved profile details."
                    }
                }
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun updateStep1Data(d: Map<String, Any>) { stepData[1] = d }
    fun updateStep2Data(d: Map<String, Any>) { stepData[2] = d }
    fun updateStep3Data(d: Map<String, Any>) { stepData[3] = d }
    fun updateStep4Data(d: Map<String, Any>) { stepData[4] = d }
    fun updateStep5Data(d: Map<String, Any>) { stepData[5] = d }
    fun updateStep6Data(d: Map<String, Any>) { stepData[6] = d }

    fun saveStep(step: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val payload = stepData[step]
            if (payload.isNullOrEmpty()) {
                _errorMessage.value = "Please complete the required details for this step."
                return@launch
            }
            _isSaving.value = true
            _errorMessage.value = null
            try {
                val req = mutableMapOf<String, Any>("step" to step)
                req.putAll(payload)
                val response = profileApi.createProfileStep(req)
                if (response.isSuccessful && response.body()?.success == true) {
                    usingMockProfile = false
                    response.body()?.data?.profileId?.let { prefs.saveProfileId(it) }
                    prefs.saveWizardStep(step + 1)
                    loadProfile()
                    onSuccess()
                } else if (usingMockProfile && AppEnvironment.allowDemoFallback) {
                    saveMockStep(step, payload)
                    onSuccess()
                } else {
                    _errorMessage.value = response.body()?.error?.message ?: "Could not save this step."
                }
            } catch (e: Exception) {
                if (usingMockProfile && AppEnvironment.allowDemoFallback) {
                    saveMockStep(step, payload)
                    onSuccess()
                } else {
                    _errorMessage.value = profileSaveErrorMessage(e)
                }
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun saveMockStep(step: Int, payload: Map<String, Any>) {
        MarketFixtures.updateMyProfileStep(step, payload)
        _profile.value = MarketFixtures.myProfile
        _loadMessage.value = "Saved locally in demo mode. Start the backend to persist this profile."
    }

    private fun profileSaveErrorMessage(error: Throwable): String {
        val raw = error.message.orEmpty()
        return when {
            raw.contains("type variable or wildcard", ignoreCase = true) ->
                "This build hit a profile form bug while saving. Please try again after updating the app."
            error is IOException ->
                "Couldn't reach the server. Check your connection and try again."
            raw.isNotBlank() ->
                "Couldn't save your profile right now. Please try again."
            else ->
                "Couldn't save your profile right now. Please try again."
        }
    }
}

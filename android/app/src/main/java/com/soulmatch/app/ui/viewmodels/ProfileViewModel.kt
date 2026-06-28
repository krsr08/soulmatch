package com.soulmatch.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmatch.app.data.api.ProfileApiService
import com.soulmatch.app.data.config.AppEnvironment
import com.soulmatch.app.data.local.UserPreferences
import com.soulmatch.app.data.mock.MarketFixtures
import com.soulmatch.app.data.models.AiBioSuggestionRequest
import com.soulmatch.app.data.models.PartnerPreferencesData
import com.soulmatch.app.data.models.ProfileData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val _isGeneratingBioSuggestions = MutableStateFlow(false)
    private val _bioSuggestions = MutableStateFlow<List<String>>(emptyList())
    private val _partnerPreferences = MutableStateFlow(PartnerPreferencesData())
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _loadMessage = MutableStateFlow<String?>(null)
    private var usingMockProfile = false

    val profile: StateFlow<ProfileData?> = _profile.asStateFlow()
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()
    val isGeneratingBioSuggestions: StateFlow<Boolean> = _isGeneratingBioSuggestions.asStateFlow()
    val bioSuggestions: StateFlow<List<String>> = _bioSuggestions.asStateFlow()
    val partnerPreferences: StateFlow<PartnerPreferencesData> = _partnerPreferences.asStateFlow()
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    val loadMessage: StateFlow<String?> = _loadMessage.asStateFlow()

    init {
        loadProfile()
    }

    private suspend fun canUseDemoFallback(): Boolean =
        AppEnvironment.allowDemoFallback && prefs.authToken.first().isNullOrBlank()

    fun loadProfile() {
        viewModelScope.launch {
            _loadMessage.value = null
            val canUseFallback = canUseDemoFallback()
            try {
                val response = profileApi.getMyProfile()
                val body = response.body()
                if (response.isSuccessful && body?.success == true) {
                    _profile.value = body.data
                    val profileId = body.data?.profileId.orEmpty()
                    if (profileId.isNotBlank()) {
                        val prefResponse = runCatching { profileApi.getPreferences(profileId) }.getOrNull()
                        _partnerPreferences.value = prefResponse?.body()?.takeIf { it.success }?.data
                            ?: PartnerPreferencesData(
                                religion = body.data?.religion,
                                educationLevels = listOf(body.data?.educationLevel.orEmpty()).filter { it.isNotBlank() },
                                occupations = listOf(body.data?.occupation.orEmpty()).filter { it.isNotBlank() },
                                locations = listOf(body.data?.workingCity.orEmpty(), body.data?.familyCity.orEmpty()).filter { it.isNotBlank() }.distinct(),
                                dietPrefs = listOf(body.data?.diet.orEmpty()).filter { it.isNotBlank() }
                            )
                    } else {
                        _partnerPreferences.value = PartnerPreferencesData()
                    }
                    usingMockProfile = false
                } else {
                    if (canUseFallback) {
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
                if (canUseFallback) {
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

    fun updatePartnerPreferences(preferences: PartnerPreferencesData) {
        _partnerPreferences.value = preferences.safePreferences()
    }

    fun requestBioSuggestions(currentBio: String) {
        val profileId = _profile.value?.profileId.orEmpty()
        if (profileId.isBlank()) {
            _bioSuggestions.value = localBioSuggestions(currentBio)
            return
        }
        viewModelScope.launch {
            _isGeneratingBioSuggestions.value = true
            _errorMessage.value = null
            val fallback = localBioSuggestions(currentBio)
            try {
                val response = profileApi.getBioSuggestions(profileId, AiBioSuggestionRequest(currentBio.trim()))
                val body = response.body()
                _bioSuggestions.value = if (response.isSuccessful && body?.success == true) {
                    body.data?.suggestions?.takeIf { it.isNotEmpty() } ?: fallback
                } else {
                    fallback
                }
            } catch (_: Exception) {
                _bioSuggestions.value = fallback
            } finally {
                _isGeneratingBioSuggestions.value = false
            }
        }
    }

    fun clearBioSuggestions() {
        _bioSuggestions.value = emptyList()
    }

    fun saveStep(step: Int, onSuccess: () -> Unit) {
        if (step == 6) {
            savePartnerPreferences(onSuccess)
            return
        }
        viewModelScope.launch {
            val payload = stepData[step]
            if (payload.isNullOrEmpty()) {
                _errorMessage.value = "Please complete the required details for this step."
                return@launch
            }
            _isSaving.value = true
            _errorMessage.value = null
            val canUseFallback = canUseDemoFallback()
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
                } else if (usingMockProfile && canUseFallback) {
                    saveMockStep(step, payload)
                    onSuccess()
                } else {
                    _errorMessage.value = response.body()?.error?.message ?: "Could not save this step."
                }
            } catch (e: Exception) {
                if (usingMockProfile && canUseFallback) {
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

    private fun savePartnerPreferences(onSuccess: () -> Unit) {
        val profileId = _profile.value?.profileId.orEmpty()
        if (profileId.isBlank()) {
            _errorMessage.value = "Complete the earlier steps first."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null
            val request = _partnerPreferences.value.safePreferences()
            try {
                val response = profileApi.updatePreferences(profileId, request)
                if (response.isSuccessful && response.body()?.success == true) {
                    _profile.value = _profile.value?.copy(isPartnerPrefSet = true)
                    prefs.saveWizardStep(7)
                    loadProfile()
                    onSuccess()
                } else {
                    _errorMessage.value = response.body()?.error?.message ?: "Could not save partner preferences."
                }
            } catch (error: Exception) {
                _errorMessage.value = profileSaveErrorMessage(error)
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun localBioSuggestions(currentBio: String): List<String> {
        val profile = _profile.value
        val name = listOfNotNull(profile?.firstName, profile?.lastName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "I" }
        val base = currentBio.trim().ifBlank {
            "$name value family, respectful communication, and a meaningful partnership built on trust."
        }
        return listOf(
            "$base I am looking for a mature partner who values family, kindness, and shared growth through every stage of life.",
            "$name comes from a grounded family and believes marriage should bring respect, emotional support, and warmth to both families.",
            "I am serious about finding a compatible life partner and hope to build a home shaped by trust, stability, and thoughtful family bonds."
        )
    }

    private fun PartnerPreferencesData.safePreferences(): PartnerPreferencesData = copy(
        religion = religion?.trim()?.ifBlank { null },
        manglikPref = manglikPref.trim().ifBlank { "any" },
        educationLevels = educationLevels.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
        occupations = occupations.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
        locations = locations.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
        dietPrefs = dietPrefs.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
        maritalStatuses = maritalStatuses.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
        familyTypes = familyTypes.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
        timeline = timeline?.trim()?.ifBlank { null },
        dealBreakers = dealBreakers.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
        goodToHave = goodToHave.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    )
}

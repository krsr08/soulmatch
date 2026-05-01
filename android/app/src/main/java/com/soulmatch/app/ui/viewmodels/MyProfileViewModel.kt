package com.soulmatch.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmatch.app.data.api.PaymentApiService
import com.soulmatch.app.data.api.ProfileApiService
import com.soulmatch.app.data.config.AppEnvironment
import com.soulmatch.app.data.mock.MarketFixtures
import com.soulmatch.app.data.models.PartnerPreferencesData
import com.soulmatch.app.data.models.ProfilePhoto
import com.soulmatch.app.data.models.ProfileData
import com.soulmatch.app.data.models.SubscriptionData
import com.soulmatch.app.data.models.ViewerData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import java.io.IOException

data class ProfileChecklistItem(
    val title: String,
    val description: String,
    val isComplete: Boolean,
    val editStep: Int,
    val statusLabel: String = if (isComplete) "Complete" else "Needs attention"
)

@HiltViewModel
class MyProfileViewModel @Inject constructor(
    private val profileApi: ProfileApiService,
    private val paymentApi: PaymentApiService
) : ViewModel() {
    private val _profile = MutableStateFlow<ProfileData?>(null)
    private val _subscription = MutableStateFlow(SubscriptionData(planId = "free", isActive = false))
    private val _checklist = MutableStateFlow<List<ProfileChecklistItem>>(emptyList())
    private val _preferences = MutableStateFlow(PartnerPreferencesData())
    private val _viewers = MutableStateFlow<List<ViewerData>>(emptyList())
    private val _photos = MutableStateFlow<List<ProfilePhoto>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _isUploadingPhotos = MutableStateFlow(false)
    private val _status = MutableStateFlow<String?>(null)
    private val _loadMessage = MutableStateFlow<String?>(null)

    val profile: StateFlow<ProfileData?> = _profile.asStateFlow()
    val subscription: StateFlow<SubscriptionData> = _subscription.asStateFlow()
    val checklist: StateFlow<List<ProfileChecklistItem>> = _checklist.asStateFlow()
    val preferences: StateFlow<PartnerPreferencesData> = _preferences.asStateFlow()
    val viewers: StateFlow<List<ViewerData>> = _viewers.asStateFlow()
    val photos: StateFlow<List<ProfilePhoto>> = _photos.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val isUploadingPhotos: StateFlow<Boolean> = _isUploadingPhotos.asStateFlow()
    val status: StateFlow<String?> = _status.asStateFlow()
    val loadMessage: StateFlow<String?> = _loadMessage.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadMessage.value = null
            try {
                val profileResponse = profileApi.getMyProfile()
                val profileBody = profileResponse.body()
                if (!profileResponse.isSuccessful || profileBody?.success != true) {
                    if (AppEnvironment.allowDemoFallback) {
                        applyMockProfileFallback(profileBody?.error?.message ?: "Showing demo profile data because your saved profile could not be loaded.")
                    } else {
                        _profile.value = null
                        _checklist.value = buildChecklist(null)
                        _loadMessage.value = profileBody?.error?.message ?: "Your saved profile could not be loaded."
                    }
                    return@launch
                }

                val resolvedProfile = profileBody.data
                _profile.value = resolvedProfile
                _checklist.value = buildChecklist(resolvedProfile)

                if (resolvedProfile == null || resolvedProfile.profileId.isBlank()) {
                    _subscription.value = SubscriptionData(planId = "free", isActive = false)
                    _preferences.value = PartnerPreferencesData()
                    _viewers.value = emptyList()
                    _photos.value = emptyList()
                    _loadMessage.value = "Start with your basic details to build your profile."
                    return@launch
                }

                _subscription.value = runCatching { paymentApi.getSubscription() }
                    .getOrNull()
                    ?.body()
                    ?.takeIf { it.success }
                    ?.data
                    ?: if (AppEnvironment.allowDemoFallback) MarketFixtures.currentSubscription else SubscriptionData(planId = "free", isActive = false)
                _preferences.value = runCatching { profileApi.getPreferences(resolvedProfile.profileId) }
                    .getOrNull()
                    ?.body()
                    ?.takeIf { it.success }
                    ?.data
                    ?: PartnerPreferencesData(religion = resolvedProfile.religion, manglikPref = "any")
                _viewers.value = runCatching { profileApi.getViewers(resolvedProfile.profileId) }
                    .getOrNull()
                    ?.body()
                    ?.takeIf { it.success }
                    ?.data
                    .orEmpty()
                    .ifEmpty { if (AppEnvironment.allowDemoFallback) MarketFixtures.recentViewers else emptyList() }
                _photos.value = runCatching { profileApi.getPhotos(resolvedProfile.profileId) }
                    .getOrNull()
                    ?.body()
                    ?.takeIf { it.success }
                    ?.data
                    .orEmpty()
                    .ifEmpty { if (AppEnvironment.allowDemoFallback) MarketFixtures.profilePhotos else emptyList() }
            } catch (error: Exception) {
                if (AppEnvironment.allowDemoFallback) {
                    applyMockProfileFallback(
                        when (error) {
                            is IOException -> "Couldn't reach the server to load your profile. Showing demo profile data for UI testing."
                            else -> "Couldn't load your saved profile right now. Showing demo profile data for UI testing."
                        }
                    )
                } else {
                    _profile.value = null
                    _checklist.value = buildChecklist(null)
                    _loadMessage.value = when (error) {
                        is IOException -> "Couldn't reach the server to load your profile."
                        else -> "Couldn't load your saved profile right now."
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun applyMockProfileFallback(message: String) {
        if (!AppEnvironment.allowDemoFallback) return
        val fallback = MarketFixtures.myProfile
        _profile.value = fallback
        _subscription.value = MarketFixtures.currentSubscription
        _preferences.value = PartnerPreferencesData(religion = fallback.religion, manglikPref = "any")
        _viewers.value = MarketFixtures.recentViewers
        _photos.value = MarketFixtures.profilePhotos
        _checklist.value = buildChecklist(fallback)
        _loadMessage.value = message
    }

    fun uploadPhotos(parts: List<MultipartBody.Part>) {
        val profileId = _profile.value?.profileId ?: return
        if (parts.isEmpty()) return
        viewModelScope.launch {
            _isUploadingPhotos.value = true
            _status.value = null
            try {
                val response = profileApi.uploadPhotos(profileId, parts)
                if (response.isSuccessful && response.body()?.success == true) {
                    _status.value = if (parts.size == 1) "Photo uploaded." else "${parts.size} photos uploaded."
                    load()
                } else {
                    _status.value = response.body()?.error?.message ?: "Couldn't upload photos right now."
                }
            } catch (error: Exception) {
                _status.value = when (error) {
                    is IOException -> "Couldn't reach the server. Check your connection and try again."
                    else -> "Couldn't upload photos right now. Please try again."
                }
            } finally {
                _isUploadingPhotos.value = false
            }
        }
    }

    fun setPrimaryPhoto(photoId: String) {
        val profileId = _profile.value?.profileId ?: return
        viewModelScope.launch {
            try {
                val response = profileApi.setPrimaryPhoto(profileId, photoId)
                if (response.isSuccessful && response.body()?.success == true) {
                    _status.value = "Primary photo updated."
                    load()
                } else {
                    _status.value = response.body()?.error?.message ?: "Couldn't update the primary photo."
                }
            } catch (error: Exception) {
                _status.value = when (error) {
                    is IOException -> "Couldn't reach the server. Check your connection and try again."
                    else -> "Couldn't update the primary photo right now."
                }
            }
        }
    }

    fun deletePhoto(photoId: String) {
        val profileId = _profile.value?.profileId ?: return
        viewModelScope.launch {
            try {
                val response = profileApi.deletePhoto(profileId, photoId)
                if (response.isSuccessful && response.body()?.success == true) {
                    _status.value = "Photo removed."
                    load()
                } else {
                    _status.value = response.body()?.error?.message ?: "Couldn't remove this photo."
                }
            } catch (error: Exception) {
                _status.value = when (error) {
                    is IOException -> "Couldn't reach the server. Check your connection and try again."
                    else -> "Couldn't remove this photo right now."
                }
            }
        }
    }

    fun updatePartnerPreferences(ageMin: Int, ageMax: Int, religion: String, manglikPref: String) {
        val profileId = _profile.value?.profileId ?: return
        viewModelScope.launch {
            val request = PartnerPreferencesData(
                ageMin = ageMin,
                ageMax = ageMax,
                religion = religion.ifBlank { null },
                manglikPref = manglikPref
            )
            _isLoading.value = true
            _status.value = null
            try {
                val response = profileApi.updatePreferences(profileId, request)
                if (response.isSuccessful && response.body()?.success == true) {
                    _preferences.value = request
                    _status.value = "Partner preferences updated."
                } else {
                    _status.value = response.body()?.error?.message ?: "Couldn't update partner preferences right now."
                }
            } catch (error: Exception) {
                _status.value = when (error) {
                    is IOException -> "Couldn't reach the server. Check your connection and try again."
                    else -> "Couldn't update partner preferences right now. Please try again."
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearStatus() {
        _status.value = null
    }

    private fun buildChecklist(profile: ProfileData?): List<ProfileChecklistItem> {
        val resolved = profile ?: ProfileData()
        val hasHoroscopeDetails = resolved.rashi.isNotBlank() ||
            resolved.nakshatra.isNotBlank() ||
            resolved.birthCity.isNotBlank() ||
            resolved.gotra.isNotBlank() ||
            resolved.isManglik

        return listOf(
            ProfileChecklistItem(
                title = "Basic details",
                description = "Name, DOB, gender, religion, community, language, and marital status",
                isComplete = resolved.firstName.isNotBlank() &&
                    resolved.lastName.isNotBlank() &&
                    !resolved.dob.isNullOrBlank() &&
                    resolved.gender.isNotBlank() &&
                    resolved.religion.isNotBlank() &&
                    resolved.caste.isNotBlank() &&
                    resolved.maritalStatus.isNotBlank() &&
                    resolved.motherTongue.isNotBlank(),
                editStep = 1
            ),
            ProfileChecklistItem(
                title = "Physical details",
                description = "Height, weight, complexion, body type, and blood group",
                isComplete = (resolved.heightCm ?: 0) > 0 &&
                    (resolved.weightKg ?: 0) > 0 &&
                    resolved.complexion.isNotBlank() &&
                    resolved.bodyType.isNotBlank() &&
                    resolved.bloodGroup.isNotBlank(),
                editStep = 2
            ),
            ProfileChecklistItem(
                title = "Work and education",
                description = "Education level, occupation, annual income, and working city",
                isComplete = resolved.educationLevel.isNotBlank() &&
                    resolved.occupation.isNotBlank() &&
                    resolved.annualIncome.isNotBlank() &&
                    resolved.workingCity.isNotBlank(),
                editStep = 3
            ),
            ProfileChecklistItem(
                title = "Family details",
                description = "Parent occupations, siblings, family type, and family city",
                isComplete = resolved.fatherOccupation.isNotBlank() &&
                    resolved.motherOccupation.isNotBlank() &&
                    resolved.numBrothers != null &&
                    resolved.numSisters != null &&
                    resolved.familyType.isNotBlank() &&
                    resolved.familyCity.isNotBlank(),
                editStep = 4
            ),
            ProfileChecklistItem(
                title = "Lifestyle",
                description = "Diet, smoking, drinking, and an about section with at least 30 characters",
                isComplete = resolved.diet.isNotBlank() &&
                    resolved.smoking.isNotBlank() &&
                    resolved.drinking.isNotBlank() &&
                    resolved.aboutMe.trim().length >= 30,
                editStep = 5
            ),
            ProfileChecklistItem(
                title = "Horoscope",
                description = "Optional: add rashi, nakshatra, birth city, or gotra if relevant",
                isComplete = hasHoroscopeDetails,
                editStep = 6,
                statusLabel = if (hasHoroscopeDetails) "Complete" else "Optional"
            )
        )
    }
}

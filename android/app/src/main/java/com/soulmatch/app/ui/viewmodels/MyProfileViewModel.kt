package com.soulmatch.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmatch.app.data.api.PaymentApiService
import com.soulmatch.app.data.api.ProfileApiService
import com.soulmatch.app.data.config.AppEnvironment
import com.soulmatch.app.data.local.UserPreferences
import com.soulmatch.app.data.mock.MarketFixtures
import com.soulmatch.app.data.models.PartnerPreferencesData
import com.soulmatch.app.data.models.PhotoAccessActionRequest
import com.soulmatch.app.data.models.PhotoAccessRequestData
import com.soulmatch.app.data.models.AssistStatusData
import com.soulmatch.app.data.models.AssistStatusRequest
import com.soulmatch.app.data.models.FamilyDecisionData
import com.soulmatch.app.data.models.PrivacySettingsRequest
import com.soulmatch.app.data.models.ProfilePhoto
import com.soulmatch.app.data.models.ProfileData
import com.soulmatch.app.data.models.SubscriptionData
import com.soulmatch.app.data.models.VerificationRequestData
import com.soulmatch.app.data.models.VerificationSubmitRequest
import com.soulmatch.app.data.models.ViewerData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val paymentApi: PaymentApiService,
    private val prefs: UserPreferences
) : ViewModel() {
    private val _profile = MutableStateFlow<ProfileData?>(null)
    private val _subscription = MutableStateFlow(SubscriptionData(planId = "free", isActive = false))
    private val _checklist = MutableStateFlow<List<ProfileChecklistItem>>(emptyList())
    private val _preferences = MutableStateFlow(PartnerPreferencesData())
    private val _assistStatus = MutableStateFlow(AssistStatusData())
    private val _viewers = MutableStateFlow<List<ViewerData>>(emptyList())
    private val _photos = MutableStateFlow<List<ProfilePhoto>>(emptyList())
    private val _verifications = MutableStateFlow<List<VerificationRequestData>>(emptyList())
    private val _photoAccessRequests = MutableStateFlow<List<PhotoAccessRequestData>>(emptyList())
    private val _familyDecisions = MutableStateFlow<List<FamilyDecisionData>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _isUploadingPhotos = MutableStateFlow(false)
    private val _isSubmittingVerification = MutableStateFlow(false)
    private val _status = MutableStateFlow<String?>(null)
    private val _loadMessage = MutableStateFlow<String?>(null)

    val profile: StateFlow<ProfileData?> = _profile.asStateFlow()
    val subscription: StateFlow<SubscriptionData> = _subscription.asStateFlow()
    val checklist: StateFlow<List<ProfileChecklistItem>> = _checklist.asStateFlow()
    val preferences: StateFlow<PartnerPreferencesData> = _preferences.asStateFlow()
    val assistStatus: StateFlow<AssistStatusData> = _assistStatus.asStateFlow()
    val viewers: StateFlow<List<ViewerData>> = _viewers.asStateFlow()
    val photos: StateFlow<List<ProfilePhoto>> = _photos.asStateFlow()
    val verifications: StateFlow<List<VerificationRequestData>> = _verifications.asStateFlow()
    val photoAccessRequests: StateFlow<List<PhotoAccessRequestData>> = _photoAccessRequests.asStateFlow()
    val familyDecisions: StateFlow<List<FamilyDecisionData>> = _familyDecisions.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val isUploadingPhotos: StateFlow<Boolean> = _isUploadingPhotos.asStateFlow()
    val isSubmittingVerification: StateFlow<Boolean> = _isSubmittingVerification.asStateFlow()
    val status: StateFlow<String?> = _status.asStateFlow()
    val loadMessage: StateFlow<String?> = _loadMessage.asStateFlow()

    init {
        load()
    }

    private suspend fun canUseDemoFallback(): Boolean =
        AppEnvironment.allowDemoFallback && prefs.authToken.first().isNullOrBlank()

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadMessage.value = null
            val canUseFallback = canUseDemoFallback()
            try {
                val profileResponse = profileApi.getMyProfile()
                val profileBody = profileResponse.body()
                if (!profileResponse.isSuccessful || profileBody?.success != true) {
                    if (canUseFallback) {
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
                    _assistStatus.value = AssistStatusData()
                    _viewers.value = emptyList()
                    _photos.value = emptyList()
                    _verifications.value = emptyList()
                    _photoAccessRequests.value = emptyList()
                    _familyDecisions.value = emptyList()
                    _loadMessage.value = "Start with your basic details to build your profile."
                    return@launch
                }

                _subscription.value = runCatching { paymentApi.getSubscription() }
                    .getOrNull()
                    ?.body()
                    ?.takeIf { it.success }
                    ?.data
                    ?: if (canUseFallback) MarketFixtures.currentSubscription else SubscriptionData(planId = "free", isActive = false)
                _preferences.value = runCatching { profileApi.getPreferences(resolvedProfile.profileId) }
                    .getOrNull()
                    ?.body()
                    ?.takeIf { it.success }
                    ?.data
                    ?: PartnerPreferencesData(
                        religion = resolvedProfile.religion,
                        manglikPref = "any",
                        educationLevels = listOf(resolvedProfile.educationLevel).filter { it.isNotBlank() },
                        occupations = listOf(resolvedProfile.occupation).filter { it.isNotBlank() },
                        heightMinCm = resolvedProfile.heightCm?.minus(10),
                        heightMaxCm = resolvedProfile.heightCm?.plus(10),
                        locations = listOf(resolvedProfile.workingCity, resolvedProfile.familyCity).filter { it.isNotBlank() }.distinct(),
                        dietPrefs = listOf(resolvedProfile.diet).filter { it.isNotBlank() },
                        maritalStatuses = listOf(resolvedProfile.maritalStatus).filter { it.isNotBlank() },
                        familyTypes = listOf(resolvedProfile.familyType).filter { it.isNotBlank() }
                    )
                _assistStatus.value = runCatching { profileApi.getAssistStatus() }
                    .getOrNull()
                    ?.body()
                    ?.takeIf { it.success }
                    ?.data
                    ?: AssistStatusData(
                        profileId = resolvedProfile.profileId,
                        location = com.soulmatch.app.data.models.AssistLocationData(
                            city = resolvedProfile.familyCity,
                            state = resolvedProfile.familyState,
                            locality = resolvedProfile.familyLocality,
                            pincode = resolvedProfile.familyPincode
                        )
                    )
                _viewers.value = runCatching { profileApi.getViewers(resolvedProfile.profileId) }
                    .getOrNull()
                    ?.body()
                    ?.takeIf { it.success }
                    ?.data
                    .orEmpty()
                    .ifEmpty { if (canUseFallback) MarketFixtures.recentViewers else emptyList() }
                _photos.value = runCatching { profileApi.getPhotos(resolvedProfile.profileId) }
                    .getOrNull()
                    ?.body()
                    ?.takeIf { it.success }
                    ?.data
                    .orEmpty()
                    .ifEmpty { if (canUseFallback) MarketFixtures.profilePhotos else emptyList() }
                _verifications.value = runCatching { profileApi.getVerifications(resolvedProfile.profileId) }
                    .getOrNull()
                    ?.body()
                    ?.takeIf { it.success }
                    ?.data
                    .orEmpty()
                _photoAccessRequests.value = runCatching { profileApi.getPhotoAccessRequests() }
                    .getOrNull()
                    ?.body()
                    ?.takeIf { it.success }
                    ?.data
                    .orEmpty()
                _familyDecisions.value = runCatching { profileApi.getFamilyDecisions() }
                    .getOrNull()
                    ?.body()
                    ?.takeIf { it.success }
                    ?.data
                    .orEmpty()
            } catch (error: Exception) {
                if (canUseFallback) {
                    applyMockProfileFallback(
                        when (error) {
                            is IOException -> "Couldn't reach the server to load your profile. Showing demo profile data for UI testing."
                            else -> "Couldn't load your saved profile right now. Showing demo profile data for UI testing."
                        }
                    )
                } else {
                    _profile.value = null
                    _checklist.value = buildChecklist(null)
                    _verifications.value = emptyList()
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
        _preferences.value = PartnerPreferencesData(
            religion = fallback.religion,
            manglikPref = "any",
            educationLevels = listOf(fallback.educationLevel).filter { it.isNotBlank() },
            occupations = listOf(fallback.occupation).filter { it.isNotBlank() },
            locations = listOf(fallback.workingCity, fallback.familyCity).filter { it.isNotBlank() }.distinct(),
            dietPrefs = listOf(fallback.diet).filter { it.isNotBlank() },
            maritalStatuses = listOf(fallback.maritalStatus).filter { it.isNotBlank() },
            familyTypes = listOf(fallback.familyType).filter { it.isNotBlank() }
        )
        _assistStatus.value = AssistStatusData(
            profileId = fallback.profileId,
            location = com.soulmatch.app.data.models.AssistLocationData(
                city = fallback.familyCity,
                state = fallback.familyState,
                locality = fallback.familyLocality,
                pincode = fallback.familyPincode
            )
        )
        _viewers.value = MarketFixtures.recentViewers
        _photos.value = MarketFixtures.profilePhotos
        _verifications.value = emptyList()
        _photoAccessRequests.value = emptyList()
        _familyDecisions.value = emptyList()
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

    fun updatePartnerPreferences(request: PartnerPreferencesData) {
        val profileId = _profile.value?.profileId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _status.value = null
            try {
                val response = profileApi.updatePreferences(profileId, request)
                if (response.isSuccessful && response.body()?.success == true) {
                    _preferences.value = request
                    _profile.value = _profile.value?.copy(isPartnerPrefSet = true)
                    _status.value = "Partner preferences updated."
                    load()
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

    fun updatePrivacySettings(photoPrivacy: String, profileVisibility: String, hideLastSeen: Boolean) {
        val current = _profile.value ?: return
        val profileId = current.profileId
        if (profileId.isBlank()) return
        _profile.value = current.copy(
            photoPrivacy = photoPrivacy,
            profileVisibility = profileVisibility,
            hideLastSeen = hideLastSeen
        )
        viewModelScope.launch {
            _status.value = null
            try {
                val response = profileApi.updatePrivacy(
                    profileId,
                    PrivacySettingsRequest(
                        photoPrivacy = photoPrivacy,
                        profileVisibility = profileVisibility,
                        hideLastSeen = hideLastSeen
                    )
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    _status.value = "Privacy settings updated."
                } else {
                    _profile.value = current
                    _status.value = response.body()?.error?.message ?: "Couldn't update privacy settings right now."
                }
            } catch (error: Exception) {
                _profile.value = current
                _status.value = when (error) {
                    is IOException -> "Couldn't reach the server. Check your connection and try again."
                    else -> "Couldn't update privacy settings right now. Please try again."
                }
            }
        }
    }

    fun updateAssistStatus(
        isOptedIn: Boolean,
        supportLevel: String,
        preferredContactWindow: String,
        familyContactName: String,
        familyContactPhone: String,
        notes: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _status.value = null
            try {
                val response = profileApi.updateAssistStatus(
                    AssistStatusRequest(
                        isOptedIn = isOptedIn,
                        supportLevel = supportLevel,
                        preferredContactWindow = preferredContactWindow,
                        familyContactName = familyContactName,
                        familyContactPhone = familyContactPhone,
                        notes = notes
                    )
                )
                val body = response.body()
                if (response.isSuccessful && body?.success == true && body.data != null) {
                    _assistStatus.value = body.data
                    _status.value = body.message ?: "SoulMatch Assist updated."
                    load()
                } else {
                    _status.value = body?.error?.message ?: "Couldn't update SoulMatch Assist right now."
                }
            } catch (error: Exception) {
                _status.value = when (error) {
                    is IOException -> "Couldn't reach the server. Check your connection and try again."
                    else -> "Couldn't update SoulMatch Assist right now. Please try again."
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitProfileVerification() {
        val profileId = _profile.value?.profileId ?: return
        viewModelScope.launch {
            _isSubmittingVerification.value = true
            _status.value = null
            try {
                val response = profileApi.submitVerification(profileId, VerificationSubmitRequest(type = "profile"))
                val body = response.body()
                if (response.isSuccessful && body?.success == true) {
                    body.data?.let { verification ->
                        _verifications.value = listOf(verification) + _verifications.value.filterNot { it.verificationId == verification.verificationId }
                    }
                    _status.value = body.message ?: "Verification request submitted."
                    load()
                } else {
                    _status.value = body?.error?.message ?: "Couldn't submit verification right now."
                }
            } catch (error: Exception) {
                _status.value = when (error) {
                    is IOException -> "Couldn't reach the server. Check your connection and try again."
                    else -> "Couldn't submit verification right now. Please try again."
                }
            } finally {
                _isSubmittingVerification.value = false
            }
        }
    }

    fun respondPhotoAccessRequest(requestId: String, approved: Boolean) {
        viewModelScope.launch {
            val nextStatus = if (approved) "approved" else "declined"
            try {
                val response = profileApi.respondPhotoAccessRequest(requestId, PhotoAccessActionRequest(nextStatus))
                if (response.isSuccessful && response.body()?.success == true) {
                    _photoAccessRequests.value = _photoAccessRequests.value.map { request ->
                        if (request.requestId == requestId) request.copy(status = nextStatus) else request
                    }
                    _status.value = if (approved) "Photo access approved." else "Photo access declined."
                } else {
                    _status.value = response.body()?.error?.message ?: "Couldn't update photo access right now."
                }
            } catch (error: Exception) {
                _status.value = when (error) {
                    is IOException -> "Couldn't reach the server. Check your connection and try again."
                    else -> "Couldn't update photo access right now."
                }
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
                    (!resolved.isEmployed || (
                        resolved.occupation.isNotBlank() &&
                            resolved.annualIncome.isNotBlank() &&
                            resolved.workingCity.isNotBlank() &&
                            resolved.workingState.isNotBlank() &&
                            resolved.workingPincode.length == 6
                        )),
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

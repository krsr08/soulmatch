package com.soulmatch.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmatch.app.data.api.PaymentApiService
import com.soulmatch.app.data.api.ProfileApiService
import com.soulmatch.app.data.local.UserPreferences
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
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
    private val _isSavingAssist = MutableStateFlow(false)
    private val _photoUploadProgress = MutableStateFlow(0)
    private val _photoUploadLabel = MutableStateFlow<String?>(null)
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
    val isSavingAssist: StateFlow<Boolean> = _isSavingAssist.asStateFlow()
    val photoUploadProgress: StateFlow<Int> = _photoUploadProgress.asStateFlow()
    val photoUploadLabel: StateFlow<String?> = _photoUploadLabel.asStateFlow()
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
                    if (_profile.value == null) {
                        _checklist.value = buildChecklist(null)
                    }
                    _loadMessage.value = profileBody?.error?.message ?: "Your saved profile could not be loaded."
                    return@launch
                }

                val resolvedProfile = profileBody.data?.safeProfileData()
                _profile.value = resolvedProfile
                _checklist.value = buildChecklist(resolvedProfile)
                if (!resolvedProfile?.profileId.isNullOrBlank()) {
                    prefs.saveProfileId(resolvedProfile?.profileId.orEmpty())
                }

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

                coroutineScope {
                    val subscriptionCall = async { runCatching { paymentApi.getSubscription() }.getOrNull() }
                    val preferencesCall = async { runCatching { profileApi.getPreferences(resolvedProfile.profileId) }.getOrNull() }
                    val assistCall = async { runCatching { profileApi.getAssistStatus() }.getOrNull() }
                    val viewersCall = async { runCatching { profileApi.getViewers(resolvedProfile.profileId) }.getOrNull() }
                    val photosCall = async { runCatching { profileApi.getPhotos(resolvedProfile.profileId) }.getOrNull() }
                    val verificationsCall = async { runCatching { profileApi.getVerifications(resolvedProfile.profileId) }.getOrNull() }
                    val photoAccessCall = async { runCatching { profileApi.getPhotoAccessRequests() }.getOrNull() }
                    val familyDecisionsCall = async { runCatching { profileApi.getFamilyDecisions() }.getOrNull() }

                    _subscription.value = subscriptionCall.await()
                        ?.body()
                        ?.takeIf { it.success }
                        ?.data
                        ?: SubscriptionData(planId = "free", isActive = false)
                    _preferences.value = (preferencesCall.await()
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
                        )).safePreferences()
                    _assistStatus.value = assistCall.await()
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
                    _viewers.value = viewersCall.await()?.body()?.takeIf { it.success }?.data.orEmpty()
                        .ifEmpty { emptyList() }
                    _photos.value = photosCall.await()?.body()?.takeIf { it.success }?.data.orEmpty()
                        .ifEmpty { emptyList() }
                    _verifications.value = verificationsCall.await()?.body()?.takeIf { it.success }?.data.orEmpty()
                    _photoAccessRequests.value = photoAccessCall.await()?.body()?.takeIf { it.success }?.data.orEmpty()
                    _familyDecisions.value = familyDecisionsCall.await()?.body()?.takeIf { it.success }?.data.orEmpty()
                }
            } catch (error: Exception) {
                if (_profile.value == null) {
                    _checklist.value = buildChecklist(null)
                    _verifications.value = emptyList()
                }
                _loadMessage.value = when (error) {
                    is IOException -> "Couldn't reach the server to refresh your profile. Showing the last saved details on this device."
                    else -> "Couldn't refresh your saved profile right now. Showing the last saved details on this device."
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadPhotos(parts: List<MultipartBody.Part>) {
        val profileId = _profile.value?.profileId ?: return
        if (parts.isEmpty()) return
        viewModelScope.launch {
            _isUploadingPhotos.value = true
            _photoUploadProgress.value = 0
            val targetSlot = (_photos.value.size + 1).coerceAtLeast(1)
            _photoUploadLabel.value = "Uploading gallery photo $targetSlot - 0%"
            _status.value = null
            val progressJob = launch {
                while (isActive && _photoUploadProgress.value < 92) {
                    delay(120)
                    val next = (_photoUploadProgress.value + 6).coerceAtMost(92)
                    _photoUploadProgress.value = next
                    _photoUploadLabel.value = "Uploading gallery photo $targetSlot - $next%"
                }
            }
            try {
                val response = profileApi.uploadPhotos(profileId, parts)
                progressJob.cancel()
                if (response.isSuccessful && response.body()?.success == true) {
                    _photoUploadProgress.value = 100
                    _photoUploadLabel.value = "Uploading gallery photo $targetSlot - 100%"
                    _status.value = if (parts.size == 1) "Photo uploaded." else "${parts.size} photos uploaded."
                    load()
                } else {
                    _status.value = response.body()?.error?.message ?: "Couldn't upload photos right now."
                }
            } catch (error: Exception) {
                progressJob.cancel()
                _status.value = when (error) {
                    is IOException -> "Service is temporarily not available. Please try again."
                    else -> "Couldn't upload photos right now. Please try again."
                }
            } finally {
                _isUploadingPhotos.value = false
                if (_photoUploadProgress.value >= 100) {
                    delay(300)
                }
                _photoUploadProgress.value = 0
                _photoUploadLabel.value = null
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
                    is IOException -> "Service is temporarily not available. Please try again."
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
                    is IOException -> "Service is temporarily not available. Please try again."
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
            val sanitizedRequest = request.safePreferences()
            try {
                val response = profileApi.updatePreferences(profileId, sanitizedRequest)
                if (response.isSuccessful && response.body()?.success == true) {
                    _preferences.value = sanitizedRequest
                    _profile.value = _profile.value?.copy(isPartnerPrefSet = true)
                    _status.value = "Partner preferences updated."
                    load()
                } else {
                    _status.value = response.body()?.error?.message ?: "Couldn't update partner preferences right now."
                }
            } catch (error: Exception) {
                _status.value = when (error) {
                    is IOException -> "Service is temporarily not available. Please try again."
                    else -> "Couldn't update partner preferences right now. Please try again."
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updatePrivacySettings(photoPrivacy: String, profileVisibility: String, hideLastSeen: Boolean, contactPrivacy: String? = null) {
        val current = _profile.value ?: return
        val profileId = current.profileId
        if (profileId.isBlank()) return
        _profile.value = current.copy(
            photoPrivacy = photoPrivacy,
            profileVisibility = profileVisibility,
            hideLastSeen = hideLastSeen,
            contactPrivacy = contactPrivacy ?: current.contactPrivacy
        )
        viewModelScope.launch {
            _status.value = null
            try {
                val response = profileApi.updatePrivacy(
                    profileId,
                    PrivacySettingsRequest(
                        photoPrivacy = photoPrivacy,
                        profileVisibility = profileVisibility,
                        hideLastSeen = hideLastSeen,
                        contactPrivacy = contactPrivacy
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
        shareMode: String = _assistStatus.value.shareMode,
        selectedAdvisorIds: List<String> = _assistStatus.value.selectedAdvisorIds,
        preferredContactWindow: String,
        familyContactName: String,
        familyContactPhone: String,
        notes: String
    ) {
        viewModelScope.launch {
            val previous = _assistStatus.value
            val optimistic = previous.copy(
                isOptedIn = isOptedIn,
                supportLevel = supportLevel,
                shareMode = shareMode,
                selectedAdvisorIds = selectedAdvisorIds,
                preferredContactWindow = preferredContactWindow,
                familyContactName = familyContactName,
                familyContactPhone = familyContactPhone,
                notes = notes
            )
            _isSavingAssist.value = true
            _status.value = null
            _assistStatus.value = optimistic
            try {
                val response = profileApi.updateAssistStatus(
                    AssistStatusRequest(
                        isOptedIn = isOptedIn,
                        supportLevel = supportLevel,
                        shareMode = shareMode,
                        selectedAdvisorIds = selectedAdvisorIds,
                        preferredContactWindow = preferredContactWindow,
                        familyContactName = familyContactName,
                        familyContactPhone = familyContactPhone,
                        notes = notes
                    )
                )
                val body = response.body()
                if (response.isSuccessful && body?.success == true) {
                    _assistStatus.value = body.data ?: optimistic
                    _status.value = body.message ?: "SoulMatch Assist updated."
                    refreshAssistStatus(keepCurrentOnFailure = true)
                } else {
                    _assistStatus.value = previous
                    _status.value = body?.error?.message ?: "Couldn't update SoulMatch Assist right now."
                }
            } catch (error: Exception) {
                _assistStatus.value = previous
                _status.value = when (error) {
                    is IOException -> "Couldn't reach the server. Check your connection and try again."
                    else -> "Couldn't update SoulMatch Assist right now. Please try again."
                }
            } finally {
                _isSavingAssist.value = false
            }
        }
    }

    private suspend fun refreshAssistStatus(keepCurrentOnFailure: Boolean = false) {
        val refreshed = runCatching { profileApi.getAssistStatus() }
            .getOrNull()
            ?.body()
            ?.takeIf { it.success }
            ?.data

        when {
            refreshed != null -> _assistStatus.value = refreshed
            !keepCurrentOnFailure -> _assistStatus.value = AssistStatusData()
        }
    }

    fun submitProfileVerification() {
        submitTrustVerification("profile")
    }

    fun submitTrustVerification(
        type: String,
        document: MultipartBody.Part? = null,
        documentType: String? = null,
        referenceNumber: String? = null
    ) {
        val profileId = _profile.value?.profileId ?: return
        viewModelScope.launch {
            _isSubmittingVerification.value = true
            _status.value = null
            try {
                val response = if (document != null) {
                    profileApi.submitVerificationUpload(
                        profileId,
                        buildMap {
                            put("type", type.toRequestBody("text/plain".toMediaTypeOrNull()))
                            documentType?.takeIf { it.isNotBlank() }?.let {
                                put("documentType", it.toRequestBody("text/plain".toMediaTypeOrNull()))
                            }
                            referenceNumber?.takeIf { it.isNotBlank() }?.let {
                                put("referenceNumber", it.toRequestBody("text/plain".toMediaTypeOrNull()))
                            }
                        },
                        document
                    )
                } else {
                    profileApi.submitVerification(profileId, VerificationSubmitRequest(type = type))
                }
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
                    is IOException -> "Service is temporarily not available. Please try again."
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
        val resolved = profile?.safeProfileData() ?: ProfileData()
        val hasHoroscopeDetails = safeText(resolved.rashi).isNotBlank() ||
            safeText(resolved.nakshatra).isNotBlank() ||
            safeText(resolved.birthCity).isNotBlank() ||
            safeText(resolved.gotra).isNotBlank() ||
            resolved.isManglik

        return listOf(
            ProfileChecklistItem(
                title = "Basic details",
                description = "Name, DOB, gender, height, language, marital status, and current city",
                isComplete = safeText(resolved.firstName).isNotBlank() &&
                    safeText(resolved.lastName).isNotBlank() &&
                    safeText(resolved.dob).isNotBlank() &&
                    safeText(resolved.gender).isNotBlank() &&
                    resolved.heightCm != null &&
                    safeText(resolved.maritalStatus).isNotBlank() &&
                    safeText(resolved.motherTongue).isNotBlank() &&
                    safeText(resolved.workingCity).isNotBlank(),
                editStep = 1
            ),
            ProfileChecklistItem(
                title = "Religious and community",
                description = "Religion and community",
                isComplete = safeText(resolved.religion).isNotBlank() &&
                    safeText(resolved.caste).isNotBlank(),
                editStep = 2
            ),
            ProfileChecklistItem(
                title = "Work and education",
                description = "Education level, occupation, and annual income",
                isComplete = safeText(resolved.educationLevel).isNotBlank() &&
                    safeText(resolved.occupation).isNotBlank() &&
                    safeText(resolved.annualIncome).isNotBlank(),
                editStep = 3
            ),
            ProfileChecklistItem(
                title = "Family details",
                description = "Parent occupations and family type",
                isComplete = safeText(resolved.fatherOccupation).isNotBlank() &&
                    safeText(resolved.motherOccupation).isNotBlank() &&
                    safeText(resolved.familyType).isNotBlank(),
                editStep = 4
            ),
            ProfileChecklistItem(
                title = "Lifestyle",
                description = "Diet, smoking, and drinking",
                isComplete = safeText(resolved.diet).isNotBlank() &&
                    safeText(resolved.smoking).isNotBlank() &&
                    safeText(resolved.drinking).isNotBlank(),
                editStep = 5
            ),
            ProfileChecklistItem(
                title = "Partner preferences",
                description = "Match filters and recommendation inputs",
                isComplete = resolved.isPartnerPrefSet,
                editStep = 6,
                statusLabel = if (resolved.isPartnerPrefSet) "Complete" else "Pending"
            )
        )
    }

    private fun ProfileData.safeProfileData(): ProfileData = copy(
        profileId = safeText(profileId),
        userId = safeText(userId),
        firstName = safeText(firstName),
        lastName = safeText(lastName),
        dob = safeText(dob).ifBlank { null },
        gender = safeText(gender),
        phone = safeText(phone),
        email = safeText(email),
        religion = safeText(religion),
        caste = safeText(caste),
        motherTongue = safeText(motherTongue),
        maritalStatus = safeText(maritalStatus),
        profileStatus = safeText(profileStatus).ifBlank { "active" },
        profileCreatedBy = safeText(profileCreatedBy).ifBlank { "self" },
        verificationStatus = safeText(verificationStatus).ifBlank { "pending" },
        trustLevel = safeText(trustLevel).ifBlank { "low" },
        trustSignals = safeList(trustSignals),
        trustWarnings = safeList(trustWarnings),
        trustFactors = safeList(trustFactors),
        seriousnessLevel = safeText(seriousnessLevel).ifBlank { "low" },
        seriousnessSignals = safeList(seriousnessSignals),
        seriousnessWarnings = safeList(seriousnessWarnings),
        primaryPhotoUrl = safeText(primaryPhotoUrl).ifBlank { null },
        photoPrivacy = safeText(photoPrivacy).ifBlank { "all" },
        contactPrivacy = safeText(contactPrivacy).ifBlank { "visible" },
        contactAccessStatus = safeText(contactAccessStatus).ifBlank { "masked" },
        contactAccessMessage = safeText(contactAccessMessage),
        maskedPhone = safeText(maskedPhone),
        maskedEmail = safeText(maskedEmail),
        photoAccessStatus = safeText(photoAccessStatus).ifBlank { "visible" },
        photoAccessRequestId = safeText(photoAccessRequestId).ifBlank { null },
        profileVisibility = safeText(profileVisibility).ifBlank { "all" },
        lastLogin = safeText(lastLogin).ifBlank { null },
        updatedAt = safeText(updatedAt).ifBlank { null },
        educationLevel = safeText(educationLevel),
        occupation = safeText(occupation),
        annualIncome = safeText(annualIncome),
        workingCity = safeText(workingCity),
        workingState = safeText(workingState),
        workingPincode = safeText(workingPincode),
        complexion = safeText(complexion),
        bodyType = safeText(bodyType),
        bloodGroup = safeText(bloodGroup),
        fatherOccupation = safeText(fatherOccupation),
        motherOccupation = safeText(motherOccupation),
        familyType = safeText(familyType),
        familyCity = safeText(familyCity),
        familyState = safeText(familyState),
        familyLocality = safeText(familyLocality),
        familyPincode = safeText(familyPincode),
        diet = safeText(diet),
        smoking = safeText(smoking),
        drinking = safeText(drinking),
        aboutMe = safeText(aboutMe),
        rashi = safeText(rashi),
        nakshatra = safeText(nakshatra),
        birthCity = safeText(birthCity),
        gotra = safeText(gotra)
    )

    private fun PartnerPreferencesData.safePreferences(): PartnerPreferencesData = copy(
        religion = safeText(religion).ifBlank { null },
        manglikPref = safeText(manglikPref).ifBlank { "any" },
        educationLevels = safeStringList(educationLevels),
        occupations = safeStringList(occupations),
        locations = safeStringList(locations),
        locationPreferences = safeStringList(locationPreferences),
        dietPrefs = safeStringList(dietPrefs),
        incomePreferences = safeStringList(incomePreferences),
        lifestylePreferences = safeStringList(lifestylePreferences),
        maritalStatuses = safeStringList(maritalStatuses),
        familyTypes = safeStringList(familyTypes),
        timeline = safeText(timeline).ifBlank { null },
        dealBreakers = safeStringList(dealBreakers),
        goodToHave = safeStringList(goodToHave)
    )

    private fun safeStringList(value: List<String>?): List<String> =
        value.orEmpty()
            .mapNotNull { item -> (item as? String)?.trim()?.takeIf { it.isNotBlank() } }
            .distinct()

    private fun safeText(value: String?): String = value.orEmpty()

    private fun <T> safeList(value: List<T>?): List<T> = value ?: emptyList()
}

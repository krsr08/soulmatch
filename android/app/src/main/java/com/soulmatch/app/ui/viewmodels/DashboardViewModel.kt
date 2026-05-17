package com.soulmatch.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmatch.app.data.api.InterestApiService
import com.soulmatch.app.data.api.MatchingApiService
import com.soulmatch.app.data.api.ProfileApiService
import com.soulmatch.app.data.api.SearchApiService
import com.soulmatch.app.data.config.AppEnvironment
import com.soulmatch.app.data.local.UserPreferences
import com.soulmatch.app.data.local.ProfileInteractionStore
import com.soulmatch.app.data.mock.MarketFixtures
import com.soulmatch.app.data.models.InterestRequest
import com.soulmatch.app.data.models.InterestListItem
import com.soulmatch.app.data.models.PartnerPreferencesData
import com.soulmatch.app.data.models.PhotoAccessRequestBody
import com.soulmatch.app.data.models.ProfileData
import com.soulmatch.app.data.models.ProfileSummary
import com.soulmatch.app.data.models.RespondRequest
import com.soulmatch.app.data.models.SearchRequest
import com.soulmatch.app.data.models.toProfileSummary
import com.soulmatch.app.data.realtime.InterestSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val matchingApi: MatchingApiService,
    private val interestApi: InterestApiService,
    private val profileApi: ProfileApiService,
    private val searchApi: SearchApiService,
    private val interestSyncManager: InterestSyncManager,
    private val prefs: UserPreferences
) : ViewModel() {
    private val _matches = MutableStateFlow<List<ProfileSummary>>(emptyList())
    private val _pendingInvitations = MutableStateFlow<List<InterestListItem>>(emptyList())
    private val _myProfile = MutableStateFlow(ProfileData())
    private val _assistEnabled = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(false)
    private val _headline = MutableStateFlow("New matches are ready")
    private val _verifiedOnlyMode = MutableStateFlow(false)
    private var loadedMatches: List<ProfileSummary> = emptyList()

    val matches: StateFlow<List<ProfileSummary>> = _matches.asStateFlow()
    val pendingInvitations: StateFlow<List<InterestListItem>> = _pendingInvitations.asStateFlow()
    val myProfile: StateFlow<ProfileData> = _myProfile.asStateFlow()
    val assistEnabled: StateFlow<Boolean> = _assistEnabled.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val headline: StateFlow<String> = _headline.asStateFlow()
    val verifiedOnlyMode: StateFlow<Boolean> = _verifiedOnlyMode.asStateFlow()

    init {
        loadProfile()
        loadMatches()
        viewModelScope.launch {
            interestSyncManager.changes.collect {
                _matches.value = applyInteractionState(_matches.value)
                loadPendingInvitations()
            }
        }
        viewModelScope.launch {
            ProfileInteractionStore.state.collect {
                _matches.value = loadedMatches.applyLocalInteractionState().filterVisibleProfiles()
            }
        }
    }

    private suspend fun canUseDemoFallback(): Boolean =
        AppEnvironment.allowDemoFallback && prefs.authToken.first().isNullOrBlank()

    fun loadProfile() {
        viewModelScope.launch {
            val canUseFallback = canUseDemoFallback()
            val response = runCatching { profileApi.getMyProfile() }.getOrNull()
            val profile = response
                ?.body()
                ?.takeIf { response.isSuccessful && it.success }
                ?.data
            _myProfile.value = (profile ?: if (canUseFallback) MarketFixtures.myProfile else ProfileData()).safeDashboardProfile()
            _matches.value = loadedMatches.applyLocalInteractionState().filterVisibleProfiles()
            val assistResponse = runCatching { profileApi.getAssistStatus() }.getOrNull()
            _assistEnabled.value = assistResponse
                ?.body()
                ?.takeIf { assistResponse.isSuccessful && it.success }
                ?.data
                ?.isOptedIn
                ?: false
        }
    }

    fun loadMatches() {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val canUseFallback = canUseDemoFallback()
                val verifiedOnly = _verifiedOnlyMode.value
                val remoteResponse = runCatching { matchingApi.getRecommended(page = 1, limit = MAX_DISCOVERY_PROFILES, verifiedOnly = verifiedOnly) }.getOrNull()
                val remoteBody = remoteResponse?.body()
                val baseMatches = if (remoteResponse?.isSuccessful == true && remoteBody?.success == true) {
                    remoteBody.data?.matches.orEmpty()
                } else {
                    emptyList()
                }.ifEmpty { if (canUseFallback) MarketFixtures.matches else emptyList() }
                    .map { it.safeDashboardSummary() }
                    .let { list -> if (verifiedOnly) list.filter { it.isVerified } else list }
                val expandedMatches = if (baseMatches.size >= MIN_DISCOVERY_PROFILES) {
                    baseMatches
                } else {
                    mergeDiscoveryProfiles(baseMatches, fetchSearchTopUp(baseMatches, verifiedOnly))
                }
                loadedMatches = applyInteractionState(expandedMatches.map { it.safeDashboardSummary() })
                _matches.value = loadedMatches.applyLocalInteractionState().filterVisibleProfiles()
                _headline.value = when {
                    verifiedOnly && _matches.value.isEmpty() -> "No verified profiles available yet"
                    _matches.value.any { it.compatibilityScore >= 90 } -> "High-compatibility matches this week"
                    _matches.value.isEmpty() -> "Complete your profile for more visibility"
                    else -> "Profiles aligned to your preferences"
                }
                loadPendingInvitations()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setVerifiedOnlyMode(enabled: Boolean) {
        if (_verifiedOnlyMode.value == enabled) return
        _verifiedOnlyMode.value = enabled
        loadMatches()
    }

    private suspend fun loadPendingInvitations() {
        val canUseFallback = canUseDemoFallback()
        val remoteResponse = runCatching { interestApi.getReceived() }.getOrNull()
        val remoteItems = remoteResponse
            ?.body()
            ?.takeIf { remoteResponse.isSuccessful && it.success }
            ?.data
            .orEmpty()
        _pendingInvitations.value = remoteItems
            .ifEmpty { if (canUseFallback) MarketFixtures.receivedInterests else emptyList() }
            .filter { it.status.equals("pending", ignoreCase = true) }
    }

    fun sendInterest(profileId: String) {
        viewModelScope.launch {
            val response = runCatching { interestApi.sendInterest(InterestRequest(profileId)) }.getOrNull()
            val result = response?.body()?.takeIf { response.isSuccessful && it.success }?.data
            if (result?.status == "interest_sent" || result?.status == "interest_resent" || result?.status == "already_sent") {
                ProfileInteractionStore.markInterest(profileId)
                _matches.update { list ->
                    list.map { profile ->
                        if (profile.profileId == profileId) profile.copy(interestSent = true) else profile
                    }
                }
                interestSyncManager.notifyChanged()
            }
        }
    }

    fun toggleShortlist(profileId: String) {
        viewModelScope.launch {
            val response = runCatching { interestApi.toggleShortlist(profileId) }.getOrNull()
            _matches.update { list ->
                list.map { profile ->
                    if (profile.profileId == profileId) {
                        val action = response?.body()?.takeIf { response.isSuccessful && it.success }?.data?.action
                        if (action == null) return@map profile
                        val nextShortlisted = action == "added"
                        ProfileInteractionStore.setShortlisted(profileId, nextShortlisted)
                        profile.copy(shortlisted = nextShortlisted)
                    } else {
                        profile
                    }
                }
            }
            interestSyncManager.notifyChanged()
        }
    }

    fun hideProfile(profileId: String) {
        ProfileInteractionStore.hideProfile(profileId)
        _matches.value = loadedMatches.applyLocalInteractionState().filterVisibleProfiles()
        _headline.value = "Profile hidden from your feed"
    }

    fun markProfileViewed(profileId: String) {
        ProfileInteractionStore.markViewed(profileId)
        _matches.value = loadedMatches.applyLocalInteractionState().filterVisibleProfiles()
    }

    fun requestPhotoAccess(profileId: String) {
        if (profileId.isBlank()) return
        viewModelScope.launch {
            val response = runCatching {
                profileApi.requestPhotoAccess(profileId, PhotoAccessRequestBody("I would like to view your profile photo."))
            }.getOrNull()
            if (response?.isSuccessful == true && response.body()?.success == true) {
                _headline.value = "Photo request sent"
            }
        }
    }

    fun blockProfile(profileId: String) {
        viewModelScope.launch {
            val response = runCatching { profileApi.blockProfile(profileId) }.getOrNull()
            if (response?.isSuccessful == true && response.body()?.success == true) {
                ProfileInteractionStore.blockProfile(profileId)
                _matches.value = loadedMatches.applyLocalInteractionState().filterVisibleProfiles()
                _headline.value = "Blocked profile removed"
            }
        }
    }

    fun reportProfile(profileId: String, concern: String) {
        viewModelScope.launch {
            val response = runCatching { profileApi.reportProfile(profileId, mapOf("reason" to "member_report", "description" to concern)) }.getOrNull()
            if (response?.isSuccessful == true && response.body()?.success == true) {
                ProfileInteractionStore.reportConcern(profileId, concern)
                _headline.value = "Concern saved for review"
            }
        }
    }

    fun respondToInvitation(interestId: String, status: String) {
        viewModelScope.launch {
            runCatching { interestApi.respond(interestId, RespondRequest(status)) }
            _pendingInvitations.update { invitations ->
                invitations.filterNot { it.interestId == interestId }
            }
            interestSyncManager.notifyChanged()
        }
    }

    fun savePartnerPreferences(preferences: PartnerPreferencesData, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val profileId = _myProfile.value.profileId
            if (profileId.isBlank()) return@launch
            val response = runCatching { profileApi.updatePreferences(profileId, preferences) }.getOrNull()
            if (response?.isSuccessful == true && response.body()?.success == true) {
                _myProfile.update { it.copy(isPartnerPrefSet = true) }
                onSaved()
            }
        }
    }

    private suspend fun applyInteractionState(matches: List<ProfileSummary>): List<ProfileSummary> {
        if (matches.isEmpty()) return emptyList()
        val sentItems = runCatching { interestApi.getSent() }
            .getOrNull()
            ?.body()
            ?.takeIf { it.success }
            ?.data
            .orEmpty()
        val activeSentIds = sentItems
            .filter { it.status.equals("pending", ignoreCase = true) || it.status.equals("accepted", ignoreCase = true) }
            .map { it.profileId }
            .toSet()
        val declinedSentIds = sentItems
            .filter { it.status.equals("declined", ignoreCase = true) }
            .map { it.profileId }
            .toSet()
        declinedSentIds.forEach(ProfileInteractionStore::clearSentInterest)
        val shortlistedIds = runCatching { interestApi.getShortlist() }
            .getOrNull()
            ?.body()
            ?.takeIf { it.success }
            ?.data
            .orEmpty()
            .map { it.profileId }
            .toSet()
        return matches.map { profile ->
            val safeProfile = profile.safeDashboardSummary()
            safeProfile.copy(
                interestSent = when (safeProfile.profileId) {
                    in declinedSentIds -> false
                    else -> safeProfile.interestSent || safeProfile.profileId in activeSentIds || safeProfile.profileId in ProfileInteractionStore.state.value.sentInterestProfileIds
                },
                shortlisted = when (safeProfile.profileId) {
                    in ProfileInteractionStore.state.value.unshortlistedProfileIds -> false
                    else -> safeProfile.shortlisted || safeProfile.profileId in shortlistedIds || safeProfile.profileId in ProfileInteractionStore.state.value.shortlistedProfileIds
                }
            ).safeDashboardSummary()
        }
    }

    private fun List<ProfileSummary>.filterVisibleProfiles(): List<ProfileSummary> {
        val interactions = ProfileInteractionStore.state.value
        return map { it.safeDashboardSummary() }
            .filterNot { it.profileId in interactions.hiddenProfileIds || it.profileId in interactions.blockedProfileIds }
            .filterForViewerGender()
    }

    private fun List<ProfileSummary>.filterForViewerGender(): List<ProfileSummary> {
        val targetGender = oppositeGender(_myProfile.value.gender) ?: return this
        return filter { profile ->
            profile.gender.isBlank() || profile.gender.equals(targetGender, ignoreCase = true)
        }
    }

    private fun List<ProfileSummary>.applyLocalInteractionState(): List<ProfileSummary> {
        val interactions = ProfileInteractionStore.state.value
        return map { profile ->
            val safeProfile = profile.safeDashboardSummary()
            safeProfile.copy(
                interestSent = safeProfile.interestSent || safeProfile.profileId in interactions.sentInterestProfileIds,
                shortlisted = when (safeProfile.profileId) {
                    in interactions.unshortlistedProfileIds -> false
                    else -> safeProfile.shortlisted || safeProfile.profileId in interactions.shortlistedProfileIds
                }
            ).safeDashboardSummary()
        }
    }

    private suspend fun fetchSearchTopUp(
        seed: List<ProfileSummary>,
        verifiedOnly: Boolean
    ): List<ProfileSummary> {
        val seedMap = seed.associateBy { it.profileId }
        val candidateGender = oppositeGender(_myProfile.value.gender)
        val response = runCatching {
            searchApi.advancedSearch(
                SearchRequest(
                    page = 1,
                    limit = MAX_DISCOVERY_PROFILES,
                    verifiedOnly = verifiedOnly,
                    gender = candidateGender
                )
            )
        }.getOrNull()
        val topUpProfiles = response
            ?.body()
            ?.takeIf { response.isSuccessful && it.success }
            ?.data
            ?.results
            ?.map { result -> result.toProfileSummary(seedMap[result.profileId]) }
            .orEmpty()
        return topUpProfiles.map { profile ->
            val safeProfile = profile.safeDashboardSummary()
            if (safeProfile.profileId in seedMap) safeProfile else safeProfile.withLiveCompatibility()
        }
    }

    private fun mergeDiscoveryProfiles(
        primary: List<ProfileSummary>,
        secondary: List<ProfileSummary>
    ): List<ProfileSummary> {
        val byId = linkedMapOf<String, ProfileSummary>()
        (primary + secondary).forEach { profile ->
            if (profile.profileId.isNotBlank()) byId.putIfAbsent(profile.profileId, profile)
        }
        return byId.values.toList()
    }

    private suspend fun ProfileSummary.withLiveCompatibility(): ProfileSummary {
        val safeProfile = safeDashboardSummary()
        if (safeProfile.profileId.isBlank()) return safeProfile
        val response = runCatching { matchingApi.getCompatibility(safeProfile.profileId) }.getOrNull()
        val compatibility = response
            ?.body()
            ?.takeIf { response.isSuccessful && it.success }
            ?.data
            ?: return safeProfile
        return safeProfile.copy(
            compatibilityScore = compatibility.overallScore.coerceIn(0, 99),
            compatibilityBreakdown = compatibility.breakdown ?: safeProfile.compatibilityBreakdown
        )
    }

    private fun oppositeGender(gender: String?): String? {
        return when (safeText(gender).trim().lowercase()) {
            "male" -> "female"
            "female" -> "male"
            else -> null
        }
    }

    private fun ProfileData.safeDashboardProfile(): ProfileData = copy(
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

    private fun ProfileSummary.safeDashboardSummary(): ProfileSummary = copy(
        profileId = safeText(profileId),
        userId = safeText(userId),
        name = safeText(name).ifBlank { "SoulMatch member" },
        gender = safeText(gender),
        location = safeText(location),
        occupation = safeText(occupation),
        primaryPhoto = safeText(primaryPhoto).ifBlank { null },
        trustLevel = safeText(trustLevel).ifBlank { "low" },
        trustSignals = safeList(trustSignals),
        trustFactors = safeList(trustFactors),
        education = safeText(education),
        community = safeText(community),
        religion = safeText(religion),
        annualIncome = safeText(annualIncome),
        familyCity = safeText(familyCity),
        familyState = safeText(familyState),
        maritalStatus = safeText(maritalStatus),
        diet = safeText(diet),
        createdAt = safeText(createdAt),
        lastActiveLabel = safeText(lastActiveLabel).ifBlank { "Recently Active" },
        matchReasons = safeList(matchReasons),
        profileCreatedBy = safeText(profileCreatedBy).ifBlank { "self" }
    )

    private fun safeText(value: String?): String = value.orEmpty()

    private fun <T> safeList(value: List<T>?): List<T> = value ?: emptyList()

    private companion object {
        const val MIN_DISCOVERY_PROFILES = 15
        const val MAX_DISCOVERY_PROFILES = 80
    }
}

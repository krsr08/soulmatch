package com.soulmatch.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmatch.app.data.api.ChatApiService
import com.soulmatch.app.data.api.InterestApiService
import com.soulmatch.app.data.api.MatchingApiService
import com.soulmatch.app.data.api.ProfileApiService
import com.soulmatch.app.data.config.AppEnvironment
import com.soulmatch.app.data.local.ProfileInteractionStore
import com.soulmatch.app.data.mock.MarketFixtures
import com.soulmatch.app.data.models.CompatibilityData
import com.soulmatch.app.data.models.FamilyDecisionRequest
import com.soulmatch.app.data.models.IcebreakerRequest
import com.soulmatch.app.data.models.InterestRequest
import com.soulmatch.app.data.models.PhotoAccessRequestBody
import com.soulmatch.app.data.models.ProfileData
import com.soulmatch.app.data.models.ProfileSummary
import com.soulmatch.app.data.models.fullName
import com.soulmatch.app.data.realtime.InterestSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    private val profileApi: ProfileApiService,
    private val matchingApi: MatchingApiService,
    private val interestApi: InterestApiService,
    private val chatApi: ChatApiService,
    private val interestSyncManager: InterestSyncManager
) : ViewModel() {
    private val _profile = MutableStateFlow<ProfileData?>(null)
    private val _compatibility = MutableStateFlow(CompatibilityData())
    private val _isLoading = MutableStateFlow(false)
    private val _interestSent = MutableStateFlow(false)
    private val _shortlisted = MutableStateFlow(false)
    private val _canChat = MutableStateFlow(false)
    private val _status = MutableStateFlow<String?>(null)
    private val _icebreakers = MutableStateFlow<List<String>>(emptyList())
    private val _isGeneratingIcebreakers = MutableStateFlow(false)

    val profile: StateFlow<ProfileData?> = _profile.asStateFlow()
    val compatibility: StateFlow<CompatibilityData> = _compatibility.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val interestSent: StateFlow<Boolean> = _interestSent.asStateFlow()
    val shortlisted: StateFlow<Boolean> = _shortlisted.asStateFlow()
    val canChat: StateFlow<Boolean> = _canChat.asStateFlow()
    val status: StateFlow<String?> = _status.asStateFlow()
    val icebreakers: StateFlow<List<String>> = _icebreakers.asStateFlow()
    val isGeneratingIcebreakers: StateFlow<Boolean> = _isGeneratingIcebreakers.asStateFlow()

    fun load(profileId: String) {
        if (_isLoading.value) return
        ProfileInteractionStore.markViewed(profileId)
        viewModelScope.launch {
            _isLoading.value = true
            val remoteProfile = runCatching { profileApi.getProfile(profileId) }
                .getOrNull()
                ?.body()
                ?.takeIf { it.success }
                ?.data
                ?: if (AppEnvironment.allowDemoFallback) MarketFixtures.profileDetails(profileId) else null
            val discoverySummary = resolveDiscoverySummary(profileId)
            _profile.value = remoteProfile?.mergeDiscoverySummary(discoverySummary)
            val summaryCompatibility = discoverySummary?.toCompatibilityData()
            if (summaryCompatibility != null) {
                _compatibility.value = summaryCompatibility
            }
            val remoteCompatibility = runCatching { matchingApi.getCompatibility(profileId) }
                .getOrNull()
                ?.body()
                ?.takeIf { it.success }
                ?.data
            _compatibility.value = remoteCompatibility
                ?: summaryCompatibility
                ?: if (AppEnvironment.allowDemoFallback) MarketFixtures.compatibility(profileId) else CompatibilityData()
            val participantUserId = _profile.value?.userId.orEmpty()
            _canChat.value = if (participantUserId.isNotBlank()) {
                runCatching { chatApi.checkEligibility(participantUserId) }
                    .getOrNull()
                    ?.body()
                    ?.takeIf { it.success }
                    ?.data
                    ?.canChat
                    ?: false
            } else {
                false
            }
            val sentIds = runCatching { interestApi.getSent() }
                .getOrNull()
                ?.body()
                ?.takeIf { it.success }
                ?.data
                .orEmpty()
                .map { it.profileId }
                .toSet()
            val shortlistedIds = runCatching { interestApi.getShortlist() }
                .getOrNull()
                ?.body()
                ?.takeIf { it.success }
                ?.data
                .orEmpty()
                .map { it.profileId }
                .toSet()
            _shortlisted.value = profileId in shortlistedIds
            val interactions = ProfileInteractionStore.state.value
            _shortlisted.value = _shortlisted.value || profileId in interactions.shortlistedProfileIds
            _interestSent.value = profileId in sentIds || profileId in interactions.sentInterestProfileIds
            _isLoading.value = false
        }
    }

    private suspend fun resolveDiscoverySummary(profileId: String): ProfileSummary? {
        val recommended = runCatching { matchingApi.getRecommended(page = 1, limit = 25) }
            .getOrNull()
            ?.body()
            ?.takeIf { it.success }
            ?.data
            ?.matches
            .orEmpty()
        return recommended.firstOrNull { it.profileId == profileId }
            ?: if (AppEnvironment.allowDemoFallback) MarketFixtures.matchSeed(profileId) else null
    }

    private fun ProfileData.mergeDiscoverySummary(summary: ProfileSummary?): ProfileData {
        if (summary == null) return this
        val summaryVerified = summary.isVerified || verificationStatus.equals("verified", ignoreCase = true)
        return copy(
            age = if (age > 0) age else summary.age,
            occupation = occupation.ifBlank { summary.occupation },
            workingCity = workingCity.ifBlank { summary.location },
            heightCm = heightCm ?: summary.heightCm,
            primaryPhotoUrl = primaryPhotoUrl ?: summary.primaryPhoto,
            profileCreatedBy = if (summary.profileCreatedBy.isNotBlank()) summary.profileCreatedBy else profileCreatedBy,
            verificationStatus = if (summaryVerified) "verified" else verificationStatus,
            trustScore = if (summary.trustScore > 0) summary.trustScore else trustScore,
            trustLevel = if (summary.trustLevel.isNotBlank()) summary.trustLevel else trustLevel,
            trustSignals = if (summary.trustSignals.isNotEmpty()) summary.trustSignals else trustSignals,
            trustFactors = if (summary.trustFactors.isNotEmpty()) summary.trustFactors else trustFactors
        )
    }

    private fun ProfileSummary.toCompatibilityData(): CompatibilityData? {
        if (compatibilityScore <= 0) return null
        return CompatibilityData(
            overallScore = compatibilityScore.coerceIn(0, 99),
            breakdown = compatibilityBreakdown
        )
    }

    fun sendInterest() {
        val target = _profile.value?.profileId ?: return
        viewModelScope.launch {
            val response = runCatching { interestApi.sendInterest(InterestRequest(target)) }.getOrNull()
            val result = response?.body()?.takeIf { response.isSuccessful && it.success }?.data
            if (result?.status == "interest_sent" || result?.status == "already_sent") {
                ProfileInteractionStore.markInterest(target)
                _interestSent.value = true
                if (result?.isMutual == true) {
                    _canChat.value = true
                }
                interestSyncManager.notifyChanged()
            }
        }
    }

    fun toggleShortlist() {
        val target = _profile.value?.profileId ?: return
        viewModelScope.launch {
            val response = runCatching { interestApi.toggleShortlist(target) }.getOrNull()
            val action = response?.body()?.takeIf { response.isSuccessful && it.success }?.data?.action
            if (action == null) return@launch
            _shortlisted.value = action == "added"
            ProfileInteractionStore.setShortlisted(target, _shortlisted.value)
            interestSyncManager.notifyChanged()
        }
    }

    fun requestPhotoAccess() {
        val target = _profile.value?.profileId ?: return
        viewModelScope.launch {
            val response = runCatching {
                profileApi.requestPhotoAccess(target, PhotoAccessRequestBody("I would like to view your profile photo."))
            }.getOrNull()
            val body = response?.body()
            if (response?.isSuccessful == true && body?.success == true) {
                val nextStatus = body.data?.status ?: "pending"
                _profile.value = _profile.value?.copy(photoAccessStatus = nextStatus)
                _status.value = body.message ?: "Photo access request sent."
            } else {
                _status.value = body?.error?.message ?: "Couldn't request photo access right now."
            }
        }
    }

    fun generateIcebreakers() {
        val target = _profile.value?.profileId ?: return
        viewModelScope.launch {
            _isGeneratingIcebreakers.value = true
            _status.value = null
            val fallback = localIcebreakers(_profile.value)
            try {
                val response = profileApi.getIcebreakers(target, IcebreakerRequest())
                val body = response.body()
                _icebreakers.value = if (response.isSuccessful && body?.success == true) {
                    body.data?.suggestions?.takeIf { it.isNotEmpty() } ?: fallback
                } else {
                    fallback
                }
            } catch (_: Exception) {
                _icebreakers.value = fallback
            } finally {
                _isGeneratingIcebreakers.value = false
            }
        }
    }

    fun addToFamilyBoard() {
        val target = _profile.value?.profileId ?: return
        viewModelScope.launch {
            val response = runCatching {
                profileApi.upsertFamilyDecision(
                    target,
                    FamilyDecisionRequest(
                        status = "family_review",
                        note = "Added from profile detail for family review.",
                        nextStep = "Discuss with family"
                    )
                )
            }.getOrNull()
            val body = response?.body()
            _status.value = if (response?.isSuccessful == true && body?.success == true) {
                body.message ?: "Added to your family decision board."
            } else {
                body?.error?.message ?: "Couldn't add this profile to family board right now."
            }
        }
    }

    fun hideProfile() {
        _profile.value?.profileId?.let(ProfileInteractionStore::hideProfile)
    }

    fun blockProfile() {
        val target = _profile.value?.profileId ?: return
        viewModelScope.launch {
            val response = runCatching { profileApi.blockProfile(target) }.getOrNull()
            if (response?.isSuccessful == true && response.body()?.success == true) {
                ProfileInteractionStore.blockProfile(target)
            }
        }
    }

    fun reportConcern(concern: String) {
        val target = _profile.value?.profileId ?: return
        viewModelScope.launch {
            val response = runCatching {
                profileApi.reportProfile(target, mapOf("reason" to "member_report", "description" to concern))
            }.getOrNull()
            if (response?.isSuccessful == true && response.body()?.success == true) {
                ProfileInteractionStore.reportConcern(target, concern)
            }
        }
    }

    fun clearStatus() {
        _status.value = null
    }

    private fun localIcebreakers(profile: ProfileData?): List<String> {
        val name = profile?.fullName()?.takeIf { it.isNotBlank() } ?: "there"
        val location = if (profile == null) "" else profile.workingCity.ifBlank { profile.familyCity }
        val work = listOfNotNull(profile?.educationLevel, profile?.occupation, location)
            .filter { it.isNotBlank() }
            .joinToString(", ")
        return listOf(
            "Hi $name, I liked your profile and would like to understand what kind of partnership you are hoping for.",
            "Hello $name, your ${work.ifBlank { "profile details" }} stood out. Would you be open to a respectful conversation?",
            "Hi $name, what values do you feel are most important when two families start a matrimonial conversation?"
        )
    }
}

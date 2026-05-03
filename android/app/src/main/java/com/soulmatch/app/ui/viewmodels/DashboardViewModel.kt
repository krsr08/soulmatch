package com.soulmatch.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmatch.app.data.api.InterestApiService
import com.soulmatch.app.data.api.MatchingApiService
import com.soulmatch.app.data.api.ProfileApiService
import com.soulmatch.app.data.config.AppEnvironment
import com.soulmatch.app.data.local.UserPreferences
import com.soulmatch.app.data.local.ProfileInteractionStore
import com.soulmatch.app.data.mock.MarketFixtures
import com.soulmatch.app.data.models.InterestRequest
import com.soulmatch.app.data.models.InterestListItem
import com.soulmatch.app.data.models.PartnerPreferencesData
import com.soulmatch.app.data.models.ProfileData
import com.soulmatch.app.data.models.ProfileSummary
import com.soulmatch.app.data.models.RespondRequest
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
    private val interestSyncManager: InterestSyncManager,
    private val prefs: UserPreferences
) : ViewModel() {
    private val _matches = MutableStateFlow<List<ProfileSummary>>(emptyList())
    private val _pendingInvitations = MutableStateFlow<List<InterestListItem>>(emptyList())
    private val _myProfile = MutableStateFlow(ProfileData())
    private val _isLoading = MutableStateFlow(false)
    private val _headline = MutableStateFlow("New matches are ready")
    private val _verifiedOnlyMode = MutableStateFlow(false)
    private var loadedMatches: List<ProfileSummary> = emptyList()

    val matches: StateFlow<List<ProfileSummary>> = _matches.asStateFlow()
    val pendingInvitations: StateFlow<List<InterestListItem>> = _pendingInvitations.asStateFlow()
    val myProfile: StateFlow<ProfileData> = _myProfile.asStateFlow()
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
            _myProfile.value = profile ?: if (canUseFallback) MarketFixtures.myProfile else ProfileData()
        }
    }

    fun loadMatches() {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            val canUseFallback = canUseDemoFallback()
            val verifiedOnly = _verifiedOnlyMode.value
            val remoteResponse = runCatching { matchingApi.getRecommended(page = 1, limit = 25, verifiedOnly = verifiedOnly) }.getOrNull()
            val remoteBody = remoteResponse?.body()
            val baseMatches = if (remoteResponse?.isSuccessful == true && remoteBody?.success == true) {
                remoteBody.data?.matches.orEmpty()
            } else {
                emptyList()
            }.ifEmpty { if (canUseFallback) MarketFixtures.matches else emptyList() }
                .let { list -> if (verifiedOnly) list.filter { it.isVerified } else list }
            loadedMatches = applyInteractionState(baseMatches)
            _matches.value = loadedMatches.applyLocalInteractionState().filterVisibleProfiles()
            _headline.value = when {
                verifiedOnly && _matches.value.isEmpty() -> "No verified profiles available yet"
                _matches.value.any { it.compatibilityScore >= 90 } -> "High-compatibility matches this week"
                _matches.value.isEmpty() -> "Complete your profile for more visibility"
                else -> "Profiles aligned to your preferences"
            }
            loadPendingInvitations()
            _isLoading.value = false
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
            profile.copy(
                interestSent = when (profile.profileId) {
                    in declinedSentIds -> false
                    else -> profile.interestSent || profile.profileId in activeSentIds || profile.profileId in ProfileInteractionStore.state.value.sentInterestProfileIds
                },
                shortlisted = when (profile.profileId) {
                    in ProfileInteractionStore.state.value.unshortlistedProfileIds -> false
                    else -> profile.shortlisted || profile.profileId in shortlistedIds || profile.profileId in ProfileInteractionStore.state.value.shortlistedProfileIds
                }
            )
        }
    }

    private fun List<ProfileSummary>.filterVisibleProfiles(): List<ProfileSummary> {
        val interactions = ProfileInteractionStore.state.value
        return filterNot { it.profileId in interactions.hiddenProfileIds || it.profileId in interactions.blockedProfileIds }
    }

    private fun List<ProfileSummary>.applyLocalInteractionState(): List<ProfileSummary> {
        val interactions = ProfileInteractionStore.state.value
        return map { profile ->
            profile.copy(
                interestSent = profile.interestSent || profile.profileId in interactions.sentInterestProfileIds,
                shortlisted = when (profile.profileId) {
                    in interactions.unshortlistedProfileIds -> false
                    else -> profile.shortlisted || profile.profileId in interactions.shortlistedProfileIds
                }
            )
        }
    }
}

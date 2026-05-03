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
import com.soulmatch.app.data.models.InterestRequest
import com.soulmatch.app.data.models.PhotoAccessRequestBody
import com.soulmatch.app.data.models.ProfileData
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

    val profile: StateFlow<ProfileData?> = _profile.asStateFlow()
    val compatibility: StateFlow<CompatibilityData> = _compatibility.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val interestSent: StateFlow<Boolean> = _interestSent.asStateFlow()
    val shortlisted: StateFlow<Boolean> = _shortlisted.asStateFlow()
    val canChat: StateFlow<Boolean> = _canChat.asStateFlow()
    val status: StateFlow<String?> = _status.asStateFlow()

    fun load(profileId: String) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            val remoteProfile = runCatching { profileApi.getProfile(profileId) }
                .getOrNull()
                ?.body()
                ?.takeIf { it.success }
                ?.data
                ?: if (AppEnvironment.allowDemoFallback) MarketFixtures.profileDetails(profileId) else null
            _profile.value = remoteProfile
            _compatibility.value = runCatching { matchingApi.getCompatibility(profileId) }
                .getOrNull()
                ?.body()
                ?.takeIf { it.success }
                ?.data
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
}

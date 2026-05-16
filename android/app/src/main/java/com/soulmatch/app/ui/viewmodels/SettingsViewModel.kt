package com.soulmatch.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmatch.app.data.api.AuthApiService
import com.soulmatch.app.data.api.ProfileApiService
import com.soulmatch.app.data.config.AppEnvironment
import com.soulmatch.app.data.local.UserPreferences
import com.soulmatch.app.data.local.ProfileInteractionStore
import com.soulmatch.app.data.mock.MarketFixtures
import com.soulmatch.app.data.models.PrivacySettingsRequest
import com.soulmatch.app.data.models.ProfileStatusRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SettingsUiState(
    val pushEnabled: Boolean = true,
    val photoPrivacyEnabled: Boolean = true,
    val contactMasked: Boolean = false,
    val contactFilterEnabled: Boolean = false,
    val profileVisible: Boolean = true,
    val profileActive: Boolean = true
)

data class PrivacyMemberUi(
    val profileId: String,
    val name: String,
    val detail: String
)

data class ReportedConcernUi(
    val profileId: String,
    val name: String,
    val detail: String,
    val concern: String
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val profileApi: ProfileApiService,
    private val authApi: AuthApiService
) : ViewModel() {
    private val _settings = MutableStateFlow(SettingsUiState())
    private val _status = MutableStateFlow<String?>(null)
    private val _profileStatus = MutableStateFlow("active")
    private val _contactPrivacy = MutableStateFlow("visible")
    private val removedHiddenProfileIds = mutableSetOf<String>()
    private val removedBlockedProfileIds = mutableSetOf<String>()
    private val starterHiddenMembers = if (AppEnvironment.allowDemoFallback) MarketFixtures.matches.drop(5).take(1).map {
        PrivacyMemberUi(it.profileId, it.name, "${it.location} | ${it.community}")
    } else emptyList()
    private val starterBlockedMembers = if (AppEnvironment.allowDemoFallback) MarketFixtures.matches.drop(9).take(1).map {
        PrivacyMemberUi(it.profileId, it.name, "${it.location} | ${it.community}")
    } else emptyList()
    private val _hiddenMembers = MutableStateFlow(starterHiddenMembers)
    private val _blockedMembers = MutableStateFlow(starterBlockedMembers)
    private val _reportedConcerns = MutableStateFlow<List<ReportedConcernUi>>(emptyList())

    val settings: StateFlow<SettingsUiState> = _settings.asStateFlow()
    val status: StateFlow<String?> = _status.asStateFlow()
    val hiddenMembers: StateFlow<List<PrivacyMemberUi>> = _hiddenMembers.asStateFlow()
    val blockedMembers: StateFlow<List<PrivacyMemberUi>> = _blockedMembers.asStateFlow()
    val reportedConcerns: StateFlow<List<ReportedConcernUi>> = _reportedConcerns.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                prefs.pushNotifications,
                prefs.photoPrivacy,
                prefs.contactFilters,
                prefs.profileVisibility,
                _profileStatus
            ) { push, photoPrivacy, contactFilters, visibility, profileStatus ->
                SettingsUiState(
                    pushEnabled = push,
                    photoPrivacyEnabled = photoPrivacy == "request_only" || photoPrivacy == "matches_only" || photoPrivacy == "private",
                    contactMasked = _contactPrivacy.value == "masked",
                    contactFilterEnabled = contactFilters,
                    profileVisible = visibility == "all",
                    profileActive = !profileStatus.equals("inactive", ignoreCase = true)
                )
            }.collect { _settings.value = it }
        }
        viewModelScope.launch {
            _contactPrivacy.collect { contactPrivacy ->
                _settings.value = _settings.value.copy(contactMasked = contactPrivacy == "masked")
            }
        }
        viewModelScope.launch {
            val response = runCatching { profileApi.getMyProfile() }.getOrNull()
            val profile = response
                ?.body()
                ?.takeIf { response.isSuccessful && it.success }
                ?.data
            _profileStatus.value = profile?.profileStatus?.ifBlank { "active" } ?: "active"
            _contactPrivacy.value = profile?.contactPrivacy?.ifBlank { "visible" } ?: "visible"
        }
        viewModelScope.launch {
            ProfileInteractionStore.state.collect { interaction ->
                _hiddenMembers.value = (
                    starterHiddenMembers.filterNot { it.profileId in removedHiddenProfileIds } +
                        interaction.hiddenProfileIds.map(::memberForProfileId)
                    ).distinctBy { it.profileId }
                _blockedMembers.value = (
                    starterBlockedMembers.filterNot { it.profileId in removedBlockedProfileIds } +
                        interaction.blockedProfileIds.map(::memberForProfileId)
                    ).distinctBy { it.profileId }
                _reportedConcerns.value = interaction.reportedConcerns.values
                    .sortedByDescending { it.updatedMillis }
                    .map { concern ->
                        val member = memberForProfileId(concern.profileId)
                        ReportedConcernUi(
                            profileId = concern.profileId,
                            name = member.name,
                            detail = member.detail,
                            concern = concern.concern
                        )
                    }
            }
        }
    }

    fun setPushNotifications(enabled: Boolean) {
        viewModelScope.launch {
            prefs.savePushNotifications(enabled)
            if (!enabled) {
                prefs.saveNotificationPromptDismissed(false)
            }
            _status.value = "Push notification preference updated."
        }
    }

    fun setContactFilters(enabled: Boolean) {
        viewModelScope.launch {
            prefs.saveContactFilters(enabled)
            _status.value = "Contact filter preference updated."
        }
    }

    fun setPrivacy(photoPrivate: Boolean, visible: Boolean) {
        viewModelScope.launch {
            val photoPrivacy = if (photoPrivate) "request_only" else "all"
            val profileVisibility = if (visible) "all" else "hidden"
            prefs.savePhotoPrivacy(photoPrivacy)
            prefs.saveProfileVisibility(profileVisibility)
            val profileId = prefs.profileId.first()
            if (!profileId.isNullOrEmpty()) {
                runCatching {
                    profileApi.updatePrivacy(
                        profileId,
                        PrivacySettingsRequest(
                            photoPrivacy = photoPrivacy,
                            profileVisibility = profileVisibility
                        )
                    )
                }
            }
            _status.value = "Privacy settings saved."
        }
    }

    fun setContactPrivacy(masked: Boolean) {
        viewModelScope.launch {
            val next = if (masked) "masked" else "visible"
            val previous = _contactPrivacy.value
            _contactPrivacy.value = next
            val profileId = prefs.profileId.first()
            if (!profileId.isNullOrEmpty()) {
                val response = runCatching {
                    profileApi.updatePrivacy(
                        profileId,
                        PrivacySettingsRequest(
                            photoPrivacy = prefs.photoPrivacy.first(),
                            profileVisibility = prefs.profileVisibility.first(),
                            contactPrivacy = next
                        )
                    )
                }.getOrNull()
                if (response?.isSuccessful == true && response.body()?.success == true) {
                    _status.value = if (masked) {
                        "Contact details are private. Members can connect through chat."
                    } else {
                        "Eligible members can unlock your contact details."
                    }
                } else {
                    _contactPrivacy.value = previous
                    _status.value = response?.body()?.error?.message ?: "Could not update contact privacy."
                }
            }
        }
    }

    fun setProfileStatus(active: Boolean) {
        viewModelScope.launch {
            val previous = _profileStatus.value
            val next = if (active) "active" else "inactive"
            _profileStatus.value = next
            val response = runCatching { profileApi.updateProfileStatus(ProfileStatusRequest(next)) }.getOrNull()
            if (response?.isSuccessful == true && response.body()?.success == true) {
                _status.value = if (active) {
                    "Profile is active and eligible for search and matches."
                } else {
                    "Profile is inactive and hidden from search and matches."
                }
            } else {
                _profileStatus.value = previous
                _status.value = "Could not update profile status. Please try again."
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            val userId = prefs.userId.first()
            if (!userId.isNullOrBlank()) {
                runCatching { authApi.logout(mapOf("userId" to userId)) }
            }
            prefs.clearAll()
            _status.value = null
            onLoggedOut()
        }
    }

    fun showHiddenMemberAgain(profileId: String) {
        removedHiddenProfileIds += profileId
        ProfileInteractionStore.showProfileAgain(profileId)
        _hiddenMembers.value = _hiddenMembers.value.filterNot { it.profileId == profileId }
        _status.value = "This member can appear in your matches again."
    }

    fun unblockMember(profileId: String) {
        removedBlockedProfileIds += profileId
        ProfileInteractionStore.unblockProfile(profileId)
        _blockedMembers.value = _blockedMembers.value.filterNot { it.profileId == profileId }
        _status.value = "This member is unblocked."
    }

    fun updateConcern(profileId: String, concern: String) {
        ProfileInteractionStore.reportConcern(profileId, concern)
        _status.value = "Concern updated."
    }

    fun deleteConcern(profileId: String) {
        ProfileInteractionStore.deleteConcern(profileId)
        _status.value = "Concern deleted."
    }

    fun clearStatus() {
        _status.value = null
    }

    private fun memberForProfileId(profileId: String): PrivacyMemberUi {
        if (!AppEnvironment.allowDemoFallback) return PrivacyMemberUi(profileId, "Member", "Private profile")
        val summary = MarketFixtures.matchSeed(profileId)
        if (summary != null) {
            return PrivacyMemberUi(summary.profileId, summary.name, "${summary.location} | ${summary.community}")
        }
        val detail = MarketFixtures.profileDetails(profileId)
        return PrivacyMemberUi(
            detail.profileId,
            detail.firstName + " " + detail.lastName,
            "${detail.workingCity} | ${detail.religion}, ${detail.caste}"
        )
    }
}

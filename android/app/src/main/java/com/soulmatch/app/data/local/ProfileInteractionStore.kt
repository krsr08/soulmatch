package com.soulmatch.app.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ReportedConcern(
    val profileId: String,
    val concern: String,
    val updatedMillis: Long = System.currentTimeMillis()
)

data class ProfileInteractionState(
    val hiddenProfileIds: Set<String> = emptySet(),
    val blockedProfileIds: Set<String> = emptySet(),
    val reportedConcerns: Map<String, ReportedConcern> = emptyMap(),
    val acceptedInterestIds: Set<String> = emptySet(),
    val declinedInterestIds: Set<String> = emptySet(),
    val viewedProfileIds: Set<String> = emptySet(),
    val sentInterestProfileIds: Set<String> = emptySet(),
    val shortlistedProfileIds: Set<String> = emptySet(),
    val unshortlistedProfileIds: Set<String> = emptySet()
)

object ProfileInteractionStore {
    private val _state = MutableStateFlow(ProfileInteractionState())
    val state: StateFlow<ProfileInteractionState> = _state.asStateFlow()

    fun hideProfile(profileId: String) {
        if (profileId.isBlank()) return
        _state.update { it.copy(hiddenProfileIds = it.hiddenProfileIds + profileId) }
    }

    fun showProfileAgain(profileId: String) {
        _state.update { it.copy(hiddenProfileIds = it.hiddenProfileIds - profileId) }
    }

    fun blockProfile(profileId: String) {
        if (profileId.isBlank()) return
        _state.update { it.copy(blockedProfileIds = it.blockedProfileIds + profileId) }
    }

    fun unblockProfile(profileId: String) {
        _state.update { it.copy(blockedProfileIds = it.blockedProfileIds - profileId) }
    }

    fun reportConcern(profileId: String, concern: String) {
        val cleanConcern = concern.trim()
        if (profileId.isBlank() || cleanConcern.isBlank()) return
        _state.update {
            it.copy(
                reportedConcerns = it.reportedConcerns + (
                    profileId to ReportedConcern(profileId = profileId, concern = cleanConcern)
                )
            )
        }
    }

    fun deleteConcern(profileId: String) {
        _state.update { it.copy(reportedConcerns = it.reportedConcerns - profileId) }
    }

    fun markInterest(profileId: String) {
        if (profileId.isBlank()) return
        _state.update { it.copy(sentInterestProfileIds = it.sentInterestProfileIds + profileId) }
    }

    fun clearSentInterest(profileId: String) {
        if (profileId.isBlank()) return
        _state.update { it.copy(sentInterestProfileIds = it.sentInterestProfileIds - profileId) }
    }

    fun markViewed(profileId: String) {
        if (profileId.isBlank()) return
        _state.update { it.copy(viewedProfileIds = it.viewedProfileIds + profileId) }
    }

    fun setShortlisted(profileId: String, shortlisted: Boolean) {
        if (profileId.isBlank()) return
        _state.update {
            it.copy(
                shortlistedProfileIds = if (shortlisted) {
                    it.shortlistedProfileIds + profileId
                } else {
                    it.shortlistedProfileIds - profileId
                },
                unshortlistedProfileIds = if (shortlisted) {
                    it.unshortlistedProfileIds - profileId
                } else {
                    it.unshortlistedProfileIds + profileId
                }
            )
        }
    }

    fun respondToInterest(interestId: String, status: String) {
        if (interestId.isBlank()) return
        when (status.lowercase()) {
            "accepted" -> _state.update {
                it.copy(
                    acceptedInterestIds = it.acceptedInterestIds + interestId,
                    declinedInterestIds = it.declinedInterestIds - interestId
                )
            }
            "declined" -> _state.update {
                it.copy(
                    declinedInterestIds = it.declinedInterestIds + interestId,
                    acceptedInterestIds = it.acceptedInterestIds - interestId
                )
            }
        }
    }
}

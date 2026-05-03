package com.soulmatch.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmatch.app.data.api.ProfileApiService
import com.soulmatch.app.data.models.FamilyDecisionCommentRequest
import com.soulmatch.app.data.models.FamilyDecisionData
import com.soulmatch.app.data.models.FamilyDecisionRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FamilyDecisionBoardViewModel @Inject constructor(
    private val profileApi: ProfileApiService
) : ViewModel() {
    private val _decisions = MutableStateFlow<List<FamilyDecisionData>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _status = MutableStateFlow<String?>(null)

    val decisions: StateFlow<List<FamilyDecisionData>> = _decisions.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val status: StateFlow<String?> = _status.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            val response = runCatching { profileApi.getFamilyDecisions() }.getOrNull()
            val body = response?.body()
            if (response?.isSuccessful == true && body?.success == true) {
                _decisions.value = body.data.orEmpty()
                _status.value = null
            } else {
                _status.value = body?.error?.message ?: "Family board is empty or unavailable."
            }
            _isLoading.value = false
        }
    }

    fun updateDecision(decision: FamilyDecisionData, status: String) {
        viewModelScope.launch {
            val familyVote = when (status) {
                "accepted" -> "approve"
                "declined", "archived" -> "reject"
                else -> decision.familyVote
            }
            val response = runCatching {
                profileApi.upsertFamilyDecision(
                    decision.targetProfileId,
                    FamilyDecisionRequest(
                        status = status,
                        familyVote = familyVote,
                        note = decision.note,
                        nextStep = when (status) {
                            "call_scheduled" -> "Schedule family call"
                            "accepted" -> "Move to conversation"
                            "declined" -> "Archive after family discussion"
                            else -> decision.nextStep
                        }
                    )
                )
            }.getOrNull()
            val body = response?.body()
            if (response?.isSuccessful == true && body?.success == true) {
                _decisions.update { list ->
                    list.map { item ->
                        if (item.targetProfileId == decision.targetProfileId) item.copy(status = status, familyVote = familyVote) else item
                    }.filterNot { it.status.equals("archived", ignoreCase = true) }
                }
                _status.value = body.message ?: "Family decision updated."
            } else {
                _status.value = body?.error?.message ?: "Couldn't update the family decision."
            }
        }
    }

    fun submitFamilyInput(decision: FamilyDecisionData, vote: String, comment: String) {
        if (decision.familyDecisionId.isBlank()) {
            _status.value = "Open this profile once before adding family comments."
            return
        }
        viewModelScope.launch {
            val response = runCatching {
                profileApi.addFamilyDecisionComment(
                    decision.familyDecisionId,
                    FamilyDecisionCommentRequest(vote = vote, comment = comment.trim())
                )
            }.getOrNull()
            val body = response?.body()
            if (response?.isSuccessful == true && body?.success == true) {
                _status.value = body.message ?: "Family input recorded."
                load()
            } else {
                _status.value = body?.error?.message ?: "Couldn't save family input."
            }
        }
    }
}

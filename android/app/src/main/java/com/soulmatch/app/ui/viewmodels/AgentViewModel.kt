package com.soulmatch.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmatch.app.data.api.ProfileApiService
import com.soulmatch.app.data.models.AgentManagedProfileSummaryData
import com.soulmatch.app.data.models.AgentMembershipData
import com.soulmatch.app.data.models.AgentManagedProfileCreateRequest
import com.soulmatch.app.data.models.AgentOnboardingRequest
import com.soulmatch.app.data.models.AgentProfileData
import com.soulmatch.app.data.models.AgentProfileUpsertRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray

data class AgentUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val agentProfile: AgentProfileData? = null,
    val membership: AgentMembershipData? = null,
    val managedProfiles: List<AgentManagedProfileSummaryData> = emptyList(),
    val error: String? = null,
    val saveMessage: String? = null
)

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val profileApi: ProfileApiService
) : ViewModel() {
    private val _state = MutableStateFlow(AgentUiState(loading = true))
    val state: StateFlow<AgentUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, saveMessage = null)
            try {
                val profile = profileApi.getAgentProfile().body()?.takeIf { it.success }?.data
                val membership = profileApi.getAgentMembership().body()?.takeIf { it.success }?.data
                val managedProfiles = profileApi.getAgentManagedProfiles().body()?.takeIf { it.success }?.data.orEmpty()
                _state.value = AgentUiState(
                    loading = false,
                    agentProfile = profile,
                    membership = membership,
                    managedProfiles = managedProfiles
                )
            } catch (error: Exception) {
                _state.value = _state.value.copy(loading = false, error = error.message ?: "Unable to load agent workspace.")
            }
        }
    }

    fun submitOnboarding(request: AgentOnboardingRequest, onCompleted: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null, saveMessage = null)
            try {
                val response = profileApi.submitAgentOnboarding(request).body()
                if (response?.success == true) {
                    _state.value = _state.value.copy(saving = false, saveMessage = response.message ?: "Agent onboarding submitted.")
                    refresh()
                    onCompleted()
                } else {
                    _state.value = _state.value.copy(saving = false, error = response?.error?.message ?: "Could not submit agent onboarding.")
                }
            } catch (error: Exception) {
                _state.value = _state.value.copy(saving = false, error = error.message ?: "Could not submit agent onboarding.")
            }
        }
    }

    fun submitOnboardingWithDocuments(
        request: AgentOnboardingRequest,
        documents: List<MultipartBody.Part>,
        documentMeta: List<Map<String, String>>,
        onCompleted: () -> Unit
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null, saveMessage = null)
            try {
                val fields = linkedMapOf<String, RequestBody>(
                    "fullName" to request.fullName.asPlainPart(),
                    "phone" to request.phone.asPlainPart(),
                    "email" to request.email.asPlainPart(),
                    "city" to request.city.asPlainPart(),
                    "state" to request.state.asPlainPart(),
                    "businessName" to request.businessName.asPlainPart(),
                    "referralCode" to request.referralCode.asPlainPart(),
                    "serviceLabel" to request.serviceLabel.asPlainPart(),
                    "kycDocumentMeta" to JSONArray(documentMeta).toString().asPlainPart()
                )
                val response = profileApi.submitAgentOnboardingMultipart(fields, documents).body()
                if (response?.success == true) {
                    _state.value = _state.value.copy(saving = false, saveMessage = response.message ?: "Agent onboarding submitted.")
                    refresh()
                    onCompleted()
                } else {
                    _state.value = _state.value.copy(saving = false, error = response?.error?.message ?: "Could not submit agent onboarding.")
                }
            } catch (error: Exception) {
                _state.value = _state.value.copy(saving = false, error = error.message ?: "Could not submit agent onboarding.")
            }
        }
    }

    fun createManagedProfile(
        request: AgentManagedProfileCreateRequest,
        onCompleted: () -> Unit
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null, saveMessage = null)
            try {
                val response = profileApi.createAgentManagedProfile(request).body()
                if (response?.success == true && response.data != null) {
                    runCatching { profileApi.submitAgentManagedProfile(response.data.profileId) }
                    _state.value = _state.value.copy(saving = false, saveMessage = "Profile added to pending verification.")
                    refresh()
                    onCompleted()
                } else {
                    _state.value = _state.value.copy(saving = false, error = response?.error?.message ?: "Could not create member profile.")
                }
            } catch (error: Exception) {
                _state.value = _state.value.copy(saving = false, error = error.message ?: "Could not create member profile.")
            }
        }
    }

    fun saveCommissionPreferences(
        enabled: Boolean,
        verifiedProfileRate: String,
        successfulMatchRate: String,
        monthlyTarget: String,
        onCompleted: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null, saveMessage = null)
            try {
                val current = _state.value.agentProfile
                val request = AgentProfileUpsertRequest(
                    fullName = current?.fullName.orEmpty(),
                    email = current?.email.orEmpty(),
                    phone = current?.phone.orEmpty(),
                    city = current?.city.orEmpty(),
                    state = current?.state.orEmpty(),
                    pincode = current?.pincode.orEmpty(),
                    bio = current?.bio.orEmpty(),
                    serviceLabel = current?.serviceLabel ?: "SoulMatch Agent",
                    languages = current?.languages.orEmpty(),
                    communities = current?.communities.orEmpty(),
                    feePreferences = mapOf(
                        "enabled" to enabled.toString(),
                        "verifiedProfileRateInr" to verifiedProfileRate.trim(),
                        "successfulMatchRateInr" to successfulMatchRate.trim(),
                        "monthlyTargetInr" to monthlyTarget.trim()
                    )
                )
                val response = profileApi.upsertAgentProfile(request).body()
                if (response?.success == true) {
                    _state.value = _state.value.copy(saving = false, saveMessage = "Commission settings updated.")
                    refresh()
                    onCompleted?.invoke()
                } else {
                    _state.value = _state.value.copy(saving = false, error = response?.error?.message ?: "Could not save commission settings.")
                }
            } catch (error: Exception) {
                _state.value = _state.value.copy(saving = false, error = error.message ?: "Could not save commission settings.")
            }
        }
    }
}

private fun String.asPlainPart(): RequestBody = toRequestBody("text/plain".toMediaTypeOrNull())

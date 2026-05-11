package com.soulmatch.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmatch.app.data.api.ProfileApiService
import com.soulmatch.app.data.local.UserPreferences
import com.soulmatch.app.data.models.AgentManagedProfileSummaryData
import com.soulmatch.app.data.models.AgentMembershipData
import com.soulmatch.app.data.models.AgentManagedProfileCreateRequest
import com.soulmatch.app.data.models.AgentOnboardingRequest
import com.soulmatch.app.data.models.AgentProfileData
import com.soulmatch.app.data.models.AgentProfileUpsertRequest
import com.soulmatch.app.data.models.GenericResponse
import com.soulmatch.app.data.models.PaymentVerifyRequest
import com.soulmatch.app.data.payments.PaymentCoordinator
import com.soulmatch.app.data.payments.PaymentOutcome
import com.soulmatch.app.data.payments.PendingCheckout
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
import org.json.JSONObject
import retrofit2.Response

data class AgentUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val agentProfile: AgentProfileData? = null,
    val membership: AgentMembershipData? = null,
    val managedProfiles: List<AgentManagedProfileSummaryData> = emptyList(),
    val pennyCheckout: PendingCheckout? = null,
    val processingPennyDrop: Boolean = false,
    val error: String? = null,
    val saveMessage: String? = null
)

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val profileApi: ProfileApiService,
    private val userPreferences: UserPreferences,
    private val paymentCoordinator: PaymentCoordinator
) : ViewModel() {
    private val _state = MutableStateFlow(AgentUiState(loading = true))
    val state: StateFlow<AgentUiState> = _state.asStateFlow()

    init {
        observePaymentResults()
        refresh()
    }

    private fun observePaymentResults() {
        viewModelScope.launch {
            paymentCoordinator.results.collect { result ->
                when (result) {
                    is PaymentOutcome.Success -> {
                        if (result.order.planId == "agent_penny_drop") verifyPennyDropPayment(result)
                    }
                    is PaymentOutcome.Failure -> {
                        if (result.order?.planId == "agent_penny_drop") {
                            _state.value = _state.value.copy(
                                processingPennyDrop = false,
                                pennyCheckout = null,
                                error = result.message.ifBlank { "Bank verification payment was not completed." }
                            )
                        }
                    }
                }
            }
        }
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
                    "yearsExperience" to request.yearsExperience.toString().asPlainPart(),
                    "languages" to JSONArray(request.languages).toString().asPlainPart(),
                    "termsAccepted" to request.termsAccepted.toString().asPlainPart(),
                    "termsVersion" to request.termsVersion.asPlainPart(),
                    "kycDocumentMeta" to JSONArray(documentMeta).toString().asPlainPart(),
                    "kycDocuments" to JSONArray(request.kycDocuments.map {
                        mapOf(
                            "documentType" to it.documentType,
                            "documentSide" to it.documentSide,
                            "fileUrl" to it.fileUrl
                        )
                    }).toString().asPlainPart()
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

    fun saveAgentProfile(
        request: AgentProfileUpsertRequest,
        onCompleted: () -> Unit
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null, saveMessage = null)
            try {
                val response = profileApi.upsertAgentProfile(request).body()
                if (response?.success == true) {
                    _state.value = _state.value.copy(saving = false, saveMessage = "Agent profile updated.")
                    refresh()
                    onCompleted()
                } else {
                    _state.value = _state.value.copy(saving = false, error = response?.error?.message ?: "Could not update agent profile.")
                }
            } catch (error: Exception) {
                _state.value = _state.value.copy(saving = false, error = error.message ?: "Could not update agent profile.")
            }
        }
    }

    fun createManagedProfile(
        request: AgentManagedProfileCreateRequest,
        photoParts: List<MultipartBody.Part> = emptyList(),
        publishProfile: Boolean = true,
        onCompleted: () -> Unit
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null, saveMessage = null)
            try {
                val createResponse = profileApi.createAgentManagedProfile(request)
                val created = createResponse.body()
                if (createResponse.isSuccessful && created?.success == true && created.data != null) {
                    val profileId = created.data.profileId
                    if (photoParts.isNotEmpty()) {
                        val uploadResponse = profileApi.uploadPhotos(profileId, photoParts)
                        val uploadBody = uploadResponse.body()
                        if (!uploadResponse.isSuccessful || uploadBody?.success != true) {
                            _state.value = _state.value.copy(
                                saving = false,
                                error = extractErrorMessage(uploadResponse, "Profile saved, but photo upload failed.")
                            )
                            refresh()
                            return@launch
                        }
                    }
                    if (publishProfile) {
                        val submitResponse = profileApi.submitAgentManagedProfile(profileId)
                        val submitBody = submitResponse.body()
                        if (!submitResponse.isSuccessful || submitBody?.success != true) {
                            _state.value = _state.value.copy(
                                saving = false,
                                error = extractErrorMessage(submitResponse, "Profile saved, but submission failed.")
                            )
                            refresh()
                            return@launch
                        }
                    }
                    _state.value = _state.value.copy(
                        saving = false,
                        saveMessage = if (publishProfile) "Profile added to pending verification." else "Profile saved as draft."
                    )
                    refresh()
                    onCompleted()
                } else {
                    _state.value = _state.value.copy(
                        saving = false,
                        error = extractErrorMessage(createResponse, "Could not create member profile.")
                    )
                }
            } catch (error: Exception) {
                _state.value = _state.value.copy(saving = false, error = error.message ?: "Could not create member profile.")
            }
        }
    }

    fun saveCommissionPreferences(
        enabled: Boolean,
        status: String,
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
                    businessName = current?.businessName.orEmpty(),
                    referralCode = current?.referralCode.orEmpty(),
                    pincode = current?.pincode.orEmpty(),
                    bio = current?.bio.orEmpty(),
                    serviceLabel = current?.serviceLabel ?: "SoulMatch Agent",
                    status = status,
                    languages = current?.languages.orEmpty(),
                    communities = current?.communities.orEmpty(),
                    yearsExperience = current?.yearsExperience ?: 0,
                    feePreferences = mapOf(
                        "enabled" to enabled.toString(),
                        "matchSearchingRateInr" to verifiedProfileRate.trim(),
                        "marriageSettingRateInr" to successfulMatchRate.trim(),
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

    fun startPennyDropVerification() {
        viewModelScope.launch {
            _state.value = _state.value.copy(processingPennyDrop = true, error = null, saveMessage = null)
            try {
                val response = profileApi.createAgentPennyDropOrder()
                val order = response.body()?.takeIf { response.isSuccessful && it.success }?.data
                if (order == null) {
                    _state.value = _state.value.copy(
                        processingPennyDrop = false,
                        error = response.body()?.error?.message ?: "Could not start bank verification."
                    )
                    return@launch
                }
                val checkout = PendingCheckout(order = order, planName = "Agent bank verification")
                paymentCoordinator.registerCheckout(checkout)
                _state.value = _state.value.copy(pennyCheckout = checkout)
            } catch (error: Exception) {
                _state.value = _state.value.copy(processingPennyDrop = false, error = error.message ?: "Could not start bank verification.")
            }
        }
    }

    fun markPennyCheckoutConsumed() {
        _state.value = _state.value.copy(pennyCheckout = null)
    }

    fun failPennyCheckout(message: String) {
        viewModelScope.launch {
            paymentCoordinator.completeFailure(message)
        }
    }

    private fun verifyPennyDropPayment(result: PaymentOutcome.Success) {
        viewModelScope.launch {
            try {
                val response = profileApi.verifyAgentPennyDropPayment(
                    PaymentVerifyRequest(
                        orderId = result.order.orderId,
                        paymentId = result.paymentId,
                        signature = result.signature,
                        planId = result.order.planId
                    )
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    _state.value = _state.value.copy(
                        processingPennyDrop = false,
                        pennyCheckout = null,
                        saveMessage = "Bank access payment confirmed. Final verification is pending review.",
                        agentProfile = response.body()?.data ?: _state.value.agentProfile
                    )
                    refresh()
                } else {
                    _state.value = _state.value.copy(
                        processingPennyDrop = false,
                        pennyCheckout = null,
                        error = response.body()?.error?.message ?: "Bank verification payment could not be confirmed."
                    )
                }
            } catch (error: Exception) {
                _state.value = _state.value.copy(
                    processingPennyDrop = false,
                    pennyCheckout = null,
                    error = error.message ?: "Bank verification payment could not be confirmed."
                )
            }
        }
    }

    fun logout(onCompleted: () -> Unit) {
        viewModelScope.launch {
            userPreferences.clearAll()
            onCompleted()
        }
    }
}

private fun String.asPlainPart(): RequestBody = toRequestBody("text/plain".toMediaTypeOrNull())

private fun <T> extractErrorMessage(response: Response<GenericResponse<T>>, fallback: String): String {
    val body = response.body()
    body?.error?.message?.takeIf { it.isNotBlank() }?.let { return it }
    body?.message?.takeIf { it.isNotBlank() }?.let { return it }
    val raw = runCatching { response.errorBody()?.string().orEmpty() }.getOrDefault("")
    if (raw.isNotBlank()) {
        runCatching {
            val json = JSONObject(raw)
            json.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                ?: json.optString("message").takeIf { it.isNotBlank() }
        }.getOrNull()?.let { return it }
    }
    return fallback
}

package com.soulmatch.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmatch.app.data.api.InterestApiService
import com.soulmatch.app.data.api.ProfileApiService
import com.soulmatch.app.data.config.AppEnvironment
import com.soulmatch.app.data.local.ProfileInteractionStore
import com.soulmatch.app.data.mock.MarketFixtures
import com.soulmatch.app.data.models.InterestListItem
import com.soulmatch.app.data.models.RespondRequest
import com.soulmatch.app.data.models.ShortlistItem
import com.soulmatch.app.data.models.ViewerData
import com.soulmatch.app.data.realtime.InterestSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class InterestsViewModel @Inject constructor(
    private val interestApi: InterestApiService,
    private val profileApi: ProfileApiService,
    private val interestSyncManager: InterestSyncManager
) : ViewModel() {
    private val _received = MutableStateFlow<List<InterestListItem>>(emptyList())
    private val _sent = MutableStateFlow<List<InterestListItem>>(emptyList())
    private val _shortlisted = MutableStateFlow<List<ShortlistItem>>(emptyList())
    private val _viewers = MutableStateFlow<List<ViewerData>>(emptyList())
    private val _isLoading = MutableStateFlow(false)

    val received: StateFlow<List<InterestListItem>> = _received.asStateFlow()
    val sent: StateFlow<List<InterestListItem>> = _sent.asStateFlow()
    val shortlisted: StateFlow<List<ShortlistItem>> = _shortlisted.asStateFlow()
    val viewers: StateFlow<List<ViewerData>> = _viewers.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            interestSyncManager.changes.collect {
                load(showSpinner = false)
            }
        }
        viewModelScope.launch {
            ProfileInteractionStore.state.collect {
                _received.value = applyLocalStatuses(_received.value)
                _sent.value = mergeLocalSent(applyLocalStatuses(_sent.value))
                _shortlisted.value = mergeLocalShortlist(_shortlisted.value)
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(15_000)
                load(showSpinner = false)
            }
        }
    }

    fun load(showSpinner: Boolean = true) {
        if (_isLoading.value) return
        viewModelScope.launch {
            if (showSpinner) _isLoading.value = true
            try {
                val receivedResponse = runCatching { interestApi.getReceived() }.getOrNull()
                val receivedBody = receivedResponse?.body()
                _received.value = applyLocalStatuses(if (receivedResponse?.isSuccessful == true && receivedBody?.success == true) {
                    receivedBody.data.orEmpty().ifEmpty { if (AppEnvironment.allowDemoFallback) MarketFixtures.receivedInterests else emptyList() }
                } else {
                    if (AppEnvironment.allowDemoFallback) MarketFixtures.receivedInterests else emptyList()
                })

                val sentResponse = runCatching { interestApi.getSent() }.getOrNull()
                val sentBody = sentResponse?.body()
                _sent.value = mergeLocalSent(applyLocalStatuses(if (sentResponse?.isSuccessful == true && sentBody?.success == true) {
                    sentBody.data.orEmpty().ifEmpty { if (AppEnvironment.allowDemoFallback) MarketFixtures.sentInterests else emptyList() }
                } else {
                    if (AppEnvironment.allowDemoFallback) MarketFixtures.sentInterests else emptyList()
                }))

                val shortlistResponse = runCatching { interestApi.getShortlist() }.getOrNull()
                val shortlistBody = shortlistResponse?.body()
                _shortlisted.value = mergeLocalShortlist(if (shortlistResponse?.isSuccessful == true && shortlistBody?.success == true) {
                    shortlistBody.data.orEmpty().ifEmpty { if (AppEnvironment.allowDemoFallback) MarketFixtures.shortlistedProfiles else emptyList() }
                } else {
                    if (AppEnvironment.allowDemoFallback) MarketFixtures.shortlistedProfiles else emptyList()
                })

                val profileId = runCatching {
                    profileApi.getMyProfile().body()?.takeIf { it.success }?.data?.profileId
                }.getOrNull()
                _viewers.value = if (!profileId.isNullOrBlank()) {
                    runCatching { profileApi.getViewers(profileId) }
                        .getOrNull()
                        ?.body()
                        ?.takeIf { it.success }
                        ?.data
                        .orEmpty()
                        .ifEmpty { if (AppEnvironment.allowDemoFallback) MarketFixtures.recentViewers else emptyList() }
                } else {
                    if (AppEnvironment.allowDemoFallback) MarketFixtures.recentViewers else emptyList()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun respond(interestId: String, status: String) {
        viewModelScope.launch {
            val response = runCatching { interestApi.respond(interestId, RespondRequest(status)) }.getOrNull()
            val result = response?.body()?.takeIf { response.isSuccessful && it.success }?.data
            if (result?.status == status) {
                ProfileInteractionStore.respondToInterest(interestId, status)
                _received.value = _received.value.map { item ->
                    if (item.interestId == interestId) item.copy(status = status) else item
                }
                interestSyncManager.notifyChanged()
            }
        }
    }

    private fun applyLocalStatuses(items: List<InterestListItem>): List<InterestListItem> {
        val interactions = ProfileInteractionStore.state.value
        return items.map { item ->
            when (item.interestId) {
                in interactions.acceptedInterestIds -> item.copy(status = "accepted")
                in interactions.declinedInterestIds -> item.copy(status = "declined")
                else -> item
            }
        }
    }

    private fun mergeLocalSent(items: List<InterestListItem>): List<InterestListItem> {
        val interactions = ProfileInteractionStore.state.value
        val existingProfileIds = items.map { it.profileId }.toSet()
        if (!AppEnvironment.allowDemoFallback) return items
        val localItems = interactions.sentInterestProfileIds
            .filterNot { it in existingProfileIds }
            .mapNotNull { profileId ->
                val detail = runCatching { MarketFixtures.profileDetails(profileId) }.getOrNull() ?: return@mapNotNull null
                InterestListItem(
                    interestId = "local-sent-$profileId",
                    profileId = profileId,
                    userId = detail.userId,
                    firstName = detail.firstName,
                    lastName = detail.lastName,
                    primaryPhotoUrl = detail.primaryPhotoUrl,
                    occupation = detail.occupation,
                    workingCity = detail.workingCity,
                    familyCity = detail.familyCity,
                    status = "pending",
                    sentAt = Instant.now().toString()
                )
            }
        return localItems + items
    }

    private fun mergeLocalShortlist(items: List<ShortlistItem>): List<ShortlistItem> {
        val interactions = ProfileInteractionStore.state.value
        val visibleItems = items.filterNot { it.profileId in interactions.unshortlistedProfileIds }
        val existingProfileIds = visibleItems.map { it.profileId }.toSet()
        if (!AppEnvironment.allowDemoFallback) return visibleItems
        val localItems = interactions.shortlistedProfileIds
            .filterNot { it in existingProfileIds }
            .mapNotNull { profileId ->
                val detail = runCatching { MarketFixtures.profileDetails(profileId) }.getOrNull() ?: return@mapNotNull null
                ShortlistItem(
                    profileId = profileId,
                    userId = detail.userId,
                    firstName = detail.firstName,
                    lastName = detail.lastName,
                    primaryPhotoUrl = detail.primaryPhotoUrl,
                    addedAt = Instant.now().toString()
                )
            }
        return localItems + visibleItems
    }
}

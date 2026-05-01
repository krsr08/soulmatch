package com.soulmatch.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmatch.app.data.api.InterestApiService
import com.soulmatch.app.data.api.MatchingApiService
import com.soulmatch.app.data.api.ProfileApiService
import com.soulmatch.app.data.api.SearchApiService
import com.soulmatch.app.data.config.AppEnvironment
import com.soulmatch.app.data.local.ProfileInteractionStore
import com.soulmatch.app.data.mock.MarketFixtures
import com.soulmatch.app.data.models.InterestRequest
import com.soulmatch.app.data.models.ProfileSummary
import com.soulmatch.app.data.models.SearchRequest
import com.soulmatch.app.data.models.SavedSearchData
import com.soulmatch.app.data.models.toProfileSummary
import com.soulmatch.app.data.realtime.InterestSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoveryFilters(
    val ageMin: Int? = null,
    val ageMax: Int? = null,
    val religion: String = "Any",
    val city: String = "",
    val community: String = "Any",
    val motherTongue: String = "Any",
    val education: String = "Any",
    val occupation: String = "Any",
    val income: String = "Any",
    val diet: String = "Any",
    val familyType: String = "Any",
    val maritalStatus: String = "Any",
    val manglik: String = "Any",
    val verifiedOnly: Boolean = false,
    val highCompatibilityOnly: Boolean = false,
    val hasPhotoOnly: Boolean = false,
    val recentlyActiveOnly: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchApi: SearchApiService,
    private val matchingApi: MatchingApiService,
    private val profileApi: ProfileApiService,
    private val interestApi: InterestApiService,
    private val interestSyncManager: InterestSyncManager
) : ViewModel() {
    private val _catalog = MutableStateFlow<List<ProfileSummary>>(emptyList())
    private val _results = MutableStateFlow<List<ProfileSummary>>(emptyList())
    private val _filters = MutableStateFlow(DiscoveryFilters())
    private val _savedSearches = MutableStateFlow<List<SavedSearchData>>(emptyList())
    private val _isLoading = MutableStateFlow(false)

    val results: StateFlow<List<ProfileSummary>> = _results.asStateFlow()
    val filters: StateFlow<DiscoveryFilters> = _filters.asStateFlow()
    val savedSearches: StateFlow<List<SavedSearchData>> = _savedSearches.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadSeedData()
        viewModelScope.launch {
            interestSyncManager.changes.collect {
                _catalog.value = applyInteractionState(_catalog.value)
                _results.value = applyInteractionState(_results.value)
            }
        }
        viewModelScope.launch {
            ProfileInteractionStore.state.collect {
                _results.update { current -> current.filterVisibleProfiles() }
            }
        }
    }

    private fun currentRequest(): SearchRequest {
        return SearchRequest(
            ageMin = _filters.value.ageMin,
            ageMax = _filters.value.ageMax,
            religion = _filters.value.religion.takeUnless { it.equals("All", true) || it.equals("Any", true) },
            city = _filters.value.city.ifBlank { null },
            diet = _filters.value.diet.takeUnless { it == "Any" },
            community = _filters.value.community.takeUnless { it.equals("All", true) || it.equals("Any", true) },
            motherTongue = _filters.value.motherTongue.takeUnless { it.equals("All", true) || it.equals("Any", true) },
            education = _filters.value.education.takeUnless { it.equals("All", true) || it.equals("Any", true) },
            occupation = _filters.value.occupation.takeUnless { it.equals("All", true) || it.equals("Any", true) },
            income = _filters.value.income.takeUnless { it.equals("All", true) || it.equals("Any", true) },
            familyType = _filters.value.familyType.takeUnless { it.equals("All", true) || it.equals("Any", true) },
            maritalStatus = _filters.value.maritalStatus.takeUnless { it.equals("All", true) || it.equals("Any", true) },
            manglik = _filters.value.manglik.takeUnless { it.equals("All", true) || it.equals("Any", true) },
            verifiedOnly = _filters.value.verifiedOnly,
            photoOnly = _filters.value.hasPhotoOnly,
            recentlyActiveOnly = _filters.value.recentlyActiveOnly
        )
    }

    fun loadSeedData() {
        viewModelScope.launch {
            _isLoading.value = true
            val recommendedResponse = runCatching { matchingApi.getRecommended(page = 1, limit = 25) }.getOrNull()
            val recommendedBody = recommendedResponse?.body()
            val recommendedMatches = if (recommendedResponse?.isSuccessful == true && recommendedBody?.success == true) {
                recommendedBody.data?.matches.orEmpty()
            } else {
                emptyList()
            }.ifEmpty { if (AppEnvironment.allowDemoFallback) MarketFixtures.matches else emptyList() }
            _catalog.value = applyInteractionState(recommendedMatches)
            _results.value = _catalog.value.filterVisibleProfiles()
            val savedResponse = runCatching { searchApi.getSavedSearches() }.getOrNull()
            val savedBody = savedResponse?.body()
            _savedSearches.value = if (savedResponse?.isSuccessful == true && savedBody?.success == true) {
                savedBody.data.orEmpty()
            } else {
                emptyList()
            }.ifEmpty { if (AppEnvironment.allowDemoFallback) MarketFixtures.savedSearches else emptyList() }
            _isLoading.value = false
        }
    }

    fun updateFilters(transform: DiscoveryFilters.() -> DiscoveryFilters) {
        _filters.update { it.transform() }
    }

    fun applyFilters() {
        viewModelScope.launch {
            _isLoading.value = true
            val seedMap = _catalog.value.associateBy { it.profileId }
            val searchResponse = runCatching { searchApi.advancedSearch(currentRequest()) }.getOrNull()
            val searchBody = searchResponse?.body()
            val remoteResults = if (searchResponse?.isSuccessful == true && searchBody?.success == true) {
                searchBody.data?.results?.map { result -> result.toProfileSummary(seedMap[result.profileId]) }.orEmpty()
            } else {
                emptyList()
            }.ifEmpty { if (AppEnvironment.allowDemoFallback) MarketFixtures.search(currentRequest()) else emptyList() }
            val base = applyInteractionState(remoteResults)
            _results.value = base.filterVisibleProfiles().filter { match -> matchesRichFilters(match) }
            _isLoading.value = false
        }
    }

    fun clearFilters() {
        _filters.value = DiscoveryFilters()
        _results.value = _catalog.value.filterVisibleProfiles()
    }

    fun applySavedSearch(savedSearch: SavedSearchData) {
        _filters.value = DiscoveryFilters(
            ageMin = savedSearch.ageMin,
            ageMax = savedSearch.ageMax,
            religion = savedSearch.religion ?: "Any",
            city = savedSearch.city.orEmpty()
        )
        applyFilters()
    }

    fun saveCurrentSearch() {
        viewModelScope.launch {
            val saved = runCatching { searchApi.saveSearch(currentRequest()) }
                .getOrNull()
                ?.body()
                ?.takeIf { it.success }
                ?.data
                ?: return@launch
            _savedSearches.update { existing ->
                if (existing.any { it.searchId == saved.searchId }) existing else listOf(saved) + existing
            }
        }
    }

    fun sendInterest(profileId: String) {
        viewModelScope.launch {
            val response = runCatching { interestApi.sendInterest(InterestRequest(profileId)) }.getOrNull()
            val result = response?.body()?.takeIf { response.isSuccessful && it.success }?.data
            if (result?.status == "interest_sent" || result?.status == "interest_resent" || result?.status == "already_sent") {
                ProfileInteractionStore.markInterest(profileId)
                updateProfileState(profileId) { copy(interestSent = true) }
                interestSyncManager.notifyChanged()
            }
        }
    }

    fun toggleShortlist(profileId: String) {
        viewModelScope.launch {
            val response = runCatching { interestApi.toggleShortlist(profileId) }.getOrNull()
            val action = response?.body()?.takeIf { response.isSuccessful && it.success }?.data?.action
            val nextShortlisted = action?.let { it == "added" } ?: return@launch
            updateProfileState(profileId) {
                val resolved = nextShortlisted ?: !shortlisted
                ProfileInteractionStore.setShortlisted(profileId, resolved)
                copy(shortlisted = resolved)
            }
            interestSyncManager.notifyChanged()
        }
    }

    fun hideProfile(profileId: String) {
        ProfileInteractionStore.hideProfile(profileId)
        _results.update { it.filterVisibleProfiles() }
    }

    fun blockProfile(profileId: String) {
        viewModelScope.launch {
            val response = runCatching { profileApi.blockProfile(profileId) }.getOrNull()
            if (response?.isSuccessful == true && response.body()?.success == true) {
                ProfileInteractionStore.blockProfile(profileId)
                _results.update { it.filterVisibleProfiles() }
            }
        }
    }

    fun reportProfile(profileId: String, concern: String) {
        viewModelScope.launch {
            val response = runCatching { profileApi.reportProfile(profileId, mapOf("reason" to "member_report", "description" to concern)) }.getOrNull()
            if (response?.isSuccessful == true && response.body()?.success == true) {
                ProfileInteractionStore.reportConcern(profileId, concern)
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

    private fun updateProfileState(profileId: String, transform: ProfileSummary.() -> ProfileSummary) {
        _catalog.update { list ->
            list.map { profile ->
                if (profile.profileId == profileId) profile.transform() else profile
            }
        }
        _results.update { list ->
            list.map { profile ->
                if (profile.profileId == profileId) profile.transform() else profile
            }
        }
    }

    private fun List<ProfileSummary>.filterVisibleProfiles(): List<ProfileSummary> {
        val interactions = ProfileInteractionStore.state.value
        return filterNot { it.profileId in interactions.hiddenProfileIds || it.profileId in interactions.blockedProfileIds }
    }

    private fun matchesRichFilters(match: ProfileSummary): Boolean {
        val filters = _filters.value
        val fixtureDetail = if (AppEnvironment.allowDemoFallback) MarketFixtures.matchSeed(match.profileId)?.let { MarketFixtures.profileDetails(match.profileId) } else null

        fun selected(value: String): Boolean = value.isNotBlank() && !value.equals("Any", true) && !value.equals("All", true)
        fun containsAny(source: String, expected: String): Boolean = source.contains(expected, ignoreCase = true)

        val communitySource = listOf(match.community, fixtureDetail?.caste.orEmpty(), fixtureDetail?.religion.orEmpty()).joinToString(" ")
        val occupationSource = listOf(match.occupation, fixtureDetail?.occupation.orEmpty()).joinToString(" ")
        val educationSource = listOf(match.education, fixtureDetail?.educationLevel.orEmpty()).joinToString(" ")

        return (!filters.verifiedOnly || match.isVerified) &&
            (!filters.highCompatibilityOnly || match.compatibilityScore >= 90) &&
            (!filters.hasPhotoOnly || !match.primaryPhoto.isNullOrBlank()) &&
            (!filters.recentlyActiveOnly || match.lastActiveLabel.contains("active", true) || match.lastActiveLabel.contains("new", true)) &&
            (!selected(filters.community) || containsAny(communitySource, filters.community)) &&
            (!selected(filters.motherTongue) || fixtureDetail == null || containsAny(fixtureDetail.motherTongue, filters.motherTongue)) &&
            (!selected(filters.education) || containsAny(educationSource, filters.education)) &&
            (!selected(filters.occupation) || containsAny(occupationSource, filters.occupation)) &&
            (!selected(filters.income) || fixtureDetail == null || containsAny(fixtureDetail.annualIncome, filters.income)) &&
            (!selected(filters.familyType) || fixtureDetail == null || containsAny(fixtureDetail.familyType, filters.familyType)) &&
            (!selected(filters.maritalStatus) || fixtureDetail == null || containsAny(fixtureDetail.maritalStatus, filters.maritalStatus)) &&
            (!selected(filters.manglik) || when {
                fixtureDetail == null -> true
                filters.manglik.equals("yes", true) -> fixtureDetail.isManglik
                filters.manglik.equals("no", true) -> !fixtureDetail.isManglik
                else -> true
            })
    }
}

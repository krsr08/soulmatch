package com.soulmatch.app.ui.screens.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.data.models.SubscriptionData
import com.soulmatch.app.data.models.SavedSearchData
import com.soulmatch.app.ui.components.premium.ChipTone
import com.soulmatch.app.ui.components.premium.FilterChoiceChip
import com.soulmatch.app.ui.components.premium.MetricPill
import com.soulmatch.app.ui.components.premium.PremiumCard
import com.soulmatch.app.ui.components.premium.PremiumHeader
import com.soulmatch.app.ui.components.premium.PremiumScreen
import com.soulmatch.app.ui.components.cards.ProfileCard
import com.soulmatch.app.ui.components.premium.SectionTitle
import com.soulmatch.app.ui.components.premium.SignalChip
import com.soulmatch.app.ui.components.premium.SignalChips
import com.soulmatch.app.ui.components.premium.UpgradePlanGate
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.Info
import com.soulmatch.app.ui.theme.InfoSoft
import com.soulmatch.app.ui.theme.PrimaryDark
import com.soulmatch.app.ui.theme.Success
import com.soulmatch.app.ui.theme.SuccessSoft
import com.soulmatch.app.ui.theme.SurfaceSoft
import com.soulmatch.app.ui.theme.SurfaceWarm
import com.soulmatch.app.ui.theme.TextSecondary
import com.soulmatch.app.ui.titleCase
import com.soulmatch.app.ui.viewmodels.DiscoveryFilters
import com.soulmatch.app.ui.viewmodels.SearchViewModel
import com.soulmatch.app.ui.viewmodels.SubscriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit = {},
    onViewProfile: ((String) -> Unit)? = null,
    onOpenChat: ((String, String) -> Unit)? = null,
    onSubscribe: (() -> Unit)? = null,
    onEditSection: ((Int) -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    profileId: String = "",
    chatId: String = "",
    participantName: String = "",
    vm: SearchViewModel = hiltViewModel(),
    subscriptionVm: SubscriptionViewModel = hiltViewModel()
) {
    val results by vm.results.collectAsStateWithLifecycle()
    val filters by vm.filters.collectAsStateWithLifecycle()
    val savedSearches by vm.savedSearches.collectAsStateWithLifecycle()
    val loading by vm.isLoading.collectAsStateWithLifecycle()
    val subscription by subscriptionVm.subscription.collectAsStateWithLifecycle()
    var minAge by remember(filters.ageMin) { mutableFloatStateOf((filters.ageMin ?: 21).toFloat()) }
    var maxAge by remember(filters.ageMax) { mutableFloatStateOf((filters.ageMax ?: 45).toFloat()) }
    val viewProfile: (String) -> Unit = onViewProfile ?: { _ -> }
    val openSubscription: (() -> Unit)? = onSubscribe
    val hasActiveMembership = subscription.hasActivePaidMembership()
    var showFilters by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        subscriptionVm.load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Smart Search", fontWeight = FontWeight.Bold)
                        Text("Filter by family, lifestyle, activity, and compatibility", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { padding ->
        PremiumScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    PremiumHeader(
                        eyebrow = "Discovery",
                        title = "Find serious matches faster",
                        subtitle = "Use saved bundles for speed, then refine with details that matter in matrimony."
                    )
                }
                item {
                    SearchSummaryPanel(
                        filters = filters,
                        resultCount = results.size,
                        expanded = showFilters,
                        onToggle = { showFilters = !showFilters }
                    )
                }
                if (showFilters) {
                    item {
                        SearchFilterPanel(
                            filters = filters,
                            minAge = minAge,
                            maxAge = maxAge,
                            resultCount = results.size,
                            savedSearches = savedSearches,
                            onAgeChanged = { start, end ->
                                minAge = start
                                maxAge = end
                                vm.updateFilters {
                                    copy(ageMin = start.toInt(), ageMax = end.toInt())
                                }
                            },
                            onReligionSelected = { religion -> vm.updateFilters { copy(religion = religion) } },
                            onCommunitySelected = { community -> vm.updateFilters { copy(community = community) } },
                            onMotherTongueSelected = { motherTongue -> vm.updateFilters { copy(motherTongue = motherTongue) } },
                            onEducationSelected = { education -> vm.updateFilters { copy(education = education) } },
                            onOccupationSelected = { occupation -> vm.updateFilters { copy(occupation = occupation) } },
                            onIncomeSelected = { income -> vm.updateFilters { copy(income = income) } },
                            onDietSelected = { diet -> vm.updateFilters { copy(diet = diet) } },
                            onFamilyTypeSelected = { familyType -> vm.updateFilters { copy(familyType = familyType) } },
                            onMaritalStatusSelected = { maritalStatus -> vm.updateFilters { copy(maritalStatus = maritalStatus) } },
                            onManglikSelected = { manglik -> vm.updateFilters { copy(manglik = manglik) } },
                            onCityChanged = { city -> vm.updateFilters { copy(city = city) } },
                            onToggleVerified = { vm.updateFilters { copy(verifiedOnly = !verifiedOnly) } },
                            onToggleHighMatch = { vm.updateFilters { copy(highCompatibilityOnly = !highCompatibilityOnly) } },
                            onToggleHasPhoto = { vm.updateFilters { copy(hasPhotoOnly = !hasPhotoOnly) } },
                            onToggleRecentlyActive = { vm.updateFilters { copy(recentlyActiveOnly = !recentlyActiveOnly) } },
                            onSavedSearch = { vm.applySavedSearch(it) },
                            onSaveSearch = { vm.saveCurrentSearch() },
                            onApply = {
                                vm.applyFilters()
                                showFilters = false
                            },
                            onClear = {
                                minAge = 21f
                                maxAge = 45f
                                vm.clearFilters()
                            }
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionTitle(title = "Results", subtitle = "Sorted by compatibility and activity", modifier = Modifier.weight(1f))
                        MetricPill(label = "Profiles", value = results.size.toString(), background = SurfaceSoft)
                    }
                }
                if (loading && results.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else {
                    itemsIndexed(results, key = { _, profile -> profile.profileId }) { index, profile ->
                        ProfileCard(
                            profile = profile,
                            onSendInterest = { vm.sendInterest(it) },
                            onViewProfile = viewProfile,
                            onShortlist = { vm.toggleShortlist(it) }
                        )
                        if (!hasActiveMembership && (index + 1) % 5 == 0 && index != results.lastIndex) {
                            UpgradePlanGate(
                                title = "Upgrade for advanced discovery",
                                detail = "Unlock contact views, profile highlights, and richer match signals while searching.",
                                onUpgrade = openSubscription,
                                compact = true,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                if (!loading && results.isEmpty()) {
                    item {
                        PremiumCard(modifier = Modifier.padding(16.dp), containerColor = SurfaceWarm) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("No profiles matched this exact filter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Widen age, city, religion, or diet preferences to bring in more active members.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Button(onClick = { vm.clearFilters() }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Reset filters")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchSummaryPanel(
    filters: DiscoveryFilters,
    resultCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                    Text("Filters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("$resultCount profiles match your current choices", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Button(onClick = onToggle, modifier = Modifier.height(40.dp)) {
                    Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(if (expanded) "Close" else "Filters")
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    filters.ageMin?.let { min -> filters.ageMax?.let { max -> "Age: $min-$max yrs" } } ?: "Age: Any",
                    filters.city.takeIf { it.isNotBlank() }?.let { "Location: $it" } ?: "Location: Any",
                    if (filters.community.equals("Any", true)) "Community: Any" else "Community: ${titleCase(filters.community)}",
                    if (filters.religion.equals("Any", true)) "Religion: Any" else "Religion: ${titleCase(filters.religion)}"
                ).forEach { label ->
                    SignalChip(label, tone = ChipTone.Neutral)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SearchFilterPanel(
    filters: DiscoveryFilters,
    minAge: Float,
    maxAge: Float,
    resultCount: Int,
    savedSearches: List<SavedSearchData>,
    onAgeChanged: (Float, Float) -> Unit,
    onReligionSelected: (String) -> Unit,
    onCommunitySelected: (String) -> Unit,
    onMotherTongueSelected: (String) -> Unit,
    onEducationSelected: (String) -> Unit,
    onOccupationSelected: (String) -> Unit,
    onIncomeSelected: (String) -> Unit,
    onDietSelected: (String) -> Unit,
    onFamilyTypeSelected: (String) -> Unit,
    onMaritalStatusSelected: (String) -> Unit,
    onManglikSelected: (String) -> Unit,
    onCityChanged: (String) -> Unit,
    onToggleVerified: () -> Unit,
    onToggleHighMatch: () -> Unit,
    onToggleHasPhoto: () -> Unit,
    onToggleRecentlyActive: () -> Unit,
    onSavedSearch: (SavedSearchData) -> Unit,
    onSaveSearch: () -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit
) {
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = SurfaceSoft,
                border = BorderStroke(1.dp, Divider)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Location, profession, community", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Use filters below to preview matching profiles", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Icon(Icons.Filled.Tune, contentDescription = null, tint = TextSecondary)
                }
            }

            SectionTitle("Saved searches", "Reusable bundles for high-intent browsing")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (savedSearches.isEmpty()) {
                    listOf(
                        "Bangalore | 27-33 | Verified",
                        "Vegetarian | High compatibility",
                        "Parents involved | Active recently"
                    ).forEach { label ->
                        SignalChip(label, tone = ChipTone.Warm)
                    }
                } else {
                    savedSearches.take(4).forEach { search ->
                        FilterChoiceChip(
                            label = search.label.ifBlank { fallbackLabel(search) },
                            selected = false,
                            onClick = { onSavedSearch(search) }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Age range", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        if (filters.ageMin == null && filters.ageMax == null) "Any age" else "${minAge.toInt()} to ${maxAge.toInt()} yrs",
                        style = MaterialTheme.typography.titleSmall,
                        color = PrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                }
                RangeSlider(
                    value = minAge..maxAge,
                    onValueChange = { range -> onAgeChanged(range.start, range.endInclusive) },
                    valueRange = 21f..45f
                )
            }

            OutlinedTextField(
                value = filters.city,
                onValueChange = onCityChanged,
                label = { Text("Preferred city") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            FilterGroup(
                title = "Religion",
                options = listOf("Any", "Hindu", "Muslim", "Christian", "Sikh", "Jain"),
                selected = filters.religion,
                onSelected = onReligionSelected
            )
            FilterGroup(
                title = "Caste / community",
                options = listOf("Any", "Brahmin", "Iyer", "Reddy", "Nair", "Maratha", "Gujarati", "Punjabi", "Odia", "Jain"),
                selected = filters.community,
                onSelected = onCommunitySelected
            )
            FilterGroup(
                title = "Mother tongue",
                options = listOf("Any", "Telugu", "Tamil", "Malayalam", "Hindi", "Punjabi", "Gujarati", "Odia", "Marathi"),
                selected = filters.motherTongue,
                onSelected = onMotherTongueSelected
            )
            FilterGroup(
                title = "Education",
                options = listOf("Any", "Graduate", "Post Graduate", "MBA", "Doctorate", "Professional"),
                selected = filters.education,
                onSelected = onEducationSelected
            )
            FilterGroup(
                title = "Occupation",
                options = listOf("Any", "Software", "Doctor", "Product", "Founder", "Architect", "CA", "Teacher"),
                selected = filters.occupation,
                onSelected = onOccupationSelected
            )
            FilterGroup(
                title = "Income",
                options = listOf("Any", "5-10 LPA", "10-15 LPA", "15-20 LPA", "20-30 LPA", "35+ LPA"),
                selected = filters.income,
                onSelected = onIncomeSelected
            )
            FilterGroup(
                title = "Diet",
                options = listOf("Any", "vegetarian", "jain", "eggetarian", "non_vegetarian"),
                selected = filters.diet,
                onSelected = onDietSelected
            )
            FilterGroup(
                title = "Family type",
                options = listOf("Any", "Nuclear", "Joint"),
                selected = filters.familyType,
                onSelected = onFamilyTypeSelected
            )
            FilterGroup(
                title = "Marital status",
                options = listOf("Any", "never_married", "divorced", "widowed"),
                selected = filters.maritalStatus,
                onSelected = onMaritalStatusSelected
            )
            FilterGroup(
                title = "Manglik",
                options = listOf("Any", "yes", "no"),
                selected = filters.manglik,
                onSelected = onManglikSelected
            )

            SectionTitle("Quick toggles", "Trust and match-quality filters")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ToggleCard(
                    title = "Verified only",
                    detail = "ID or phone trust signal",
                    selected = filters.verifiedOnly,
                    onClick = onToggleVerified,
                    modifier = Modifier.weight(1f)
                )
                ToggleCard(
                    title = "High compatibility",
                    detail = "90%+ profile fit",
                    selected = filters.highCompatibilityOnly,
                    onClick = onToggleHighMatch,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ToggleCard(
                    title = "Has photo",
                    detail = "Prioritize visible profiles",
                    selected = filters.hasPhotoOnly,
                    onClick = onToggleHasPhoto,
                    modifier = Modifier.weight(1f)
                )
                ToggleCard(
                    title = "Recently active",
                    detail = "Fresh intent signals",
                    selected = filters.recentlyActiveOnly,
                    onClick = onToggleRecentlyActive,
                    modifier = Modifier.weight(1f)
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceWarm,
                border = BorderStroke(1.dp, Divider)
            ) {
                Text(
                    "Previewing $resultCount profiles with the current filters",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = PrimaryDark,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onApply,
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Apply filters")
                }
                Button(
                    onClick = onClear,
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = InfoSoft, contentColor = Info)
                ) {
                    Text("Reset filters")
                }
            }
            TextButton(onClick = onSaveSearch, modifier = Modifier.fillMaxWidth()) {
                Text("Save this search")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(
    title: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChoiceChip(
                    label = filterOptionLabel(title, option),
                    selected = selected.equals(option, ignoreCase = true),
                    onClick = { onSelected(option) }
                )
            }
        }
    }
}

@Composable
private fun ToggleCard(
    title: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(104.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) SuccessSoft else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) Success else Divider),
        onClick = onClick
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (selected) Success else MaterialTheme.colorScheme.onSurface)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

private fun fallbackLabel(search: SavedSearchData): String {
    val faith = search.religion ?: "Smart"
    val ageBand = listOfNotNull(search.ageMin, search.ageMax).joinToString("-")
    return "$faith $ageBand".trim()
}

private fun filterOptionLabel(title: String, option: String): String {
    if (!option.equals("Any", ignoreCase = true)) return titleCase(option)
    return when {
        title.contains("community", ignoreCase = true) -> "Any community"
        title.contains("tongue", ignoreCase = true) -> "Any language"
        title.contains("status", ignoreCase = true) -> "Any status"
        title.contains("family", ignoreCase = true) -> "Any family type"
        title.contains("manglik", ignoreCase = true) -> "Any horoscope"
        else -> "Any ${title.lowercase()}"
    }
}

private fun SubscriptionData.hasActivePaidMembership(): Boolean {
    return isActive && planId.isNotBlank() && !planId.equals("free", ignoreCase = true)
}

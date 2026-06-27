package com.soulmatch.app.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.data.models.ProfileSummary
import com.soulmatch.app.data.models.SubscriptionData
import com.soulmatch.app.ui.components.premium.PremiumCard
import com.soulmatch.app.ui.components.premium.PremiumScreen
import com.soulmatch.app.ui.components.cards.ProfileCard
import com.soulmatch.app.ui.components.premium.UpgradePlanGate
import com.soulmatch.app.ui.design.SoulMatchTokens
import com.soulmatch.app.ui.viewmodels.DashboardViewModel
import com.soulmatch.app.ui.viewmodels.SubscriptionViewModel

private enum class BestMatchFilter(val label: String) {
    All("All"),
    NinetyPlus("90%+"),
    Verified("Verified"),
    Nearby("Nearby"),
    New("New")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BestMatchesScreen(
    onBack: () -> Unit = {},
    onViewProfile: (String) -> Unit,
    onSubscribe: () -> Unit = {},
    vm: DashboardViewModel = hiltViewModel(),
    subscriptionVm: SubscriptionViewModel = hiltViewModel()
) {
    val matches by vm.matches.collectAsStateWithLifecycle()
    val myProfile by vm.myProfile.collectAsStateWithLifecycle()
    val loading by vm.isLoading.collectAsStateWithLifecycle()
    val subscription by subscriptionVm.subscription.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf(BestMatchFilter.All) }
    val hasActiveMembership = subscription.hasActivePaidMembership()
    val currentCity = myProfile.workingCity.ifBlank { myProfile.familyCity }
    val rankedMatches = remember(matches, filter, currentCity) {
        matches
            .asSequence()
            .filter { profile ->
                when (filter) {
                    BestMatchFilter.All -> true
                    BestMatchFilter.NinetyPlus -> profile.compatibilityScore >= 90
                    BestMatchFilter.Verified -> profile.isVerified
                    BestMatchFilter.Nearby -> profile.location.equals(currentCity, ignoreCase = true)
                    BestMatchFilter.New -> profile.lastActiveLabel.contains("new", ignoreCase = true)
                }
            }
            .sortedWith(
                compareByDescending<ProfileSummary> { it.compatibilityScore }
                    .thenByDescending { it.isVerified }
                    .thenBy { it.name }
            )
            .toList()
    }

    LaunchedEffect(Unit) {
        subscriptionVm.load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Best Matches", fontWeight = FontWeight.ExtraBold, color = SoulMatchTokens.Text)
                        Text(
                            "${rankedMatches.size} profiles sorted by match %",
                            style = MaterialTheme.typography.bodySmall,
                            color = SoulMatchTokens.Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color.White
    ) { padding ->
        PremiumScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (loading && matches.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        BestMatchesControlPanel(
                            selected = filter,
                            totalCount = matches.size,
                            visibleCount = rankedMatches.size,
                            onSelected = { selectedFilter ->
                                filter = selectedFilter
                                vm.setVerifiedOnlyMode(selectedFilter == BestMatchFilter.Verified)
                            }
                        )
                    }
                    if (rankedMatches.isEmpty()) {
                        item {
                            PremiumCard(modifier = Modifier.padding(16.dp), containerColor = SoulMatchTokens.TangerineSoft) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("No profiles in this filter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        if (filter == BestMatchFilter.Verified) {
                                            "No admin-verified active profiles are available for your current discovery pool yet."
                                        } else {
                                            "Try All, Verified, or 90%+ to continue browsing ranked matches."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SoulMatchTokens.Muted
                                    )
                                }
                            }
                        }
                    } else {
                        itemsIndexed(rankedMatches, key = { _, profile -> profile.profileId }) { index, profile ->
                            ProfileCard(
                                profile = profile,
                                onSendInterest = { vm.sendInterest(it) },
                                onViewProfile = onViewProfile,
                                onShortlist = { vm.toggleShortlist(it) }
                            )
                            if (!hasActiveMembership && (index + 1) % 5 == 0 && index != rankedMatches.lastIndex) {
                                UpgradePlanGate(
                                    title = "Upgrade for more match actions",
                                    detail = "Unlock contact views, profile highlights, and stronger visibility while browsing.",
                                    actionLabel = "Upgrade",
                                    onUpgrade = onSubscribe,
                                    compact = true,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun SubscriptionData.hasActivePaidMembership(): Boolean {
    return isActive && planId.isNotBlank() && !planId.equals("free", ignoreCase = true)
}

@Composable
private fun BestMatchesControlPanel(
    selected: BestMatchFilter,
    totalCount: Int,
    visibleCount: Int,
    onSelected: (BestMatchFilter) -> Unit
) {
    PremiumCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        containerColor = SoulMatchTokens.Card,
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("$visibleCount profiles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = SoulMatchTokens.Text)
                    Text("From $totalCount recommended matches", style = MaterialTheme.typography.bodySmall, color = SoulMatchTokens.Muted)
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = SoulMatchTokens.Ivory,
                    border = BorderStroke(1.dp, SoulMatchTokens.Border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Sort, contentDescription = null, modifier = Modifier.size(16.dp), tint = SoulMatchTokens.Tangerine)
                        Text("Match %", style = MaterialTheme.typography.labelSmall, color = SoulMatchTokens.Tangerine, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(BestMatchFilter.entries, key = { it.name }) { option ->
                    MatchFilterPill(
                        label = option.label,
                        selected = selected == option,
                        onClick = { onSelected(option) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchFilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) SoulMatchTokens.TangerineSoft else MaterialTheme.colorScheme.surface
    val content = if (selected) SoulMatchTokens.Tangerine else SoulMatchTokens.Muted
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = background,
        border = BorderStroke(1.dp, if (selected) SoulMatchTokens.Tangerine.copy(alpha = 0.25f) else SoulMatchTokens.Border)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = content,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

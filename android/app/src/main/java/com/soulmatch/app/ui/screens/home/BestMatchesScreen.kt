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
import com.soulmatch.app.data.mock.MarketFixtures
import com.soulmatch.app.data.models.ProfileSummary
import com.soulmatch.app.ui.components.PremiumCard
import com.soulmatch.app.ui.components.PremiumScreen
import com.soulmatch.app.ui.components.ProfileCard
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.SurfaceSoft
import com.soulmatch.app.ui.theme.SurfaceWarm
import com.soulmatch.app.ui.theme.TextSecondary
import com.soulmatch.app.ui.viewmodels.DashboardViewModel

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
    vm: DashboardViewModel = hiltViewModel()
) {
    val matches by vm.matches.collectAsStateWithLifecycle()
    val loading by vm.isLoading.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf(BestMatchFilter.All) }
    val currentCity = MarketFixtures.myProfile.workingCity
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Best Matches", fontWeight = FontWeight.ExtraBold)
                        Text(
                            "${rankedMatches.size} profiles sorted by match %",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
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
        containerColor = Color(0xFFFFF9F2)
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
                            onSelected = { filter = it }
                        )
                    }
                    if (rankedMatches.isEmpty()) {
                        item {
                            PremiumCard(modifier = Modifier.padding(16.dp), containerColor = SurfaceWarm) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("No profiles in this filter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Try All, Verified, or 90%+ to continue browsing ranked matches.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                            }
                        }
                    } else {
                        items(rankedMatches, key = { it.profileId }) { profile ->
                            ProfileCard(
                                profile = profile,
                                onSendInterest = { vm.sendInterest(it) },
                                onViewProfile = onViewProfile,
                                onShortlist = { vm.toggleShortlist(it) }
                            )
                        }
                    }
                }
            }
        }
    }
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
        containerColor = MaterialTheme.colorScheme.surface,
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("$visibleCount profiles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Text("From $totalCount recommended matches", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = SurfaceSoft,
                    border = BorderStroke(1.dp, Divider)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Sort, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Match %", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
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
    val background = if (selected) Color(0xFFFFD9DE) else MaterialTheme.colorScheme.surface
    val content = if (selected) MaterialTheme.colorScheme.primary else TextSecondary
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = background,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Divider)
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

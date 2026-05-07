package com.soulmatch.app.ui.screens.success

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soulmatch.app.ui.components.ChipTone
import com.soulmatch.app.ui.components.PremiumCard
import com.soulmatch.app.ui.components.PremiumHeader
import com.soulmatch.app.ui.components.PremiumScreen
import com.soulmatch.app.ui.components.SectionTitle
import com.soulmatch.app.ui.components.SignalChips
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.Success
import com.soulmatch.app.ui.theme.SuccessSoft
import com.soulmatch.app.ui.theme.SurfaceWarm
import com.soulmatch.app.ui.theme.TextSecondary

private const val SupportEmail = "support@soulmatch.app"

private data class SuccessStory(
    val couple: String,
    val headline: String,
    val summary: String,
    val highlights: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuccessStoriesScreen(
    year: String,
    onBack: () -> Unit,
    onOpenYear: (String) -> Unit = {}
) {
    val stories = successStoriesFor(year)
    val context = LocalContext.current
    var status by remember { mutableStateOf<String?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (year == "overview") "Success Stories" else "Success Stories $year", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
                        eyebrow = "SoulMatch",
                        title = if (year == "overview") "Success Stories" else "$year success stories",
                        subtitle = if (year == "overview") {
                            "Browse the story archive year by year and open the journeys you want to explore."
                        } else {
                            "Real journeys from members who moved from first interest to family approval, trust, and marriage planning."
                        }
                    )
                }
                if (!status.isNullOrBlank()) {
                    item {
                        PremiumCard(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            containerColor = SuccessSoft
                        ) {
                            Text(
                                status ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Success,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                item {
                    PremiumCard(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        containerColor = SurfaceWarm
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionTitle(
                                "What members value most",
                                "These stories usually begin after verified profiles, clearer partner preferences, and stronger family trust signals."
                            )
                            SignalChips(
                                labels = listOf("Verified profiles", "Family involvement", "Preference-led matching"),
                                tone = ChipTone.Info
                            )
                            Button(
                                onClick = {
                                    status = context.launchSupportEmail(
                                        subject = "Share my SoulMatch success story"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Email, contentDescription = null)
                                Text("Share your story")
                            }
                        }
                    }
                }
                if (year == "overview") {
                    items(listOf("2026", "2025", "2024")) { archiveYear ->
                        YearArchiveCard(year = archiveYear, onClick = { onOpenYear(archiveYear) })
                    }
                } else {
                    items(stories) { story ->
                        SuccessStoryCard(story = story)
                    }
                }
            }
        }
    }
}

@Composable
private fun YearArchiveCard(year: String, onClick: () -> Unit) {
    PremiumCard(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        containerColor = SurfaceWarm
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(year, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Open curated couple journeys, trust milestones, and family-approved matches from $year.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SuccessStoryCard(story: SuccessStory) {
    PremiumCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        containerColor = SurfaceWarm
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, Divider)
            ) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Text(
                story.couple,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                story.headline,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                story.summary,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            SignalChips(labels = story.highlights, tone = ChipTone.Success)
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, Divider)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Why this story matters",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "This journey moved ahead because both sides had enough trust signals to keep the conversation active.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

private fun successStoriesFor(year: String): List<SuccessStory> = when (year) {
    "2024" -> listOf(
        SuccessStory(
            couple = "Ananya and Rohit",
            headline = "A values-first match turned into a family-approved engagement",
            summary = "They shortlisted each other after seeing strong education alignment, active family participation, and clear expectations around location and lifestyle.",
            highlights = listOf("Education fit", "Family approval", "Clear intent")
        ),
        SuccessStory(
            couple = "Meera and Arjun",
            headline = "Verified profiles helped both families move faster",
            summary = "Once both sides saw complete profiles and verified signals, the conversation quickly moved from interest request to formal family discussion.",
            highlights = listOf("Verified profile", "Photos ready", "Trust led")
        )
    )
    "2025" -> listOf(
        SuccessStory(
            couple = "Priya and Karthik",
            headline = "Preference-led search created the first serious conversation",
            summary = "A narrow search with community, horoscope comfort, and lifestyle fit helped them find each other without wasting time on weak matches.",
            highlights = listOf("Search filters", "Astrology comfort", "Intent match")
        ),
        SuccessStory(
            couple = "Nisha and Varun",
            headline = "Complete profiles made family review easier",
            summary = "Because both profiles were detailed and photo-ready, both families could move ahead without repeated back-and-forth questions.",
            highlights = listOf("Complete profile", "Family review", "Fast shortlist")
        )
    )
    else -> listOf(
        SuccessStory(
            couple = "Riya and Aditya",
            headline = "Compatibility signals and family participation aligned well",
            summary = "They found each other through strong compatibility ranking and stayed engaged because both families were active from the start.",
            highlights = listOf("High compatibility", "Family led", "Consistent follow-up")
        ),
        SuccessStory(
            couple = "Sneha and Vivek",
            headline = "One interest request turned into a steady relationship track",
            summary = "Their journey began with an interest accept, moved through family review, and continued because both profiles were transparent and serious.",
            highlights = listOf("Interest accepted", "Transparent profile", "Serious intent")
        )
    )
}

private fun android.content.Context.launchSupportEmail(subject: String): String {
    return runCatching {
        startActivity(
            Intent(
                Intent.ACTION_SENDTO,
                Uri.parse("mailto:$SupportEmail?subject=${Uri.encode(subject)}")
            )
        )
        "Opening support email."
    }.getOrElse {
        "No email app was found on this device."
    }
}

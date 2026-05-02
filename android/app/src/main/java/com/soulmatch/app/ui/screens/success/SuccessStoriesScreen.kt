package com.soulmatch.app.ui.screens.success

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soulmatch.app.ui.components.PremiumCard
import com.soulmatch.app.ui.components.PremiumHeader
import com.soulmatch.app.ui.components.PremiumScreen
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.SurfaceWarm
import com.soulmatch.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuccessStoriesScreen(
    year: String,
    onBack: () -> Unit
) {
    val stories = successStoriesFor(year)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Success Stories $year", fontWeight = FontWeight.Bold) },
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
                        title = "$year success stories",
                        subtitle = "Real journeys from members who moved from introduction to family approval."
                    )
                }
                stories.forEach { story ->
                    item {
                        SuccessStoryCard(story)
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessStoryCard(story: String) {
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
                story,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

private fun successStoriesFor(year: String): List<String> = when (year) {
    "2024" -> listOf(
        "Ananya and Rohit connected through shared family values and a strong education match.",
        "Meera and Arjun used verified profiles and family introductions to move forward confidently."
    )
    "2025" -> listOf(
        "Priya and Karthik found each other through preference-led search and horoscope comfort.",
        "Nisha and Varun shortlisted each other after families reviewed complete profiles."
    )
    else -> listOf(
        "Riya and Aditya matched through high compatibility and active family participation.",
        "Sneha and Vivek started with an interest request and continued after both sides approved."
    )
}

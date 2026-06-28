package com.soulmatch.app.ui.screens.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soulmatch.app.data.models.LegalDocumentData
import com.soulmatch.app.ui.design.SoulMatchTokens
import com.soulmatch.app.ui.screens.auth.AuthPageHeader
import com.soulmatch.app.ui.theme.TextSecondary

@Composable
fun LegalContentScreen(
    document: LegalDocumentData,
    onBack: () -> Unit
) {
    val plainBody = buildString {
        if (document.subtitle.isNotBlank()) {
            append(document.subtitle.trim())
            append("\n\n")
        }
        document.sections.forEachIndexed { index, section ->
            if (section.heading.isNotBlank()) {
                append(section.heading.trim())
                append("\n")
            }
            if (section.body.isNotBlank()) {
                append(section.body.trim())
            }
            if (index != document.sections.lastIndex) {
                append("\n\n")
            }
        }
    }.trim()

    Scaffold(
        topBar = {
            AuthPageHeader(
                title = document.title.ifBlank { "SoulMatch policy" },
                onBack = onBack,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (document.updatedAt.isNotBlank()) {
                        Text(
                            text = "Updated ${document.updatedAt}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SoulMatchTokens.Tangerine,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = plainBody,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

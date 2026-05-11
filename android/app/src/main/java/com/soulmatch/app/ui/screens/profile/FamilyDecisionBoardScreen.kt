package com.soulmatch.app.ui.screens.profile

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soulmatch.app.data.models.FamilyDecisionData
import com.soulmatch.app.ui.components.premium.ChipTone
import com.soulmatch.app.ui.components.media.MemberPhoto
import com.soulmatch.app.ui.components.premium.MetricPill
import com.soulmatch.app.ui.components.premium.PremiumCard
import com.soulmatch.app.ui.components.premium.PremiumHeader
import com.soulmatch.app.ui.components.premium.PremiumScreen
import com.soulmatch.app.ui.components.premium.SectionTitle
import com.soulmatch.app.ui.components.premium.SignalChips
import com.soulmatch.app.ui.theme.Divider
import com.soulmatch.app.ui.theme.Error
import com.soulmatch.app.ui.theme.PrimaryDark
import com.soulmatch.app.ui.theme.Success
import com.soulmatch.app.ui.theme.SuccessSoft
import com.soulmatch.app.ui.theme.SurfaceSoft
import com.soulmatch.app.ui.theme.SurfaceWarm
import com.soulmatch.app.ui.theme.TextSecondary
import com.soulmatch.app.ui.titleCase
import com.soulmatch.app.ui.viewmodels.FamilyDecisionBoardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyDecisionBoardScreen(
    onBack: () -> Unit = {},
    onViewProfile: (String) -> Unit = {},
    vm: FamilyDecisionBoardViewModel = hiltViewModel()
) {
    val decisions by vm.decisions.collectAsStateWithLifecycle()
    val loading by vm.isLoading.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Family board", fontWeight = FontWeight.ExtraBold) },
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
                        eyebrow = "Decision support",
                        title = "Review matches with family",
                        subtitle = "Track who is under discussion, who needs a call, and which profiles should move forward."
                    )
                }
                if (!status.isNullOrBlank()) {
                    item {
                        PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), containerColor = SurfaceWarm) {
                            Text(status ?: "", style = MaterialTheme.typography.bodyMedium, color = PrimaryDark, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                item {
                    FamilyBoardSummary(decisions = decisions)
                }
                if (loading && decisions.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (decisions.isEmpty()) {
                    item {
                        PremiumCard(modifier = Modifier.padding(16.dp), containerColor = SurfaceWarm) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
                                Text("No profiles added yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Open any profile and tap Add to family board.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                    }
                } else {
                    items(decisions, key = { it.familyDecisionId.ifBlank { it.targetProfileId } }) { decision ->
                        FamilyDecisionRow(
                            decision = decision,
                            onOpen = { onViewProfile(decision.targetProfileId) },
                            onStatus = { next -> vm.updateDecision(decision, next) },
                            onFamilyInput = { vote, comment -> vm.submitFamilyInput(decision, vote, comment) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FamilyBoardSummary(decisions: List<FamilyDecisionData>) {
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricPill("Review", decisions.count { it.status == "family_review" || it.status == "considering" }.toString(), Modifier.weight(1f), background = SurfaceSoft)
            MetricPill("Calls", decisions.count { it.status == "call_scheduled" || it.status == "spoken" }.toString(), Modifier.weight(1f), background = SurfaceWarm)
            MetricPill("Accepted", decisions.count { it.status == "accepted" }.toString(), Modifier.weight(1f), background = SuccessSoft, accent = Success)
        }
    }
}

@Composable
private fun FamilyDecisionRow(
    decision: FamilyDecisionData,
    onOpen: () -> Unit,
    onStatus: (String) -> Unit,
    onFamilyInput: (String, String) -> Unit
) {
    var familyComment by rememberSaveable(decision.familyDecisionId) { mutableStateOf("") }
    PremiumCard(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onOpen)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                MemberPhoto(
                    photoUrl = decision.targetPhotoUrl,
                    contentDescription = decision.targetName,
                    modifier = Modifier.size(62.dp),
                    shape = RoundedCornerShape(16.dp)
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(decision.targetName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOf(
                            decision.targetOccupation,
                            decision.targetAge.takeIf { it > 0 }?.let { "$it yrs" }.orEmpty(),
                            decision.targetLocation
                        ).filter { it.isNotBlank() }.joinToString(" | ").ifBlank { "Profile under family review" },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    SignalChips(
                        labels = listOf(
                            titleCase(decision.status.replace('_', ' ')),
                            "Family ${titleCase(decision.familyVote)}",
                            if (decision.trustScore > 0) "Trust ${decision.trustScore}%" else "Trust building"
                        ),
                        tone = if (decision.trustScore >= 80) ChipTone.Success else ChipTone.Info
                    )
                }
            }
            if (decision.trustSignals.isNotEmpty()) {
                SignalChips(labels = decision.trustSignals.take(3), tone = ChipTone.Neutral)
            }
            if (decision.comments.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    decision.comments.take(2).forEach { comment ->
                        Surface(shape = RoundedCornerShape(14.dp), color = SurfaceSoft, border = BorderStroke(1.dp, Divider)) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("Family vote: ${titleCase(comment.vote)}", style = MaterialTheme.typography.labelSmall, color = PrimaryDark, fontWeight = FontWeight.Bold)
                                if (comment.comment.isNotBlank()) {
                                    Text(comment.comment, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
            OutlinedTextField(
                value = familyComment,
                onValueChange = { familyComment = it.take(240) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Family comment") },
                singleLine = false,
                maxLines = 2
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = {
                    onFamilyInput("approve", familyComment)
                    familyComment = ""
                }, modifier = Modifier.weight(1f)) {
                    Text("Approve")
                }
                OutlinedButton(onClick = {
                    onFamilyInput("discuss", familyComment)
                    familyComment = ""
                }, modifier = Modifier.weight(1f)) {
                    Text("Discuss")
                }
                OutlinedButton(onClick = {
                    onFamilyInput("reject", familyComment)
                    familyComment = ""
                }, modifier = Modifier.weight(1f)) {
                    Text("Reject")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { onStatus("call_scheduled") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Call")
                }
                Button(onClick = { onStatus("accepted") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Accept")
                }
                OutlinedButton(onClick = { onStatus("declined") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp), tint = Error)
                    Text("Decline")
                }
            }
        }
    }
}

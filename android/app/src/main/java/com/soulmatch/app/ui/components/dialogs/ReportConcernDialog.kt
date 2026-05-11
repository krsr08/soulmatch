package com.soulmatch.app.ui.components.dialogs

// Shared reporting dialog used wherever a member can flag a profile or interaction.

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soulmatch.app.ui.theme.TextSecondary

@Composable
fun ReportConcernDialog(
    profileName: String,
    initialConcern: String = "",
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var concern by remember(profileName, initialConcern) { mutableStateOf(initialConcern) }
    val canSubmit = concern.trim().length >= 8

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialConcern.isBlank()) "Report concern" else "Edit concern",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "Tell us what happened with $profileName. This does not hide the profile unless you choose Hide or Block.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = concern,
                    onValueChange = { concern = it },
                    label = { Text("Concern details") },
                    placeholder = { Text("Example: Asked for money or shared uncomfortable messages") },
                    minLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 132.dp)
                )
                if (!canSubmit) {
                    Text(
                        "Add at least 8 characters so support can understand the concern.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(concern.trim()) },
                enabled = canSubmit
            ) {
                Text(if (initialConcern.isBlank()) "Save concern" else "Update concern")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

package com.soulmatch.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun CallActionDialog(
    memberName: String,
    isVideo: Boolean,
    onDismiss: () -> Unit
) {
    val action = if (isVideo) "video call" else "voice call"
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (isVideo) Icons.Filled.Videocam else Icons.Filled.Call,
                contentDescription = null
            )
        },
        title = { Text(if (isVideo) "Start video call?" else "Start voice call?") },
        text = {
            Text(
                "A $action request will be sent to ${memberName.ifBlank { "this member" }}. Calls open only when both members agree."
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Send request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not now")
            }
        }
    )
}

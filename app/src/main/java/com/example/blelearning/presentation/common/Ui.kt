package com.example.blelearning.presentation.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.blelearning.domain.model.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { if (onBack != null) TextButton(onClick = onBack) { Text("Back") } },
            )
        },
        content = content,
    )
}

fun ConnectionState.label(): String = when (this) {
    ConnectionState.Disconnected -> "Disconnected"
    ConnectionState.Connecting -> "Connecting…"
    is ConnectionState.Reconnecting -> "Reconnecting (attempt $attempt of $max)…"
    ConnectionState.Connected -> "Connected"
    ConnectionState.DiscoveringServices -> "Discovering services…"
    ConnectionState.Ready -> "Ready"
    ConnectionState.Disconnecting -> "Disconnecting…"
    is ConnectionState.Failed -> "Failed: ${error.message}"
}

/** Maps a raw exception to text for the UI. */
fun Throwable.userMessage(): String = message ?: "Unknown error"

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = modifier)

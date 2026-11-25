package com.example.blelearning.presentation.logs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.blelearning.domain.model.LogLevel
import com.example.blelearning.presentation.common.BleScaffold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

@Composable
fun LogsScreen(onBack: () -> Unit, vm: LogsViewModel = hiltViewModel()) {
    val logs by vm.logs.collectAsStateWithLifecycle()

    BleScaffold(title = "BLE event log", onBack = onBack) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${logs.size} entries", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = vm::clear) { Text("Clear") }
            }
            LazyColumn {
                items(logs, key = { it.id }) { e ->
                    val color = when (e.level) {
                        LogLevel.ERROR -> MaterialTheme.colorScheme.error
                        LogLevel.WARN -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Text(
                        "${timeFormat.format(Date(e.timestampMillis))} [${e.tag}] ${e.message}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = color,
                        modifier = Modifier.padding(vertical = 3.dp),
                    )
                }
            }
        }
    }
}

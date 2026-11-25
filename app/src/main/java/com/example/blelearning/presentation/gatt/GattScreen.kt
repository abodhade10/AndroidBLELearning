package com.example.blelearning.presentation.gatt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.blelearning.core.bluetooth.BleUuids
import com.example.blelearning.core.common.ValueFormatter
import com.example.blelearning.presentation.common.BleScaffold
import com.example.blelearning.presentation.common.SectionTitle

@Composable
fun GattScreen(
    onBack: () -> Unit,
    onOpenNotifications: () -> Unit,
    vm: GattViewModel = hiltViewModel(),
) {
    val ch by vm.characteristic.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    var hex by remember { mutableStateOf("48 65 6C 6C 6F") } // "Hello"
    var withResponse by remember { mutableStateOf(true) }

    BleScaffold(title = ch?.name ?: "Characteristic", onBack = onBack) { padding ->
        val c = ch
        if (c == null) {
            Text("Characteristic not available (disconnected?).", Modifier.padding(padding).padding(16.dp))
            return@BleScaffold
        }
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(BleUuids.shortOrFull(c.uuid), style = MaterialTheme.typography.bodySmall)
            Text(
                "Properties: " + c.properties.joinToString { it.name.lowercase().replace('_', ' ') },
                style = MaterialTheme.typography.bodyMedium,
            )

            SectionTitle("Value")
            Card(Modifier.fillMaxWidth()) {
                Text(
                    ui.lastValue?.let { ValueFormatter.describe(c.uuid, it.value) } ?: "No value yet",
                    Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            if (c.canRead) {
                Button(onClick = vm::read, enabled = !ui.busy, modifier = Modifier.fillMaxWidth()) { Text("Read") }
            }

            if (c.canWrite) {
                SectionTitle("Write")
                OutlinedTextField(
                    value = hex, onValueChange = { hex = it },
                    label = { Text("Hex bytes") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = withResponse, onCheckedChange = { withResponse = it })
                    Text("Write with response")
                }
                Button(onClick = { vm.write(hex, withResponse) }, enabled = !ui.busy, modifier = Modifier.fillMaxWidth()) {
                    Text("Write")
                }
            }

            if (c.canNotify) {
                SectionTitle("Notifications")
                Button(onClick = vm::toggleNotifications, enabled = !ui.busy, modifier = Modifier.fillMaxWidth()) {
                    Text(if (ui.isNotifying) "Disable notifications" else "Enable notifications")
                }
                if (ui.isNotifying) {
                    OutlinedButton(onClick = onOpenNotifications, modifier = Modifier.fillMaxWidth()) {
                        Text("Open live stream")
                    }
                }
            }

            ui.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary) }
        }
    }
}

package com.example.blelearning.presentation.device

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.blelearning.core.bluetooth.BleUuids
import com.example.blelearning.domain.model.ConnectionState
import com.example.blelearning.domain.model.GattServiceInfo
import com.example.blelearning.domain.model.isActive
import com.example.blelearning.presentation.common.BleScaffold
import com.example.blelearning.presentation.common.label
import java.util.UUID

@Composable
fun DeviceScreen(
    onBack: () -> Unit,
    onOpenCharacteristic: (service: UUID, characteristic: UUID) -> Unit,
    onOpenNotifications: () -> Unit,
    vm: DeviceViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val ready = state.connection is ConnectionState.Ready

    BleScaffold(title = state.device?.displayName ?: "Device", onBack = onBack) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(state.connection.label(), style = MaterialTheme.typography.titleLarge)
            Text(
                "${state.device?.address.orEmpty()}   MTU ${state.mtu}   RSSI ${state.rssi?.let { "$it dBm" } ?: "–"}",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.connection.isActive) {
                    OutlinedButton(onClick = vm::disconnect) { Text("Disconnect") }
                } else {
                    Button(onClick = vm::reconnect, enabled = state.device != null) { Text("Connect") }
                }
                OutlinedButton(onClick = { vm.requestMtu() }, enabled = ready) { Text("Request MTU") }
                OutlinedButton(onClick = { vm.readRssi() }, enabled = ready) { Text("RSSI") }
            }
            message?.let {
                Card(Modifier.fillMaxWidth().clickable { vm.dismissMessage() }) {
                    Text(it, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (ready) {
                OutlinedButton(onClick = onOpenNotifications, modifier = Modifier.fillMaxWidth()) {
                    Text("Live notifications")
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.services, key = { it.uuid }) { service ->
                    ServiceCard(service, onOpenCharacteristic)
                }
            }
        }
    }
}

@Composable
private fun ServiceCard(service: GattServiceInfo, onOpen: (UUID, UUID) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.clickable { expanded = !expanded }.padding(12.dp)) {
            Text(service.name, style = MaterialTheme.typography.titleMedium)
            Text(BleUuids.shortOrFull(service.uuid), style = MaterialTheme.typography.bodySmall)
            Text(
                "${service.characteristics.size} characteristics  ·  tap to ${if (expanded) "collapse" else "expand"}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (expanded) {
            service.characteristics.forEach { ch ->
                HorizontalDivider()
                Column(Modifier.fillMaxWidth().clickable { onOpen(service.uuid, ch.uuid) }.padding(12.dp)) {
                    Text(ch.name, style = MaterialTheme.typography.bodyLarge)
                    Text(BleUuids.shortOrFull(ch.uuid), style = MaterialTheme.typography.bodySmall)
                    Text(
                        ch.properties.joinToString(" · ") { it.name.lowercase().replace('_', ' ') },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

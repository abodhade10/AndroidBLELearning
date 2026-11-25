package com.example.blelearning.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.blelearning.domain.model.ConnectionState
import com.example.blelearning.domain.model.isActive
import com.example.blelearning.presentation.common.BleScaffold
import com.example.blelearning.presentation.common.SectionTitle
import com.example.blelearning.presentation.common.label

@Composable
fun DashboardScreen(
    onScan: () -> Unit,
    onDevice: () -> Unit,
    onNotifications: () -> Unit,
    onLogs: () -> Unit,
    vm: DashboardViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    BleScaffold(title = "BLE Master") { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Connection", style = MaterialTheme.typography.labelLarge)
                    Text(state.connection.label(), style = MaterialTheme.typography.headlineSmall)
                    state.device?.takeIf { state.connection.isActive }?.let {
                        Text("${it.displayName}  ·  ${it.address}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) { Text("Scan for devices") }

            if (state.connection.isActive) {
                OutlinedButton(onClick = onDevice, modifier = Modifier.fillMaxWidth()) { Text("Open device") }
                OutlinedButton(onClick = vm::disconnect, modifier = Modifier.fillMaxWidth()) { Text("Disconnect") }
            }
            if (state.connection is ConnectionState.Ready) {
                OutlinedButton(onClick = onNotifications, modifier = Modifier.fillMaxWidth()) { Text("Live notifications") }
            }
            OutlinedButton(onClick = onLogs, modifier = Modifier.fillMaxWidth()) { Text("BLE event log") }

            SectionTitle("Practice scenarios")
            Text(
                "1. Scan and find your peripheral\n" +
                    "2. Connect and discover GATT services\n" +
                    "3. Read and write a characteristic\n" +
                    "4. Enable notifications and watch the stream\n" +
                    "5. Switch the peripheral off, watch reconnection",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Tip: open the log screen alongside each step to see the callback order.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

package com.example.blelearning.presentation.scanner

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.blelearning.core.bluetooth.BlePermissions
import com.example.blelearning.domain.model.BleDevice
import com.example.blelearning.presentation.common.BleScaffold

@Composable
fun ScannerScreen(
    onBack: () -> Unit,
    onConnecting: () -> Unit,
    vm: ScannerViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) vm.startScan() else vm.onPermissionDenied()
    }

    fun onScanClicked() {
        when {
            state.isScanning -> vm.stopScan()
            BlePermissions.hasAll(context) -> vm.startScan()
            else -> permissionLauncher.launch(BlePermissions.required())
        }
    }

    BleScaffold(title = "Scanner", onBack = onBack) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            Button(onClick = ::onScanClicked, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.isScanning) "Stop scanning" else "Start scan")
            }
            if (state.isScanning) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
            if (state.devices.isEmpty() && !state.isScanning && state.error == null) {
                Text(
                    "No devices yet. Start a scan with your peripheral advertising nearby.",
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            LazyColumn(Modifier.padding(top = 8.dp)) {
                items(state.devices, key = { it.address }) { device ->
                    DeviceRow(device) {
                        vm.connect(device)
                        onConnecting()
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(device: BleDevice, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(enabled = device.isConnectable, onClick = onClick).padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(device.displayName, style = MaterialTheme.typography.titleMedium)
            Text("${device.rssi} dBm", style = MaterialTheme.typography.titleMedium)
        }
        Text(device.address, style = MaterialTheme.typography.bodySmall)
        if (device.serviceUuids.isNotEmpty()) {
            Text("Advertises: ${device.serviceUuids.joinToString()}", style = MaterialTheme.typography.bodySmall)
        }
        if (!device.isConnectable) {
            Text("Not connectable (beacon)", style = MaterialTheme.typography.bodySmall)
        }
    }
}

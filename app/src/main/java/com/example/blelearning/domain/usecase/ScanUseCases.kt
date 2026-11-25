package com.example.blelearning.domain.usecase

import com.example.blelearning.domain.model.BleDevice
import com.example.blelearning.domain.repository.BleScanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import javax.inject.Inject

/**
 * Raw scan callbacks arrive once per advertisement packet (many per second, same device repeated).
 * Business rule: de-duplicate by MAC address, keep the latest RSSI, sort strongest first.
 */
class ScanDevicesUseCase @Inject constructor(private val repository: BleScanRepository) {
    operator fun invoke(): Flow<List<BleDevice>> =
        repository.scan()
            .runningFold(emptyMap<String, BleDevice>()) { acc, device -> acc + (device.address to device) }
            .map { devices -> devices.values.sortedByDescending { it.rssi } }
            .conflate()
}

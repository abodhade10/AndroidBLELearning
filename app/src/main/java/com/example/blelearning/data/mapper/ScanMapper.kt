package com.example.blelearning.data.mapper

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import com.example.blelearning.domain.model.BleDevice

/**
 * ScanResult (Android type) -> BleDevice (domain type).
 * The domain layer never imports android.bluetooth.*.
 */
@SuppressLint("MissingPermission") // permission is verified before the scan starts
fun ScanResult.toBleDevice(): BleDevice = BleDevice(
    // scanRecord.deviceName comes from the advertisement itself and needs no permission.
    name = scanRecord?.deviceName ?: runCatching { device.name }.getOrNull(),
    address = device.address,
    rssi = rssi,
    isConnectable = isConnectable,
    serviceUuids = scanRecord?.serviceUuids.orEmpty().map { it.uuid.toString() },
    txPower = scanRecord?.txPowerLevel?.takeIf { it != Int.MIN_VALUE },
    lastSeenMillis = System.currentTimeMillis(),
)

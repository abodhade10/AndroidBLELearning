package com.example.blelearning.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import com.example.blelearning.core.bluetooth.BlePermissions
import com.example.blelearning.data.mapper.toBleDevice
import com.example.blelearning.domain.model.BleDevice
import com.example.blelearning.domain.model.BleException
import com.example.blelearning.domain.repository.LogRepository
import com.example.blelearning.domain.repository.d
import com.example.blelearning.domain.repository.e
import com.example.blelearning.domain.repository.i
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps BluetoothLeScanner in a cold Flow.
 *  collect  -> startScan()
 *  cancel   -> awaitClose { stopScan() }   (so a ViewModel can never leak a running scan)
 */
@SuppressLint("MissingPermission") // checked explicitly at the top of the flow
@Singleton
class BleScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothManager: BluetoothManager,
    private val log: LogRepository,
) {
    fun scan(): Flow<BleDevice> = callbackFlow {
        if (!BlePermissions.hasAll(context)) { close(BleException.PermissionMissing); return@callbackFlow }
        val adapter = bluetoothManager.adapter
        if (adapter == null) { close(BleException.BluetoothUnavailable); return@callbackFlow }
        if (!adapter.isEnabled) { close(BleException.BluetoothDisabled); return@callbackFlow }
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) { close(BleException.BluetoothDisabled); return@callbackFlow }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                trySend(result.toBleDevice())
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { trySend(it.toBleDevice()) }
            }

            override fun onScanFailed(errorCode: Int) {
                log.e(TAG, "onScanFailed errorCode=$errorCode")
                close(BleException.ScanFailed(errorCode))
            }
        }

        // LOW_LATENCY = highest duty cycle. Fine in the foreground, drains battery if left running.
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        log.i(TAG, "startScan(mode=LOW_LATENCY, filters=none)")
        scanner.startScan(null, settings, callback)

        awaitClose {
            log.d(TAG, "stopScan()")
            // Throws IllegalStateException if Bluetooth was switched off meanwhile.
            runCatching { scanner.stopScan(callback) }
        }
    }

    private companion object { const val TAG = "Scanner" }
}

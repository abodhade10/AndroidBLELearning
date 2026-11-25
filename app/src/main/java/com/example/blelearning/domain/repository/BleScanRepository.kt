package com.example.blelearning.domain.repository

import com.example.blelearning.domain.model.BleDevice
import kotlinx.coroutines.flow.Flow

interface BleScanRepository {
    /**
     * Cold flow: collecting starts the scan, cancelling the collector stops it.
     * Fails with a [com.example.blelearning.domain.model.BleException].
     */
    fun scan(): Flow<BleDevice>
}

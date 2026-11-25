package com.example.blelearning.data.repository

import com.example.blelearning.data.bluetooth.BleScanner
import com.example.blelearning.domain.model.BleDevice
import com.example.blelearning.domain.repository.BleScanRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BleScanRepositoryImpl @Inject constructor(private val scanner: BleScanner) : BleScanRepository {
    override fun scan(): Flow<BleDevice> = scanner.scan()
}

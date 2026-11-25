package com.example.blelearning.domain.repository

import com.example.blelearning.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

interface BleConnectionRepository {
    val connectionState: StateFlow<ConnectionState>
    val connectedDevice: StateFlow<BleDevice?>
    val services: StateFlow<List<GattServiceInfo>>
    val mtu: StateFlow<Int>
    val rssi: StateFlow<Int?>
    val notifications: Flow<CharacteristicValue>

    fun connect(device: BleDevice)
    fun disconnect()

    suspend fun read(service: UUID, characteristic: UUID): Result<ByteArray>
    suspend fun write(service: UUID, characteristic: UUID, value: ByteArray, withResponse: Boolean): Result<Unit>
    suspend fun setNotification(service: UUID, characteristic: UUID, enable: Boolean): Result<Unit>
    suspend fun requestMtu(mtu: Int): Result<Int>
    suspend fun readRssi(): Result<Int>
}

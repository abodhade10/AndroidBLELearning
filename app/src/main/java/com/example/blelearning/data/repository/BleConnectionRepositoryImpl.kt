package com.example.blelearning.data.repository

import com.example.blelearning.data.bluetooth.GattClient
import com.example.blelearning.domain.model.BleDevice
import com.example.blelearning.domain.repository.BleConnectionRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleConnectionRepositoryImpl @Inject constructor(
    private val client: GattClient,
) : BleConnectionRepository {
    override val connectionState get() = client.state
    override val connectedDevice get() = client.device
    override val services get() = client.services
    override val mtu get() = client.mtu
    override val rssi get() = client.rssi
    override val notifications get() = client.notifications

    override fun connect(device: BleDevice) = client.connect(device)
    override fun disconnect() = client.disconnect()
    override suspend fun read(service: UUID, characteristic: UUID) = client.read(service, characteristic)
    override suspend fun write(service: UUID, characteristic: UUID, value: ByteArray, withResponse: Boolean) =
        client.write(service, characteristic, value, withResponse)
    override suspend fun setNotification(service: UUID, characteristic: UUID, enable: Boolean) =
        client.setNotification(service, characteristic, enable)
    override suspend fun requestMtu(mtu: Int) = client.requestMtu(mtu)
    override suspend fun readRssi() = client.readRssi()
}

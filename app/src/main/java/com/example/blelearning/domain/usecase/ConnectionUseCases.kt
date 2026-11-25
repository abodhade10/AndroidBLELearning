package com.example.blelearning.domain.usecase

import com.example.blelearning.domain.model.BleDevice
import com.example.blelearning.domain.repository.BleConnectionRepository
import javax.inject.Inject

class ConnectDeviceUseCase @Inject constructor(private val repo: BleConnectionRepository) {
    operator fun invoke(device: BleDevice) = repo.connect(device)
}

class DisconnectDeviceUseCase @Inject constructor(private val repo: BleConnectionRepository) {
    operator fun invoke() = repo.disconnect()
}

class ObserveConnectionStateUseCase @Inject constructor(private val repo: BleConnectionRepository) {
    operator fun invoke() = repo.connectionState
}

class ObserveConnectedDeviceUseCase @Inject constructor(private val repo: BleConnectionRepository) {
    operator fun invoke() = repo.connectedDevice
}

class ObserveServicesUseCase @Inject constructor(private val repo: BleConnectionRepository) {
    operator fun invoke() = repo.services
}

class ObserveLinkInfoUseCase @Inject constructor(private val repo: BleConnectionRepository) {
    val mtu get() = repo.mtu
    val rssi get() = repo.rssi
}

class RequestMtuUseCase @Inject constructor(private val repo: BleConnectionRepository) {
    suspend operator fun invoke(mtu: Int) = repo.requestMtu(mtu)
}

class ReadRssiUseCase @Inject constructor(private val repo: BleConnectionRepository) {
    suspend operator fun invoke() = repo.readRssi()
}

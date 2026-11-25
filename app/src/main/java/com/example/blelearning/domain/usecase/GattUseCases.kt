package com.example.blelearning.domain.usecase

import com.example.blelearning.domain.repository.BleConnectionRepository
import java.util.UUID
import javax.inject.Inject

class ReadCharacteristicUseCase @Inject constructor(private val repo: BleConnectionRepository) {
    suspend operator fun invoke(service: UUID, characteristic: UUID) = repo.read(service, characteristic)
}

class WriteCharacteristicUseCase @Inject constructor(private val repo: BleConnectionRepository) {
    suspend operator fun invoke(service: UUID, characteristic: UUID, value: ByteArray, withResponse: Boolean) =
        repo.write(service, characteristic, value, withResponse)
}

class SetNotificationUseCase @Inject constructor(private val repo: BleConnectionRepository) {
    suspend operator fun invoke(service: UUID, characteristic: UUID, enable: Boolean) =
        repo.setNotification(service, characteristic, enable)
}

class ObserveNotificationsUseCase @Inject constructor(private val repo: BleConnectionRepository) {
    operator fun invoke() = repo.notifications
}

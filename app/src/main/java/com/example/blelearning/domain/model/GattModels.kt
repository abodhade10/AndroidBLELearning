package com.example.blelearning.domain.model

import java.util.UUID

enum class CharProperty { READ, WRITE, WRITE_NO_RESPONSE, NOTIFY, INDICATE }

data class GattServiceInfo(
    val uuid: UUID,
    val name: String,
    val characteristics: List<GattCharacteristicInfo>,
)

data class GattCharacteristicInfo(
    val uuid: UUID,
    val serviceUuid: UUID,
    val name: String,
    val properties: Set<CharProperty>,
    val descriptors: List<UUID>,
) {
    val canRead get() = CharProperty.READ in properties
    val canWrite get() = CharProperty.WRITE in properties || CharProperty.WRITE_NO_RESPONSE in properties
    val canNotify get() = CharProperty.NOTIFY in properties || CharProperty.INDICATE in properties
}

class CharacteristicValue(
    val serviceUuid: UUID,
    val characteristicUuid: UUID,
    val value: ByteArray,
    val timestampMillis: Long = System.currentTimeMillis(),
)

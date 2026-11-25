package com.example.blelearning.data.mapper

import android.bluetooth.BluetoothGattCharacteristic as Ch
import android.bluetooth.BluetoothGattService
import com.example.blelearning.core.bluetooth.BleUuids
import com.example.blelearning.domain.model.CharProperty
import com.example.blelearning.domain.model.GattCharacteristicInfo
import com.example.blelearning.domain.model.GattServiceInfo

fun BluetoothGattService.toDomain() = GattServiceInfo(
    uuid = uuid,
    name = BleUuids.nameOf(uuid) ?: "Unknown Service",
    characteristics = characteristics.map { it.toDomain() },
)

fun Ch.toDomain() = GattCharacteristicInfo(
    uuid = uuid,
    serviceUuid = service.uuid,
    name = BleUuids.nameOf(uuid) ?: "Unknown Characteristic",
    properties = properties.toPropertySet(),
    descriptors = descriptors.map { it.uuid },
)

/** The GATT `properties` field is a bit mask; decode it into a readable set. */
fun Int.toPropertySet(): Set<CharProperty> = buildSet {
    if ((this@toPropertySet and Ch.PROPERTY_READ) != 0) add(CharProperty.READ)
    if ((this@toPropertySet and Ch.PROPERTY_WRITE) != 0) add(CharProperty.WRITE)
    if ((this@toPropertySet and Ch.PROPERTY_WRITE_NO_RESPONSE) != 0) add(CharProperty.WRITE_NO_RESPONSE)
    if ((this@toPropertySet and Ch.PROPERTY_NOTIFY) != 0) add(CharProperty.NOTIFY)
    if ((this@toPropertySet and Ch.PROPERTY_INDICATE) != 0) add(CharProperty.INDICATE)
}

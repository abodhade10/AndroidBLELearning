package com.example.blelearning.core.bluetooth

import java.util.UUID

/**
 * 16-bit "assigned numbers" are shorthand for 128-bit UUIDs built on the Bluetooth Base UUID:
 * 0000XXXX-0000-1000-8000-00805F9B34FB
 */
object BleUuids {
    fun fromShort(short: Int): UUID =
        UUID.fromString("0000%04x-0000-1000-8000-00805f9b34fb".format(short))

    // Descriptor
    val CCCD: UUID = fromShort(0x2902) // Client Characteristic Configuration

    // Services
    val GENERIC_ACCESS = fromShort(0x1800)
    val GENERIC_ATTRIBUTE = fromShort(0x1801)
    val DEVICE_INFORMATION = fromShort(0x180A)
    val HEART_RATE = fromShort(0x180D)
    val BATTERY = fromShort(0x180F)

    // Characteristics
    val DEVICE_NAME = fromShort(0x2A00)
    val APPEARANCE = fromShort(0x2A01)
    val BATTERY_LEVEL = fromShort(0x2A19)
    val MANUFACTURER_NAME = fromShort(0x2A29)
    val HEART_RATE_MEASUREMENT = fromShort(0x2A37)

    // Custom service used by docs/esp32/ble_learning_peripheral.ino
    val LEARNING_SERVICE: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    val LEARNING_ECHO: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
    val LEARNING_COUNTER: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a9")

    private val names: Map<UUID, String> = mapOf(
        GENERIC_ACCESS to "Generic Access",
        GENERIC_ATTRIBUTE to "Generic Attribute",
        DEVICE_INFORMATION to "Device Information",
        HEART_RATE to "Heart Rate",
        BATTERY to "Battery",
        DEVICE_NAME to "Device Name",
        APPEARANCE to "Appearance",
        BATTERY_LEVEL to "Battery Level",
        MANUFACTURER_NAME to "Manufacturer Name",
        HEART_RATE_MEASUREMENT to "Heart Rate Measurement",
        fromShort(0x2A04) to "Peripheral Preferred Connection Parameters",
        fromShort(0x2A05) to "Service Changed",
        LEARNING_SERVICE to "Learning Service (ESP32)",
        LEARNING_ECHO to "Echo (read / write)",
        LEARNING_COUNTER to "Counter (notify)",
    )

    fun nameOf(uuid: UUID): String? = names[uuid]

    /** "0x180F" for SIG-assigned UUIDs, full UUID otherwise. */
    fun shortOrFull(uuid: UUID): String {
        val s = uuid.toString()
        return if (s.startsWith("0000") && s.endsWith("-0000-1000-8000-00805f9b34fb"))
            "0x" + s.substring(4, 8).uppercase()
        else s
    }
}

package com.example.blelearning.domain.model

data class BleDevice(
    val name: String?,
    val address: String,
    val rssi: Int,
    val isConnectable: Boolean,
    val serviceUuids: List<String> = emptyList(),
    val txPower: Int? = null,
    val lastSeenMillis: Long = 0L,
) {
    val displayName: String get() = name?.takeIf { it.isNotBlank() } ?: "Unknown device"
}

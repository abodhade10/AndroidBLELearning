package com.example.blelearning.domain.model

/** Every BLE failure is mapped into this hierarchy so the UI never sees raw status codes. */
sealed class BleException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    object BluetoothUnavailable : BleException("This device has no Bluetooth LE support")
    object BluetoothDisabled : BleException("Bluetooth is turned off")
    object PermissionMissing : BleException("Bluetooth permission was not granted")
    class ScanFailed(val errorCode: Int) : BleException("Scan failed (error code $errorCode)")
    class ConnectionFailed(val status: Int, detail: String) : BleException("Connection failed: $detail")
    object NotConnected : BleException("Not connected to a device")
    class CharacteristicNotFound(name: String) : BleException("Characteristic $name not found")
    class OperationFailed(detail: String, val status: Int? = null) : BleException(detail)
    class Timeout(operation: String) : BleException("$operation timed out")
}

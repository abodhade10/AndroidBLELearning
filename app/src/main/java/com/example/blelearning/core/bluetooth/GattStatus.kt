package com.example.blelearning.core.bluetooth

/** Translates the infamous numeric GATT status codes into something debuggable. */
object GattStatus {
    fun describe(status: Int): String = "$status (${name(status)})"

    private fun name(status: Int): String = when (status) {
        0 -> "GATT_SUCCESS"
        2 -> "READ_NOT_PERMITTED"
        3 -> "WRITE_NOT_PERMITTED"
        5 -> "INSUFFICIENT_AUTHENTICATION"
        6 -> "REQUEST_NOT_SUPPORTED"
        7 -> "INVALID_OFFSET"
        8 -> "CONNECTION_TIMEOUT / INSUFFICIENT_AUTHORIZATION"
        13 -> "INVALID_ATTRIBUTE_LENGTH"
        15 -> "INSUFFICIENT_ENCRYPTION"
        19 -> "CONNECTION_TERMINATED_BY_PEER"
        22 -> "CONNECTION_TERMINATED_BY_LOCAL_HOST"
        34 -> "LMP_RESPONSE_TIMEOUT"
        62 -> "CONNECTION_FAILED_TO_ESTABLISH"
        133 -> "GATT_ERROR (generic: stale cache, out of range, too many clients...)"
        257 -> "GATT_FAILURE"
        else -> "UNKNOWN"
    }
}

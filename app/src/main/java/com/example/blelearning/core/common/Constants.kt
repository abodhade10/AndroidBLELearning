package com.example.blelearning.core.common

object BleConfig {
    const val SCAN_TIMEOUT_MS = 15_000L
    const val OPERATION_TIMEOUT_MS = 10_000L
    const val MAX_RECONNECT_ATTEMPTS = 3
    const val RECONNECT_BASE_DELAY_MS = 1_000L
    const val DEFAULT_MTU = 23
    const val PREFERRED_MTU = 247
    const val MAX_LOG_ENTRIES = 500
}

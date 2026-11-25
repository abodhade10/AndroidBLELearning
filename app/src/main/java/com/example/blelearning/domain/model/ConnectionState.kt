package com.example.blelearning.domain.model

sealed interface ConnectionState {
    object Disconnected : ConnectionState
    object Connecting : ConnectionState
    data class Reconnecting(val attempt: Int, val max: Int) : ConnectionState
    object Connected : ConnectionState
    object DiscoveringServices : ConnectionState
    object Ready : ConnectionState
    object Disconnecting : ConnectionState
    data class Failed(val error: BleException) : ConnectionState
}

/** True while a connection is being set up, is up, or is being torn down. */
val ConnectionState.isActive: Boolean
    get() = this !is ConnectionState.Disconnected && this !is ConnectionState.Failed

package com.example.blelearning.presentation.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blelearning.core.common.BleConfig
import com.example.blelearning.domain.model.BleDevice
import com.example.blelearning.domain.model.ConnectionState
import com.example.blelearning.domain.model.GattServiceInfo
import com.example.blelearning.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceUiState(
    val connection: ConnectionState = ConnectionState.Disconnected,
    val device: BleDevice? = null,
    val services: List<GattServiceInfo> = emptyList(),
    val mtu: Int = BleConfig.DEFAULT_MTU,
    val rssi: Int? = null,
)

@HiltViewModel
class DeviceViewModel @Inject constructor(
    observeState: ObserveConnectionStateUseCase,
    observeDevice: ObserveConnectedDeviceUseCase,
    observeServices: ObserveServicesUseCase,
    link: ObserveLinkInfoUseCase,
    private val connectDevice: ConnectDeviceUseCase,
    private val disconnectDevice: DisconnectDeviceUseCase,
    private val requestMtuUseCase: RequestMtuUseCase,
    private val readRssiUseCase: ReadRssiUseCase,
) : ViewModel() {

    val uiState: StateFlow<DeviceUiState> = combine(
        observeState(), observeDevice(), observeServices(), link.mtu, link.rssi,
    ) { state, device, services, mtu, rssi -> DeviceUiState(state, device, services, mtu, rssi) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeviceUiState())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun disconnect() = disconnectDevice()

    fun reconnect() { uiState.value.device?.let(connectDevice::invoke) }

    fun requestMtu() = viewModelScope.launch {
        requestMtuUseCase(BleConfig.PREFERRED_MTU)
            .onSuccess { _message.value = "MTU is now $it (payload ${it - 3} bytes)" }
            .onFailure { _message.value = it.message }
    }

    fun readRssi() = viewModelScope.launch {
        readRssiUseCase()
            .onSuccess { _message.value = "RSSI $it dBm" }
            .onFailure { _message.value = it.message }
    }

    fun dismissMessage() { _message.value = null }
}

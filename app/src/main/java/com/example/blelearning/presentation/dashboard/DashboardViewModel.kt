package com.example.blelearning.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blelearning.domain.model.BleDevice
import com.example.blelearning.domain.model.ConnectionState
import com.example.blelearning.domain.usecase.DisconnectDeviceUseCase
import com.example.blelearning.domain.usecase.ObserveConnectedDeviceUseCase
import com.example.blelearning.domain.usecase.ObserveConnectionStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUiState(
    val connection: ConnectionState = ConnectionState.Disconnected,
    val device: BleDevice? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeState: ObserveConnectionStateUseCase,
    observeDevice: ObserveConnectedDeviceUseCase,
    private val disconnectDevice: DisconnectDeviceUseCase,
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> =
        combine(observeState(), observeDevice()) { s, d -> DashboardUiState(s, d) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun disconnect() = disconnectDevice()
}
